
package com.example.taskmanagement.dto;

import java.util.List;

public record TaskPageResponse(
        List<TaskResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
