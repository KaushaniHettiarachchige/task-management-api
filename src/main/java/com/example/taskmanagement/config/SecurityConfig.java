
package com.example.taskmanagement.config;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        // 401: Missing, expired or invalid authentication
        AuthenticationEntryPoint authenticationEntryPoint =
                (request, response, exception) -> {
                    response.setHeader("WWW-Authenticate", "Bearer");

                    writeJsonError(
                            request,
                            response,
                            HttpStatus.UNAUTHORIZED,
                            "Authentication is required or the token is invalid"
                    );
                };

        // 403: Authenticated user without sufficient permission
        AccessDeniedHandler accessDeniedHandler =
                (request, response, exception) -> {
                    writeJsonError(
                            request,
                            response,
                            HttpStatus.FORBIDDEN,
                            "Access denied"
                    );
                };

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .jwt(jwt -> {})
            );

        return http.build();
    }

    private static void writeJsonError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {

        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        String json = """
                {
                  "timestamp": "%s",
                  "status": %d,
                  "error": "%s",
                  "message": "%s",
                  "path": "%s"
                }
                """.formatted(
                    Instant.now(),
                    status.value(),
                    escapeJson(status.getReasonPhrase()),
                    escapeJson(message),
                    escapeJson(request.getRequestURI())
                );

        response.getWriter().write(json);
    }

    private static String escapeJson(String text) {
        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) {
                        result.append(
                            String.format("\\u%04x", (int) c)
                        );
                    } else {
                        result.append(c);
                    }
                }
            }
        }

        return result.toString();
    }
}
