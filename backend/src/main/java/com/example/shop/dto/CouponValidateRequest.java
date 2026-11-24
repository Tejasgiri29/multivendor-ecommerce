package com.example.shop.dto;

import jakarta.validation.constraints.NotBlank;

public record CouponValidateRequest(@NotBlank String code) {
}
