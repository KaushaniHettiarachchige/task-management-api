
package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.TaskPageResponse;
import com.example.taskmanagement.dto.TaskRequest;
import com.example.taskmanagement.dto.TaskResponse;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.TaskStatus;
import com.example.taskmanagement.exception.TaskNotFoundException;

import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Jwt jwt) {
        return userRepository.findByEmail(jwt.getSubject())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user no longer exists"
                        )
                );
    }

    private Task getOwnedTask(Long taskId, Long ownerId) {
        return taskRepository
                .findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() ->
                        new TaskNotFoundException(taskId)
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

    private TaskPageResponse toPageResponse(Page<Task> tasks) {
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

    private void validatePagination(int page, int size) {
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
    }

    @Transactional
    public TaskResponse createTask(
            TaskRequest request,
            Jwt jwt
    ) {
        User owner = getCurrentUser(jwt);

        Task task = new Task(
                request.title().trim(),
                request.description(),
                request.status(),
                request.dueDate(),
                owner
        );

        Task savedTask = taskRepository.saveAndFlush(task);

        return toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskPageResponse getTasks(
            Jwt jwt,
            int page,
            int size,
            TaskStatus status
    ) {
        validatePagination(page, size);

        User user = getCurrentUser(jwt);

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

        Page<Task> tasks;

        if (status == null) {
            tasks = taskRepository.findByOwnerId(
                    user.getId(),
                    pageable
            );
        } else {
            tasks = taskRepository.findByOwnerIdAndStatus(
                    user.getId(),
                    status,
                    pageable
            );
        }

        return toPageResponse(tasks);
    }

    @Transactional(readOnly = true)
    public TaskPageResponse getTasks(
            Jwt jwt,
            int page,
            int size,
            TaskStatus status,
            String keyword,
            String sortBy,
            String direction
    ) {
        validatePagination(page, size);

        User user = getCurrentUser(jwt);

        String sortField;

        if (sortBy == null || sortBy.isBlank()
                || sortBy.equalsIgnoreCase("createdAt")) {
            sortField = "createdAt";

        } else if (sortBy.equalsIgnoreCase("dueDate")) {
            sortField = "dueDate";

        } else {
            throw new IllegalArgumentException(
                    "sortBy must be createdAt or dueDate"
            );
        }

        String normalizedDirection =
                direction == null || direction.isBlank()
                        ? "desc"
                        : direction.trim().toLowerCase(Locale.ROOT);

        Sort.Direction sortDirection;

        if (normalizedDirection.equals("asc")) {
            sortDirection = Sort.Direction.ASC;

        } else if (normalizedDirection.equals("desc")) {
            sortDirection = Sort.Direction.DESC;

        } else {
            throw new IllegalArgumentException(
                    "direction must be asc or desc"
            );
        }

        Sort sort = Sort.by(sortDirection, sortField)
                .and(Sort.by(sortDirection, "id"));

        Pageable pageable = PageRequest.of(
                page,
                size,
                sort
        );

        Specification<Task> specification =
                (root, query, cb) ->
                        cb.equal(
                                root.get("owner").get("id"),
                                user.getId()
                        );

        if (status != null) {
            specification = specification.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("status"),
                                    status
                            )
            );
        }

        if (keyword != null && !keyword.isBlank()) {

            String searchText = keyword.trim()
                    .toLowerCase(Locale.ROOT);

            String escapedText = searchText
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");

            String searchPattern = "%" + escapedText + "%";

            specification = specification.and(
                    (root, query, cb) ->
                            cb.or(
                                    cb.like(
                                            cb.lower(root.get("title")),
                                            searchPattern,
                                            '\\'
                                    ),
                                    cb.like(
                                            cb.lower(root.get("description")),
                                            searchPattern,
                                            '\\'
                                    )
                            )
            );
        }

        Page<Task> tasks = taskRepository.findAll(
                specification,
                pageable
        );

        return toPageResponse(tasks);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(
            Long taskId,
            Jwt jwt
    ) {
        User user = getCurrentUser(jwt);

        Task task = getOwnedTask(
                taskId,
                user.getId()
        );

        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(
            Long taskId,
            TaskRequest request,
            Jwt jwt
    ) {
        User user = getCurrentUser(jwt);

        Task task = getOwnedTask(
                taskId,
                user.getId()
        );

        task.update(
                request.title().trim(),
                request.description(),
                request.status(),
                request.dueDate()
        );

        taskRepository.flush();

        return toResponse(task);
    }

    @Transactional
    public void deleteTask(
            Long taskId,
            Jwt jwt
    ) {
        User user = getCurrentUser(jwt);

        Task task = getOwnedTask(
                taskId,
                user.getId()
        );

        taskRepository.delete(task);
    }
}
