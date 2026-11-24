package com.example.shop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDto(Long id, String name, String description, BigDecimal price, int stock, String imageUrl,
                         Long categoryId, String categoryName, Long sellerId, String sellerName,
                         LocalDateTime createdAt) {
}
