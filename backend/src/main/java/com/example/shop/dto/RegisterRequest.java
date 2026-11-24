package com.example.shop.dto;

import com.example.shop.entity.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password,
        @NotBlank @Size(max = 100) String fullName,
        @NotNull(message = "role is required (CUSTOMER or SELLER)") Role role) {
}
