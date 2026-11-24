package com.example.shop.controller;

import com.example.shop.dto.*;
import com.example.shop.security.UserPrincipal;
import com.example.shop.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminController {
    private final CategoryService categoryService;
    private final CouponService couponService;
    private final AdminService adminService;
    private final OrderService orderService;

    public AdminController(CategoryService categoryService, CouponService couponService, AdminService adminService,
                           OrderService orderService) {
        this.categoryService = categoryService;
        this.couponService = couponService;
        this.adminService = adminService;
        this.orderService = orderService;
    }

    // categories
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest req) {
        return categoryService.create(req);
    }

    @PutMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return categoryService.update(id, req);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
    }

    // coupons
    @GetMapping("/coupons")
    public List<CouponDto> coupons() {
        return couponService.list();
    }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponDto createCoupon(@Valid @RequestBody CouponRequest req) {
        return couponService.create(req);
    }

    @PutMapping("/coupons/{id}")
    public CouponDto updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest req) {
        return couponService.update(id, req);
    }

    @DeleteMapping("/coupons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCoupon(@PathVariable Long id) {
        couponService.delete(id);
    }

    // users
    @GetMapping("/users")
    public PageResponse<UserDto> users(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return adminService.listUsers(page, size);
    }

    @PutMapping("/users/{id}/enabled")
    public UserDto setEnabled(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                              @RequestParam boolean value) {
        return adminService.setEnabled(me.getId(), id, value);
    }

    @PostMapping("/users/{id}/approve-seller")
    public UserDto approveSeller(@PathVariable Long id) {
        return adminService.approveSeller(id);
    }

    // orders
    @GetMapping("/orders")
    public PageResponse<OrderDto> orders(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return orderService.allOrders(page, size);
    }
}
