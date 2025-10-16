package org.example.todolist.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.enums.Priority;
import org.example.todolist.domain.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    Page<Task> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);

    Page<Task> findByOwnerIdAndPriority(Long ownerId, Priority priority, Pageable pageable);

    Page<Task> findByOwnerIdAndDueDateLessThanEqual(Long ownerId, LocalDate dueDate, Pageable pageable);

    @Query("select t from Task t where t.owner.id = :ownerId and lower(t.title) like lower(concat('%', :q, '%'))")
    Page<Task> searchByTitle(@Param("ownerId") Long ownerId, @Param("q") String query, Pageable pageable);

    Optional<Task> findByOwnerIdAndTitle(Long ownerId, String title);
}
