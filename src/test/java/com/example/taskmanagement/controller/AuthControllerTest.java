
package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.LoginRequest;
import com.example.taskmanagement.dto.LoginResponse;
import com.example.taskmanagement.dto.RegisterRequest;
import com.example.taskmanagement.dto.RegisterResponse;
import com.example.taskmanagement.exception.DuplicateEmailException;
import com.example.taskmanagement.exception.GlobalExceptionHandler;
import com.example.taskmanagement.exception.InvalidCredentialsException;
import com.example.taskmanagement.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {

        authService = mock(AuthService.class);

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AuthController controller =
                new AuthController(authService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // TEST 1: Successful Registration
    @Test
    void register_shouldReturn201Created() throws Exception {

        RegisterResponse response = new RegisterResponse(
                1L,
                "test@example.com",
                Instant.parse("2026-11-09T10:00:00Z")
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "test@example.com",
                              "password": "Password123!"
                            }
                            """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.createdAt").exists());

        verify(authService).register(any(RegisterRequest.class));
    }

    // TEST 2: Invalid Email Validation
    @Test
    void register_shouldRejectInvalidEmail() throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "invalid-email",
                              "password": "Password123!"
                            }
                            """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email").exists());

        verifyNoInteractions(authService);
    }

    // TEST 3: Duplicate Email
    @Test
    void register_shouldReturn409ForDuplicateEmail() throws Exception {

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException(
                        "An account with this email already exists"
                ));

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "test@example.com",
                              "password": "Password123!"
                            }
                            """)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"));

    }

    // TEST 4: Successful Login
    @Test
    void login_shouldReturn200AndJwt() throws Exception {

        LoginResponse response = new LoginResponse(
                "test.jwt.token",
                "Bearer",
                3600L
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "test@example.com",
                              "password": "Password123!"
                            }
                            """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("test.jwt.token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authService).login(any(LoginRequest.class));
    }

    // TEST 5: Incorrect Login Credentials
    @Test
    void login_shouldReturn401ForInvalidCredentials() throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "test@example.com",
                              "password": "WrongPassword123!"
                            }
                            """)
        )
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value(
                "Invalid email or password"
        ));
    }
}
