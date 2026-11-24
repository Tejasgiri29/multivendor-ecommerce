package com.example.shop.service;

import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.OrderDto;
import com.example.shop.entity.*;
import com.example.shop.exception.*;
import com.example.shop.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock CouponRepository couponRepository;
    @Mock UserRepository userRepository;
    OrderService orderService;

    User customer = TestData.user(1L, Role.CUSTOMER);
    User seller = TestData.user(2L, Role.SELLER);
    Product p1 = TestData.product(10L, "10.00", 50, seller);
    Product p2 = TestData.product(11L, "5.50", 50, seller);

    @BeforeEach
    void setUp() {
        CouponService couponService = new CouponService(couponRepository, cartItemRepository);
        orderService = new OrderService(orderRepository, cartItemRepository, productRepository, couponRepository,
                userRepository, couponService);
    }

    private void stubCart() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdWithProduct(1L))
                .thenReturn(List.of(TestData.cartItem(customer, p1, 2), TestData.cartItem(customer, p2, 1)));
    }

    @Test
    void checkoutDecrementsStockAppliesCouponAndClearsCart() {
        stubCart();
        Coupon coupon = Coupon.builder().id(7L).code("SAVE").type(CouponType.PERCENT).value(new BigDecimal("10"))
                .minOrderAmount(new BigDecimal("20")).active(true).build();
        when(productRepository.decrementStock(10L, 2)).thenReturn(1);
        when(productRepository.decrementStock(11L, 1)).thenReturn(1);
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(coupon));
        when(couponRepository.incrementUsage(7L)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto dto = orderService.checkout(1L, new CheckoutRequest("1 Main St", "SAVE"));

        assertEquals(OrderStatus.PLACED, dto.status());
        assertEquals(0, new BigDecimal("25.50").compareTo(dto.subtotal()));
        assertEquals(0, new BigDecimal("2.55").compareTo(dto.discount()));
        assertEquals(0, new BigDecimal("22.95").compareTo(dto.total()));
        assertEquals(2, dto.items().size());
        verify(productRepository).decrementStock(10L, 2);
        verify(productRepository).decrementStock(11L, 1);
        verify(cartItemRepository).deleteByUserId(1L);
    }

    @Test
    void checkoutThrowsWhenAtomicStockUpdateAffectsNoRows() {
        stubCart();
        when(productRepository.decrementStock(10L, 2)).thenReturn(1);
        when(productRepository.decrementStock(11L, 1)).thenReturn(0);   // out of stock

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> orderService.checkout(1L, new CheckoutRequest("1 Main St", null)));

        assertTrue(ex.getMessage().contains("Product 11"));
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUserId(anyLong());
    }

    @Test
    void checkoutRejectsEmptyCart() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdWithProduct(1L)).thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> orderService.checkout(1L, new CheckoutRequest("addr", null)));
        verifyNoInteractions(productRepository);
    }

    @Test
    void checkoutFailsWhenCouponUsageLimitRaceIsLost() {
        stubCart();
        Coupon coupon = Coupon.builder().id(7L).code("SAVE").type(CouponType.FIXED).value(new BigDecimal("5"))
                .active(true).usageLimit(1).usedCount(0).build();
        when(productRepository.decrementStock(anyLong(), anyInt())).thenReturn(1);
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(coupon));
        when(couponRepository.incrementUsage(7L)).thenReturn(0);

        assertThrows(BadRequestException.class, () -> orderService.checkout(1L, new CheckoutRequest("addr", "SAVE")));
        verify(orderRepository, never()).save(any());
    }

    private Order order(OrderStatus status) {
        Order o = Order.builder().id(100L).customer(customer).status(status).subtotal(new BigDecimal("25.50"))
                .discount(BigDecimal.ZERO).total(new BigDecimal("25.50")).shippingAddress("addr").build();
        o.addItem(OrderItem.builder().product(p1).seller(seller).productName("Product 10").unitPrice(p1.getPrice()).quantity(2).build());
        o.addItem(OrderItem.builder().product(p2).seller(seller).productName("Product 11").unitPrice(p2.getPrice()).quantity(1).build());
        return o;
    }

    @Test
    void cancelRestoresStockForEveryItem() {
        Order o = order(OrderStatus.PAID);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(o));

        OrderDto dto = orderService.changeStatus(100L, 1L, Role.CUSTOMER, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, dto.status());
        verify(productRepository).incrementStock(10L, 2);
        verify(productRepository).incrementStock(11L, 1);
    }

    @Test
    void cancelReleasesCouponUsage() {
        Order o = order(OrderStatus.PLACED);
        o.setCouponCode("SAVE");
        Coupon c = Coupon.builder().id(7L).code("SAVE").build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(o));
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));

        orderService.changeStatus(100L, 1L, Role.CUSTOMER, OrderStatus.CANCELLED);

        verify(couponRepository).decrementUsage(7L);
    }

    @Test
    void invalidTransitionsAreRejectedAndStockUntouched() {
        Order delivered = order(OrderStatus.DELIVERED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(delivered));
        assertThrows(BadRequestException.class, () -> orderService.changeStatus(100L, 99L, Role.ADMIN, OrderStatus.CANCELLED));
        assertEquals(OrderStatus.DELIVERED, delivered.getStatus());
        verify(productRepository, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    void shippedOrdersCannotBeCancelled() {
        Order shipped = order(OrderStatus.SHIPPED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(shipped));
        assertThrows(BadRequestException.class, () -> orderService.changeStatus(100L, 1L, Role.CUSTOMER, OrderStatus.CANCELLED));
    }

    @Test
    void customerCannotShipAndStrangersCannotSeeOrder() {
        Order o = order(OrderStatus.PAID);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(o));
        assertThrows(ForbiddenException.class, () -> orderService.changeStatus(100L, 1L, Role.CUSTOMER, OrderStatus.SHIPPED));
        assertThrows(ResourceNotFoundException.class, () -> orderService.changeStatus(100L, 55L, Role.CUSTOMER, OrderStatus.CANCELLED));
    }

    @Test
    void sellerWithItemsInOrderCanShip() {
        Order o = order(OrderStatus.PAID);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(o));
        when(orderRepository.orderContainsSeller(100L, 2L)).thenReturn(true);
        OrderDto dto = orderService.changeStatus(100L, 2L, Role.SELLER, OrderStatus.SHIPPED);
        assertEquals(OrderStatus.SHIPPED, dto.status());
    }

    @Test
    void sellerWithoutItemsInOrderIsRejected() {
        Order o = order(OrderStatus.PAID);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(o));
        when(orderRepository.orderContainsSeller(100L, 77L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> orderService.changeStatus(100L, 77L, Role.SELLER, OrderStatus.SHIPPED));
    }

    @Test
    void statusLifecycleTable() {
        assertTrue(OrderStatus.PLACED.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.PLACED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.CANCELLED.allowedNext().isEmpty());
    }
}
