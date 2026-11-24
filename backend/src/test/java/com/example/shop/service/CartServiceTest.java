package com.example.shop.service;

import com.example.shop.dto.CartDto;
import com.example.shop.entity.*;
import com.example.shop.exception.BadRequestException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.CartItemRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    CartService cartService;

    User customer = TestData.user(1L, Role.CUSTOMER);
    User seller = TestData.user(2L, Role.SELLER);

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, productRepository, userRepository);
    }

    @Test
    void addItemCreatesLineAndReturnsCartWithSubtotal() {
        Product p = TestData.product(10L, "12.50", 5, seller);
        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        when(cartItemRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(customer);
        when(cartItemRepository.findByUserIdWithProduct(1L)).thenReturn(List.of(TestData.cartItem(customer, p, 3)));

        CartDto cart = cartService.addItem(1L, 10L, 3);

        verify(cartItemRepository).save(any(CartItem.class));
        assertEquals(1, cart.items().size());
        assertEquals(0, new BigDecimal("37.50").compareTo(cart.subtotal()));
        assertEquals(3, cart.itemCount());
    }

    @Test
    void addItemMergesWithExistingQuantity() {
        Product p = TestData.product(10L, "10.00", 10, seller);
        CartItem existing = TestData.cartItem(customer, p, 4);
        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        when(cartItemRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.findByUserIdWithProduct(1L)).thenReturn(List.of(existing));

        cartService.addItem(1L, 10L, 2);

        assertEquals(6, existing.getQuantity());
    }

    @Test
    void addItemRejectsQuantityAboveStock() {
        Product p = TestData.product(10L, "10.00", 3, seller);
        CartItem existing = TestData.cartItem(customer, p, 2);
        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        when(cartItemRepository.findByUserIdAndProductId(1L, 10L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> cartService.addItem(1L, 10L, 2));
        verify(cartItemRepository, never()).save(any());
        assertEquals(2, existing.getQuantity());
    }

    @Test
    void addItemRejectsInactiveProduct() {
        Product p = TestData.product(10L, "10.00", 3, seller);
        p.setActive(false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        assertThrows(ResourceNotFoundException.class, () -> cartService.addItem(1L, 10L, 1));
    }

    @Test
    void updateQuantityRejectsMissingLine() {
        when(cartItemRepository.findByUserIdAndProductId(1L, 99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cartService.updateQuantity(1L, 99L, 1));
    }
}
