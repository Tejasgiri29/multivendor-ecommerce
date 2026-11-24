package com.example.shop.controller;

import com.example.shop.dto.ProductDto;
import com.example.shop.security.UserPrincipal;
import com.example.shop.service.WishlistService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Wishlist (customer)")
public class WishlistController {
    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<ProductDto> list(@AuthenticationPrincipal UserPrincipal me) {
        return wishlistService.list(me.getId());
    }

    @PostMapping("/{productId}")
    public List<ProductDto> add(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long productId) {
        return wishlistService.add(me.getId(), productId);
    }

    @DeleteMapping("/{productId}")
    public List<ProductDto> remove(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long productId) {
        return wishlistService.remove(me.getId(), productId);
    }
}
