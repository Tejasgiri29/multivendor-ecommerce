package com.example.shop.controller;

import com.example.shop.dto.*;
import com.example.shop.security.UserPrincipal;
import com.example.shop.service.CartService;
import com.example.shop.service.CouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Cart & Coupons (customer)")
public class CartController {
    private final CartService cartService;
    private final CouponService couponService;

    public CartController(CartService cartService, CouponService couponService) {
        this.cartService = cartService;
        this.couponService = couponService;
    }

    @GetMapping("/cart")
    public CartDto get(@AuthenticationPrincipal UserPrincipal me) {
        return cartService.getCart(me.getId());
    }

    @PostMapping("/cart/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartDto add(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody CartItemRequest req) {
        return cartService.addItem(me.getId(), req.productId(), req.quantity());
    }

    @PutMapping("/cart/items/{productId}")
    public CartDto update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long productId,
                          @Valid @RequestBody CartQuantityRequest req) {
        return cartService.updateQuantity(me.getId(), productId, req.quantity());
    }

    @DeleteMapping("/cart/items/{productId}")
    public CartDto remove(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long productId) {
        return cartService.removeItem(me.getId(), productId);
    }

    @DeleteMapping("/cart")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal UserPrincipal me) {
        cartService.clear(me.getId());
    }

    @PostMapping("/coupons/validate")
    public CouponValidationDto validateCoupon(@AuthenticationPrincipal UserPrincipal me,
                                              @Valid @RequestBody CouponValidateRequest req) {
        return couponService.validate(me.getId(), req.code());
    }
}
