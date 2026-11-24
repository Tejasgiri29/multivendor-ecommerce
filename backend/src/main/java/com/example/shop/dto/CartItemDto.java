package com.example.shop.dto;

import java.math.BigDecimal;

public record CartItemDto(Long productId, String name, String imageUrl, BigDecimal unitPrice, int quantity,
                          int stock, BigDecimal lineTotal) {
}
