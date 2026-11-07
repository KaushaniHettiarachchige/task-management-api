
package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.RegisterRequest;
import com.example.taskmanagement.dto.RegisterResponse;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.exception.DuplicateEmailException;
import com.example.taskmanagement.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(
                "An account with this email already exists"
            );
        }

        String password = request.password();

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                "Password must not exceed 72 UTF-8 bytes"
            );
        }

        String hashedPassword = passwordEncoder.encode(password);

        User user = new User(email, hashedPassword);

        try {
            User savedUser = userRepository.saveAndFlush(user);

            return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
            );

        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException(
                "An account with this email already exists"
            );
        }
    }
}
