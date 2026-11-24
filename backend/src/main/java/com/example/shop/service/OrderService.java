package com.example.shop.service;

import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.OrderDto;
import com.example.shop.dto.PageResponse;
import com.example.shop.entity.*;
import com.example.shop.exception.*;
import com.example.shop.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository,
                        ProductRepository productRepository, CouponRepository couponRepository,
                        UserRepository userRepository, CouponService couponService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.couponService = couponService;
    }

    /**
     * Converts the user's cart into an order. Everything runs in ONE transaction: if any stock decrement
     * (or coupon consumption) fails, an exception is thrown and every change made so far is rolled back.
     */
    @Transactional
    public OrderDto checkout(Long userId, CheckoutRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Items arrive ordered by product id: consistent lock ordering avoids deadlocks between concurrent checkouts.
        List<CartItem> cart = cartItemRepository.findByUserIdWithProduct(userId);
        if (cart.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Order order = Order.builder().customer(user).status(OrderStatus.PLACED)
                .shippingAddress(req.shippingAddress().trim()).build();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem ci : cart) {
            Product p = ci.getProduct();
            if (!p.isActive()) {
                throw new BadRequestException("'" + p.getName() + "' is no longer available");
            }
            // Atomic conditional UPDATE: succeeds (1 row) only if enough stock remains.
            int updated = productRepository.decrementStock(p.getId(), ci.getQuantity());
            if (updated != 1) {
                throw new InsufficientStockException("Insufficient stock for '" + p.getName() + "'");
            }
            OrderItem oi = OrderItem.builder().product(p).seller(p.getSeller()).productName(p.getName())
                    .unitPrice(p.getPrice()).quantity(ci.getQuantity()).build();
            order.addItem(oi);
            subtotal = subtotal.add(oi.lineTotal());
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            Coupon coupon = couponService.findByCode(req.couponCode());
            discount = couponService.calculateDiscount(coupon, subtotal);
            if (couponRepository.incrementUsage(coupon.getId()) != 1) {   // atomic usage-limit check
                throw new BadRequestException("Coupon usage limit reached");
            }
            order.setCouponCode(coupon.getCode());
        }
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setTotal(subtotal.subtract(discount));

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);
        return DtoMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> myOrders(Long userId, int page, int size) {
        return PageResponse.of(orderRepository.findByCustomerId(userId, pageable(page, size)), DtoMapper::toDto);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> sellerOrders(Long sellerId, int page, int size) {
        return PageResponse.of(orderRepository.findContainingSeller(sellerId, pageable(page, size)),
                o -> DtoMapper.toDto(o, sellerId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> allOrders(int page, int size) {
        return PageResponse.of(orderRepository.findAll(pageable(page, size)), DtoMapper::toDto);
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long orderId, Long userId, Role role) {
        Order o = load(orderId);
        if (role == Role.ADMIN) return DtoMapper.toDto(o);
        if (role == Role.SELLER && orderRepository.orderContainsSeller(orderId, userId)) return DtoMapper.toDto(o, userId);
        if (o.getCustomer().getId().equals(userId)) return DtoMapper.toDto(o);
        throw new ResourceNotFoundException("Order not found");
    }

    /**
     * Applies a status transition after checking (1) the lifecycle is valid and (2) the actor may perform it.
     * CUSTOMER (own orders): PAID, CANCELLED. SELLER (orders with their items): SHIPPED, DELIVERED. ADMIN: any valid step.
     * Moving to CANCELLED restores stock and releases the coupon use.
     */
    @Transactional
    public OrderDto changeStatus(Long orderId, Long actorId, Role role, OrderStatus target) {
        Order order = load(orderId);
        authorize(order, actorId, role, target);
        if (!order.getStatus().canTransitionTo(target)) {
            throw new BadRequestException("Invalid status transition: " + order.getStatus() + " -> " + target);
        }
        if (target == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
            }
            if (order.getCouponCode() != null) {
                couponRepository.findByCodeIgnoreCase(order.getCouponCode())
                        .ifPresent(c -> couponRepository.decrementUsage(c.getId()));
            }
        }
        order.setStatus(target);
        return DtoMapper.toDto(order, role == Role.SELLER ? actorId : null);
    }

    private void authorize(Order order, Long actorId, Role role, OrderStatus target) {
        switch (role) {
            case ADMIN -> { }
            case CUSTOMER -> {
                if (!order.getCustomer().getId().equals(actorId)) throw new ResourceNotFoundException("Order not found");
                if (!Set.of(OrderStatus.PAID, OrderStatus.CANCELLED).contains(target)) {
                    throw new ForbiddenException("Customers may only pay for or cancel their orders");
                }
            }
            case SELLER -> {
                if (!orderRepository.orderContainsSeller(order.getId(), actorId)) throw new ResourceNotFoundException("Order not found");
                if (!Set.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED).contains(target)) {
                    throw new ForbiddenException("Sellers may only mark orders as SHIPPED or DELIVERED");
                }
            }
        }
    }

    private Order load(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending().and(Sort.by("id").descending()));
    }
}
