
package com.example.taskmanagement.repository;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.enums.TaskStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndOwnerId(
            Long id,
            Long ownerId
    );

    Page<Task> findByOwnerId(
            Long ownerId,
            Pageable pageable
    );

    Page<Task> findByOwnerIdAndStatus(
            Long ownerId,
            TaskStatus status,
            Pageable pageable
    );
}
