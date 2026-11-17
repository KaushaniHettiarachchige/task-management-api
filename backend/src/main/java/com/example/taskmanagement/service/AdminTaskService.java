
package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.TaskPageResponse;
import com.example.taskmanagement.dto.TaskResponse;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.repository.TaskRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTaskService {

    private final TaskRepository taskRepository;

    public AdminTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // BONUS 3: Retrieve tasks belonging to all users
    @Transactional(readOnly = true)
    public TaskPageResponse getAllTasks(
            int page,
            int size
    ) {

        // Validate pagination
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number must be 0 or greater"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        // Newest tasks first
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                ).and(
                        Sort.by(
                                Sort.Direction.DESC,
                                "id"
                        )
                )
        );

        // No owner filter: ADMIN can view every user's tasks
        Page<Task> tasks = taskRepository.findAll(pageable);

        return new TaskPageResponse(
                tasks.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                tasks.getNumber(),
                tasks.getSize(),
                tasks.getTotalElements(),
                tasks.getTotalPages()
        );
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getOwner().getId(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
