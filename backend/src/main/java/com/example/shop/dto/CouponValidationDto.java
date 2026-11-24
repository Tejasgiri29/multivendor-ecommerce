package com.example.shop.dto;

import java.math.BigDecimal;

public record CouponValidationDto(String code, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
}
