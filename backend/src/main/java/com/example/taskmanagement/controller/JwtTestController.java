package com.example.taskmanagement.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwtTestController {

    @GetMapping("/api/test/protected")
    public Map<String, String> protectedEndpoint(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Map.of(
            "message", "JWT authentication successful",
            "email", jwt.getSubject()
        );
    }
}