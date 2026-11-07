package com.example.taskmanagement.exception;

public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException(String message) {
        super(message);
    }
}
