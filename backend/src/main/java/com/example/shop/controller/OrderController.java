package com.example.shop.controller;

import com.example.shop.dto.*;
import com.example.shop.entity.OrderStatus;
import com.example.shop.security.UserPrincipal;
import com.example.shop.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Checkout the cart (atomic stock decrement, optional coupon)")
    public OrderDto checkout(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody CheckoutRequest req) {
        return orderService.checkout(me.getId(), req);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<OrderDto> mine(@AuthenticationPrincipal UserPrincipal me,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return orderService.myOrders(me.getId(), page, size);
    }

    @GetMapping("/{id}")
    public OrderDto get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return orderService.get(id, me.getId(), me.getRole());
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Simulated payment: PLACED -> PAID")
    public OrderDto pay(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return orderService.changeStatus(id, me.getId(), me.getRole(), OrderStatus.PAID);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cancel own order (PLACED or PAID); restores stock")
    public OrderDto cancel(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return orderService.changeStatus(id, me.getId(), me.getRole(), OrderStatus.CANCELLED);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @Operation(summary = "Seller/Admin: advance an order (SHIPPED, DELIVERED; admin may also CANCEL)")
    public OrderDto updateStatus(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                 @Valid @RequestBody StatusUpdateRequest req) {
        return orderService.changeStatus(id, me.getId(), me.getRole(), req.status());
    }
}
