package com.example.shop.dto;

import com.example.shop.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(Long id, Long customerId, String customerName, OrderStatus status, BigDecimal subtotal,
                       BigDecimal discount, BigDecimal total, String couponCode, String shippingAddress,
                       LocalDateTime createdAt, List<OrderItemDto> items) {
}
