
package com.example.taskmanagement.dto;

import com.example.taskmanagement.enums.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(

        @NotBlank(message = "Title is required")
        @Size(
            max = 150,
            message = "Title must not exceed 150 characters"
        )
        String title,

        @Size(
            max = 2000,
            message = "Description must not exceed 2000 characters"
        )
        String description,

        @NotNull(message = "Status is required")
        TaskStatus status,

        LocalDate dueDate

) {
}
