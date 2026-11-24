package com.example.shop.service;

import com.example.shop.dto.CartDto;
import com.example.shop.dto.CartItemDto;
import com.example.shop.entity.CartItem;
import com.example.shop.entity.Product;
import com.example.shop.exception.BadRequestException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.CartItemRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartDto getCart(Long userId) {
        return toDto(cartItemRepository.findByUserIdWithProduct(userId));
    }

    /** Adds quantity to an existing line or creates it. Quantity may not exceed current stock. */
    @Transactional
    public CartDto addItem(Long userId, Long productId, int quantity) {
        Product product = productRepository.findById(productId).filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        int newQty = quantity + (item == null ? 0 : item.getQuantity());
        if (newQty > product.getStock()) {
            throw new BadRequestException("Only " + product.getStock() + " unit(s) of '" + product.getName() + "' in stock");
        }
        if (item == null) {
            item = CartItem.builder().user(userRepository.getReferenceById(userId)).product(product).quantity(newQty).build();
        } else {
            item.setQuantity(newQty);
        }
        cartItemRepository.save(item);
        return toDto(cartItemRepository.findByUserIdWithProduct(userId));
    }

    @Transactional
    public CartDto updateQuantity(Long userId, Long productId, int quantity) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"));
        if (quantity > item.getProduct().getStock()) {
            throw new BadRequestException("Only " + item.getProduct().getStock() + " unit(s) in stock");
        }
        item.setQuantity(quantity);
        return toDto(cartItemRepository.findByUserIdWithProduct(userId));
    }

    @Transactional
    public CartDto removeItem(Long userId, Long productId) {
        cartItemRepository.findByUserIdAndProductId(userId, productId).ifPresent(cartItemRepository::delete);
        cartItemRepository.flush();
        return toDto(cartItemRepository.findByUserIdWithProduct(userId));
    }

    @Transactional
    public void clear(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    static BigDecimal subtotal(List<CartItem> items) {
        return items.stream().map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartDto toDto(List<CartItem> items) {
        List<CartItemDto> dtos = items.stream().map(i -> new CartItemDto(i.getProduct().getId(), i.getProduct().getName(),
                i.getProduct().getImageUrl(), i.getProduct().getPrice(), i.getQuantity(), i.getProduct().getStock(),
                i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList();
        return new CartDto(dtos, subtotal(items), items.stream().mapToInt(CartItem::getQuantity).sum());
    }
}
