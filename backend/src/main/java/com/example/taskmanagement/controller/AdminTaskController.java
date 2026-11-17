
package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.TaskPageResponse;
import com.example.taskmanagement.service.AdminTaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminTaskController {

    private final AdminTaskService adminTaskService;

    public AdminTaskController(
            AdminTaskService adminTaskService
    ) {
        this.adminTaskService = adminTaskService;
    }

    // BONUS 3: ADMIN ONLY - VIEW ALL USERS' TASKS
    @Operation(
            summary = "View all users' tasks (ADMIN only)",
            description = """
                    Retrieve paginated tasks across all registered users.

                    Only authenticated users with the ADMIN role
                    can access this endpoint.

                    Regular USER accounts receive 403 Forbidden.
                    Unauthenticated requests receive 401 Unauthorized.

                    The response includes task owner IDs.

                    Example:
                    /api/admin/tasks?page=0&size=10
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "All tasks retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ADMIN permission required"
            )
    })
    @GetMapping("/tasks")
    public ResponseEntity<TaskPageResponse> getAllTasks(

            @Parameter(
                    description = "Page number starting from 0",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(
                    description = "Number of tasks per page (1 to 100)",
                    example = "10"
            )
            @RequestParam(defaultValue = "10")
            int size
    ) {
        return ResponseEntity.ok(
                adminTaskService.getAllTasks(page, size)
        );
    }
}
