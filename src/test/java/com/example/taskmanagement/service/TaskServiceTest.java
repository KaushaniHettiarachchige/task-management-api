
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Jwt jwt;

    private TaskService taskService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository,
                userRepository
        );

        currentUser = mock(User.class);
    }

    private void mockCurrentUser() {
        when(jwt.getSubject())
                .thenReturn("test@example.com");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(currentUser));
    }

    // TEST 1 - Create task with correct owner
    @Test
    void createTask_shouldAssignLoggedInUserAsOwner() {

        mockCurrentUser();

        when(currentUser.getId()).thenReturn(1L);

        TaskRequest request = new TaskRequest(
                "Complete Assignment",
                "Spring Boot backend",
                TaskStatus.TODO,
                LocalDate.of(2026, 11, 15)
        );

        when(taskRepository.saveAndFlush(any(Task.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        TaskResponse response = taskService.createTask(
                request,
                jwt
        );

        ArgumentCaptor<Task> captor =
                ArgumentCaptor.forClass(Task.class);

        verify(taskRepository).saveAndFlush(captor.capture());

        Task savedTask = captor.getValue();

        assertSame(currentUser, savedTask.getOwner());
        assertEquals("Complete Assignment", response.title());
        assertEquals(TaskStatus.TODO, response.status());
        assertEquals(1L, response.ownerId());
    }

    // TEST 2 - Retrieve own task by ID
    @Test
    void getTaskById_shouldReturnOwnedTask() {

        mockCurrentUser();
        when(currentUser.getId()).thenReturn(1L);

        Task task = new Task(
                "My Private Task",
                "Task description",
                TaskStatus.TODO,
                LocalDate.of(2026, 11, 15),
                currentUser
        );

        when(taskRepository.findByIdAndOwnerId(10L, 1L))
                .thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTaskById(
                10L,
                jwt
        );

        assertEquals("My Private Task", response.title());
        assertEquals(TaskStatus.TODO, response.status());
        assertEquals(1L, response.ownerId());

        verify(taskRepository).findByIdAndOwnerId(10L, 1L);
    }

    // TEST 3 - Reject another user's task
    @Test
    void getTaskById_shouldRejectTaskNotOwnedByUser() {

        mockCurrentUser();
        when(currentUser.getId()).thenReturn(1L);

        when(taskRepository.findByIdAndOwnerId(20L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskService.getTaskById(20L, jwt)
        );

        verify(taskRepository).findByIdAndOwnerId(20L, 1L);
    }

    // TEST 4 - Get own tasks with pagination
    @Test
    void getTasks_shouldReturnPaginatedOwnTasks() {

        mockCurrentUser();
        when(currentUser.getId()).thenReturn(1L);

        Task task1 = new Task(
                "Task One",
                "Description One",
                TaskStatus.TODO,
                null,
                currentUser
        );

        Task task2 = new Task(
                "Task Two",
                "Description Two",
                TaskStatus.IN_PROGRESS,
                null,
                currentUser
        );

        Page<Task> taskPage = new PageImpl<>(
                List.of(task1, task2)
        );

        when(taskRepository.findByOwnerId(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(taskPage);

        TaskPageResponse response = taskService.getTasks(
                jwt,
                0,
                2,
                null
        );

        assertEquals(2, response.content().size());
        assertEquals("Task One", response.content().get(0).title());
        assertEquals("Task Two", response.content().get(1).title());

        verify(taskRepository).findByOwnerId(
                eq(1L),
                argThat(pageable ->
                        pageable.getPageNumber() == 0 &&
                        pageable.getPageSize() == 2
                )
        );
    }

    // TEST 5 - Filter tasks by status
    @Test
    void getTasks_shouldFilterByStatus() {

        mockCurrentUser();
        when(currentUser.getId()).thenReturn(1L);

        Task doneTask = new Task(
                "Completed Task",
                "Finished work",
                TaskStatus.DONE,
                null,
                currentUser
        );

        Page<Task> taskPage = new PageImpl<>(
                List.of(doneTask)
        );

        when(taskRepository.findByOwnerIdAndStatus(
                eq(1L),
                eq(TaskStatus.DONE),
                any(Pageable.class)
        )).thenReturn(taskPage);

        TaskPageResponse response = taskService.getTasks(
                jwt,
                0,
                10,
                TaskStatus.DONE
        );

        assertEquals(1, response.content().size());
        assertEquals(
                TaskStatus.DONE,
                response.content().get(0).status()
        );

        verify(taskRepository).findByOwnerIdAndStatus(
                eq(1L),
                eq(TaskStatus.DONE),
                any(Pageable.class)
        );
    }

    // TEST 6 - Update owned task
    @Test
    void updateTask_shouldUpdateOwnedTask() {

        mockCurrentUser();
        when(currentUser.getId()).thenReturn(1L);

        Task task = new Task(
                "Old Title",
                "Old Description",
                TaskStatus.TODO,
                null,
                currentUser
        );

        when(taskRepository.findByIdAndOwnerId(5L, 1L))
                .thenReturn(Optional.of(task));

        TaskRequest request = new TaskRequest(
                "Updated Title",
                "Updated Description",
                TaskStatus.DONE,
                LocalDate.of(2026, 11, 20)
        );

        TaskResponse response = taskService.updateTask(
                5L,
                request,
                jwt
        );

        assertEquals("Updated Title", response.title());
        assertEquals("Updated Description", response.description());
        assertEquals(TaskStatus.DONE, response.status());
        assertEquals(
                LocalDate.of(2026, 11, 20),
                response.dueDate()
        );

        verify(taskRepository).flush();
    }

    // TEST 7 - Delete owned task
    @Test
    void deleteTask_shouldDeleteOwnedTask() {

        mockCurrentUser();
        when(currentUser.getId()).thenReturn(1L);

        Task task = new Task(
                "Task to Delete",
                "Delete description",
                TaskStatus.TODO,
                null,
                currentUser
        );

        when(taskRepository.findByIdAndOwnerId(8L, 1L))
                .thenReturn(Optional.of(task));

        taskService.deleteTask(8L, jwt);

        verify(taskRepository).delete(task);

        verify(taskRepository).findByIdAndOwnerId(8L, 1L);
    }
}
