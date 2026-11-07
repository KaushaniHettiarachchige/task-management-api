package com.example.taskmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email is too long")
    String email, 

    @NotBlank(message = "Password is required")
    @Size(
        min = 8,
        max = 72,
        message = "Password must contain 8 to 72 characters"
    )
    String password
) {
}
