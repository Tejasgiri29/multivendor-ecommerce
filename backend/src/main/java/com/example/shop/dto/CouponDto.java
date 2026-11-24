package com.example.shop.dto;

import com.example.shop.entity.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponDto(Long id, String code, CouponType type, BigDecimal value, BigDecimal minOrderAmount,
                        LocalDateTime expiresAt, Integer usageLimit, int usedCount, boolean active) {
}
