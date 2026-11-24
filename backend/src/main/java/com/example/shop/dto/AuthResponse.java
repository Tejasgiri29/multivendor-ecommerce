package com.example.shop.dto;

public record AuthResponse(String token, String tokenType, UserDto user) {
}
