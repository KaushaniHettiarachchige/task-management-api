
package com.example.taskmanagement.integration;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import com.example.taskmanagement.enums.TaskStatus;

import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.service.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JwtService jwtService;

    private User normalUser;
    private User adminUser;
    private Long normalUserTaskId;

    @BeforeEach
    void setUp() {

        taskRepository.deleteAll();
        userRepository.deleteAll();

        normalUser = userRepository.saveAndFlush(
                new User(
                        "employee@example.com",
                        "test-password-hash",
                        Role.USER
                )
        );

        adminUser = userRepository.saveAndFlush(
                new User(
                        "admin@example.com",
                        "test-password-hash",
                        Role.ADMIN
                )
        );

        Task userTask = taskRepository.saveAndFlush(
                new Task(
                        "Employee Task",
                        "Task owned by normal employee",
                        TaskStatus.TODO,
                        null,
                        normalUser
                )
        );

        normalUserTaskId = userTask.getId();

        taskRepository.saveAndFlush(
                new Task(
                        "Admin's Own Task",
                        "Task owned by administrator",
                        TaskStatus.IN_PROGRESS,
                        null,
                        adminUser
                )
        );
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    // TEST 1: Missing JWT -> 401
    @Test
    void adminEndpoint_withoutJwt_shouldReturn401()
            throws Exception {

        mockMvc.perform(
                get("/api/admin/tasks")
        )
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path")
                .value("/api/admin/tasks"));
    }

    // TEST 2: Normal USER -> 403
    @Test
    void adminEndpoint_asNormalUser_shouldReturn403()
            throws Exception {

        mockMvc.perform(
                get("/api/admin/tasks")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(normalUser)
                        )
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // TEST 3: ADMIN -> 200 and all users' tasks
    @Test
    void adminEndpoint_asAdmin_shouldReturnAllTasks()
            throws Exception {

        mockMvc.perform(
                get("/api/admin/tasks")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(adminUser)
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content.length()").value(2));
    }

    // TEST 4: ADMIN pagination
    @Test
    void adminEndpoint_shouldSupportPagination()
            throws Exception {

        mockMvc.perform(
                get("/api/admin/tasks")
                        .param("page", "0")
                        .param("size", "1")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(adminUser)
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.size").value(1));
    }

    // TEST 5: Invalid pagination -> 400
    @Test
    void adminEndpoint_invalidPageSize_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                get("/api/admin/tasks")
                        .param("size", "101")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(adminUser)
                        )
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    }

    // TEST 6: ADMIN cannot update another user's task
    @Test
    void adminCannotUpdateAnotherUsersTask()
            throws Exception {

        String body = """
                {
                  "title": "Unauthorized Admin Update",
                  "description": "Must be rejected",
                  "status": "DONE",
                  "dueDate": "2026-12-20"
                }
                """;

        mockMvc.perform(
                put("/api/tasks/{id}", normalUserTaskId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(adminUser)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        )
        .andExpect(status().isNotFound());

        Task existingTask = taskRepository
                .findById(normalUserTaskId)
                .orElseThrow();

        assertEquals("Employee Task", existingTask.getTitle());
        assertEquals(TaskStatus.TODO, existingTask.getStatus());
    }

    // TEST 7: ADMIN cannot delete another user's task
    @Test
    void adminCannotDeleteAnotherUsersTask()
            throws Exception {

        mockMvc.perform(
                delete("/api/tasks/{id}", normalUserTaskId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(adminUser)
                        )
        )
        .andExpect(status().isNotFound());

        assertTrue(
                taskRepository.existsById(normalUserTaskId)
        );
    }

    // TEST 8: Normal USER still sees only own tasks
    @Test
    void normalUser_stillSeesOnlyOwnTasks()
            throws Exception {

        mockMvc.perform(
                get("/api/tasks")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(normalUser)
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].ownerId")
                .value(normalUser.getId().intValue()));
    }
}
