package com.example.taskmanagement.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {
}
