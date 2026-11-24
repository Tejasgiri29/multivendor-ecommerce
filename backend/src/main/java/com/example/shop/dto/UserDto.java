package com.example.shop.dto;

import com.example.shop.entity.Role;
import java.time.LocalDateTime;

public record UserDto(Long id, String email, String fullName, Role role, boolean enabled, boolean sellerApproved,
                      LocalDateTime createdAt) {
}
