
package com.example.taskmanagement.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> details
    ) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);

        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(
            DuplicateEmailException ex,
            HttpServletRequest request
    ) {
        return errorResponse(
            HttpStatus.CONFLICT,
            ex.getMessage(),
            request.getRequestURI(),
            null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.putIfAbsent(
                error.getField(),
                error.getDefaultMessage()
            )
        );

        return errorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request.getRequestURI(),
            fieldErrors
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return errorResponse(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request.getRequestURI(),
            null
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return errorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid JSON request body",
            request.getRequestURI(),
            null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedError(
            Exception ex,
            HttpServletRequest request
    ) {
        return errorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected server error occurred",
            request.getRequestURI(),
            null
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
        InvalidCredentialsException ex,
        HttpServletRequest request
    ) {
        return errorResponse(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage(),
            request.getRequestURI(),
            null
        );
    }
}
