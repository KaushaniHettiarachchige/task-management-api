package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.TaskPageResponse;
import com.example.taskmanagement.dto.TaskRequest;
import com.example.taskmanagement.dto.TaskResponse;
import com.example.taskmanagement.enums.TaskStatus;
import com.example.taskmanagement.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // Create Task
    @Operation(
            summary = "Create a new task",
            description = "Create a task for the authenticated user. "
                    + "Requires a valid JWT access token."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Task created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid task request"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TaskResponse response = taskService.createTask(
                request,
                jwt
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get Tasks Search, Filter, Sort, Paginate
    @Operation(
            summary = "Get authenticated user's tasks",
            description = """
                    Retrieve tasks belonging to the authenticated user.

                    Supported features:
                    - Pagination using page and size
                    - Filtering by task status
                    - Case-insensitive keyword search in title and description
                    - Sorting by createdAt or dueDate
                    - Ascending or descending sorting direction

                    Example:
                    /api/tasks?keyword=report&status=TODO&sortBy=dueDate&direction=asc&page=0&size=10
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination, filter or sorting parameters"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @GetMapping
    public ResponseEntity<TaskPageResponse> getTasks(

            @Parameter(
                    description = "Page number, starting from 0",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(
                    description = "Number of tasks per page (1 to 100)",
                    example = "10"
            )
            @RequestParam(defaultValue = "10")
            int size,

            @Parameter(
                    description = "Filter by TODO, IN_PROGRESS or DONE"
            )
            @RequestParam(required = false)
            TaskStatus status,

            @Parameter(
                    description = "Search keyword in task title and description",
                    example = "report"
            )
            @RequestParam(required = false)
            String keyword,

            @Parameter(
                    description = "Sort field: createdAt or dueDate",
                    example = "dueDate"
            )
            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @Parameter(
                    description = "Sort direction: asc or desc",
                    example = "asc"
            )
            @RequestParam(defaultValue = "desc")
            String direction,

            @AuthenticationPrincipal
            Jwt jwt
    ) {
        TaskPageResponse response = taskService.getTasks(
                jwt,
                page,
                size,
                status,
                keyword,
                sortBy,
                direction
        );

        return ResponseEntity.ok(response);
    }

    //Get Single Task
    @Operation(
            summary = "Get a task by ID",
            description = "Retrieve a specific task belonging to "
                    + "the authenticated user. Tasks owned by "
                    + "other users are not accessible."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found or not owned by user"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(

            @Parameter(
                    description = "Unique task ID",
                    example = "1"
            )
            @PathVariable Long id,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                taskService.getTaskById(id, jwt)
        );
    }

    //Update Task
    @Operation(
            summary = "Update an existing task",
            description = "Update the title, description, status "
                    + "and due date of a task belonging to "
                    + "the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid task request"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found or not owned by user"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(

            @Parameter(
                    description = "ID of the task to update",
                    example = "1"
            )
            @PathVariable Long id,

            @Valid @RequestBody TaskRequest request,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                taskService.updateTask(
                        id,
                        request,
                        jwt
                )
        );
    }

    //Delete Task
    @Operation(
            summary = "Delete a task",
            description = "Permanently delete a task belonging "
                    + "to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Task deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found or not owned by user"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(

            @Parameter(
                    description = "ID of the task to delete",
                    example = "1"
            )
            @PathVariable Long id,

            @AuthenticationPrincipal Jwt jwt
    ) {
        taskService.deleteTask(id, jwt);

        return ResponseEntity.noContent().build();
    }
}
