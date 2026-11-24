package com.example.shop.service;

import com.example.shop.dto.*;
import com.example.shop.entity.*;

import java.util.List;

/** Static entity -> DTO conversions. Callers must be inside a transaction for lazy associations. */
public final class DtoMapper {
    private DtoMapper() {}

    public static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isEnabled(), u.isSellerApproved(),
                u.getCreatedAt());
    }

    public static CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription());
    }

    public static ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(), p.getImageUrl(),
                p.getCategory().getId(), p.getCategory().getName(), p.getSeller().getId(), p.getSeller().getFullName(),
                p.getCreatedAt());
    }

    public static CouponDto toDto(Coupon c) {
        return new CouponDto(c.getId(), c.getCode(), c.getType(), c.getValue(), c.getMinOrderAmount(), c.getExpiresAt(),
                c.getUsageLimit(), c.getUsedCount(), c.isActive());
    }

    public static OrderItemDto toDto(OrderItem i) {
        return new OrderItemDto(i.getProduct().getId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(),
                i.lineTotal(), i.getSeller().getId());
    }

    /** @param onlySellerId when non-null, only this seller's line items are included (seller view). */
    public static OrderDto toDto(Order o, Long onlySellerId) {
        List<OrderItemDto> items = o.getItems().stream()
                .filter(i -> onlySellerId == null || i.getSeller().getId().equals(onlySellerId))
                .map(DtoMapper::toDto).toList();
        return new OrderDto(o.getId(), o.getCustomer().getId(), o.getCustomer().getFullName(), o.getStatus(),
                o.getSubtotal(), o.getDiscount(), o.getTotal(), o.getCouponCode(), o.getShippingAddress(),
                o.getCreatedAt(), items);
    }

    public static OrderDto toDto(Order o) {
        return toDto(o, null);
    }
}
