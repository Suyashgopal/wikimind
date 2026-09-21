package com.wikimind.dto;

public record AuthResponse(String token, String role, String organization) {
}
