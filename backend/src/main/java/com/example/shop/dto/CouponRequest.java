package com.example.shop.dto;

import com.example.shop.entity.CouponType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "letters, digits, - and _ only") String code,
        @NotNull CouponType type,
        @NotNull @DecimalMin("0.01") BigDecimal value,
        @DecimalMin("0.00") BigDecimal minOrderAmount,
        LocalDateTime expiresAt,
        @Min(1) Integer usageLimit,
        Boolean active) {
}
