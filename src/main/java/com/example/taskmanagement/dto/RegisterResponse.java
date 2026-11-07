package com.example.taskmanagement.dto;

import java.time.Instant;

public record RegisterResponse(
    Long id,
    String email, 
    Instant createdAt
) {    
}
