package com.example.shop.dto;

import jakarta.validation.constraints.*;

public record CartQuantityRequest(@NotNull @Min(1) @Max(1000) Integer quantity) {
}
