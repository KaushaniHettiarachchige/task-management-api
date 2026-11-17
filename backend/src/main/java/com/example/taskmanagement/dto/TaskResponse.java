
package com.example.taskmanagement.dto;

import com.example.taskmanagement.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Long ownerId,
        Instant createdAt,
        Instant updatedAt
) {
}

