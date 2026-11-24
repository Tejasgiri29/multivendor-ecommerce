package com.example.shop.controller;

import com.example.shop.dto.*;
import com.example.shop.security.UserPrincipal;
import com.example.shop.service.OrderService;
import com.example.shop.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller")
public class SellerController {
    private final ProductService productService;
    private final OrderService orderService;

    public SellerController(ProductService productService, OrderService orderService) {
        this.productService = productService;
        this.orderService = orderService;
    }

    @GetMapping("/products")
    public PageResponse<ProductDto> myProducts(@AuthenticationPrincipal UserPrincipal me,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return productService.listForSeller(me.getId(), page, size);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ProductRequest req) {
        return productService.create(me.getId(), req);
    }

    @PutMapping("/products/{id}")
    public ProductDto update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                             @Valid @RequestBody ProductRequest req) {
        return productService.update(me.getId(), id, req);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        productService.delete(me.getId(), id);
    }

    @GetMapping("/orders")
    public PageResponse<OrderDto> orders(@AuthenticationPrincipal UserPrincipal me,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return orderService.sellerOrders(me.getId(), page, size);
    }
}
