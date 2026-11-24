package com.example.shop.dto;

import jakarta.validation.constraints.*;

public record CheckoutRequest(@NotBlank @Size(max = 500) String shippingAddress, @Size(max = 50) String couponCode) {
}
