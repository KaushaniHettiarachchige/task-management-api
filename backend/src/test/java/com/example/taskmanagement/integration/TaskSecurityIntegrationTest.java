
package com.example.taskmanagement.integration;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.TaskStatus;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    private User userA;
    private User userB;
    private Long userATaskId;

    @BeforeEach
    void setUp() {

        taskRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.saveAndFlush(
                new User(
                        "usera@example.com",
                        "test-password-hash"
                )
        );

        userB = userRepository.saveAndFlush(
                new User(
                        "userb@example.com",
                        "test-password-hash"
                )
        );

        Task task = taskRepository.saveAndFlush(
                new Task(
                        "User A Private Task",
                        "Only User A can access",
                        TaskStatus.TODO,
                        null,
                        userA
                )
        );

        userATaskId = task.getId();
    }

    // TEST 1: Missing JWT must be rejected
    @Test
    void getTasks_withoutJwt_shouldReturn401() throws Exception {

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/tasks"));
    }

    // TEST 2: User A can create own task
    @Test
    void createTask_withJwt_shouldAssignCorrectOwner()
            throws Exception {

        String requestBody = """
                {
                  "title": "New Task",
                  "description": "Created by User A",
                  "status": "TODO",
                  "dueDate": "2026-11-20"
                }
                """;

        mockMvc.perform(
                post("/api/tasks")
                        .with(jwt().jwt(token ->
                                token.subject(userA.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("New Task"))
        .andExpect(jsonPath("$.ownerId").value(
                userA.getId().intValue()
        ));

        List<Task> tasks = taskRepository.findAll();

        assertTrue(
                tasks.stream().anyMatch(task ->
                        task.getTitle().equals("New Task")
                        && task.getOwner().getId()
                                .equals(userA.getId())
                )
        );
    }

    // TEST 3: User B cannot read User A's task
    @Test
    void getTask_asAnotherUser_shouldReturn404()
            throws Exception {

        mockMvc.perform(
                get("/api/tasks/{id}", userATaskId)
                        .with(jwt().jwt(token ->
                                token.subject(userB.getEmail())
                        ))
        )
        .andExpect(status().isNotFound());
    }

    // TEST 4: User B cannot update User A's task
    @Test
    void updateTask_asAnotherUser_shouldReturn404()
            throws Exception {

        String requestBody = """
                {
                  "title": "Unauthorized Update",
                  "description": "Should be rejected",
                  "status": "DONE",
                  "dueDate": "2026-11-20"
                }
                """;

        mockMvc.perform(
                put("/api/tasks/{id}", userATaskId)
                        .with(jwt().jwt(token ->
                                token.subject(userB.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
        .andExpect(status().isNotFound());

        Task originalTask = taskRepository
                .findById(userATaskId)
                .orElseThrow();

        assertEquals(
                "User A Private Task",
                originalTask.getTitle()
        );

        assertEquals(
                TaskStatus.TODO,
                originalTask.getStatus()
        );
    }

    // TEST 5: User B cannot delete User A's task
    @Test
    void deleteTask_asAnotherUser_shouldReturn404()
            throws Exception {

        mockMvc.perform(
                delete("/api/tasks/{id}", userATaskId)
                        .with(jwt().jwt(token ->
                                token.subject(userB.getEmail())
                        ))
        )
        .andExpect(status().isNotFound());

        assertTrue(taskRepository.existsById(userATaskId));
    }

    // TEST 6: User B listing excludes User A tasks
    @Test
    void getTasks_shouldOnlyReturnOwnTasks()
            throws Exception {

        mockMvc.perform(
                get("/api/tasks")
                        .with(jwt().jwt(token ->
                                token.subject(userB.getEmail())
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.content").isEmpty());
    }
}
