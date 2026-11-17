
package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.LoginRequest;
import com.example.taskmanagement.dto.LoginResponse;
import com.example.taskmanagement.dto.RegisterRequest;
import com.example.taskmanagement.dto.RegisterResponse;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.exception.DuplicateEmailException;
import com.example.taskmanagement.exception.InvalidCredentialsException;
import com.example.taskmanagement.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }

    // TEST 1: Successful registration
    @Test
    void register_shouldCreateUserSuccessfully() {

        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "Password123!"
        );

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password123!"))
                .thenReturn("hashedPassword");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    return user;
                });

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("test@example.com", response.email());

        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).saveAndFlush(any(User.class));
    }

    // TEST 2: Duplicate email rejection
    @Test
    void register_shouldRejectDuplicateEmail() {

        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "Password123!"
        );

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }

    // TEST 3: Password hashing before saving
    @Test
    void register_shouldStoreHashedPassword() {

        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "Password123!"
        );

        when(passwordEncoder.encode("Password123!"))
                .thenReturn("bcryptHashedPassword");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).saveAndFlush(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals(
                "bcryptHashedPassword",
                savedUser.getPasswordHash()
        );

        assertNotEquals(
                "Password123!",
                savedUser.getPasswordHash()
        );
    }

    // TEST 4: Successful login and JWT generation
    @Test
    void login_shouldReturnJwtForValidCredentials() {

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "Password123!"
        );

        User user = new User(
                "test@example.com",
                "hashedPassword"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Password123!",
                "hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test.jwt.token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(3600L);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("test.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
    }

    // TEST 5: Incorrect password
    @Test
    void login_shouldRejectIncorrectPassword() {

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "WrongPassword123!"
        );

        User user = new User(
                "test@example.com",
                "hashedPassword"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword123!",
                "hashedPassword"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never())
                .generateToken(any(User.class));
    }

    // TEST 6: Unknown email
    @Test
    void login_shouldRejectUnknownEmail() {

        LoginRequest request = new LoginRequest(
                "unknown@example.com",
                "Password123!"
        );

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(any(User.class));
    }
}
