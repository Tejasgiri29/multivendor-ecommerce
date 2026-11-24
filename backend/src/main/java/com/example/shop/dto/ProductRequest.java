package com.example.shop.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @NotNull Long categoryId,
        @Size(max = 500) String imageUrl) {
}
