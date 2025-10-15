package org.example.todolist.service;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Objects;
import org.example.todolist.domain.dto.TaskCreateDto;
import org.example.todolist.domain.dto.TaskFilter;
import org.example.todolist.domain.dto.TaskUpdateDto;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.entity.User;
import org.example.todolist.domain.enums.TaskStatus;
import org.example.todolist.repository.TaskRepository;
import org.example.todolist.repository.UserRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Task createTask(Long ownerId, TaskCreateDto dto) {
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + ownerId));

        Task task = new Task();
        task.setOwner(owner);
        applyTaskCreate(dto, task);
        return taskRepository.save(task);
    }

    @Override
    public Task updateTask(Long ownerId, Long taskId, TaskUpdateDto dto) {
        Task task = getTaskForOwner(ownerId, taskId);
        assertVersion(task.getVersion(), dto.version());
        applyTaskUpdate(dto, task);
        return task;
    }

    @Override
    public Task changeStatus(Long ownerId, Long taskId, TaskStatus status, Long expectedVersion) {
        if (status == null) {
            throw new IllegalArgumentException("Task status must not be null");
        }
        Task task = getTaskForOwner(ownerId, taskId);
        assertVersion(task.getVersion(), expectedVersion);
        task.setStatus(status);
        return task;
    }

    @Override
    public void deleteTask(Long ownerId, Long taskId) {
        Task task = getTaskForOwner(ownerId, taskId);
        taskRepository.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Task getTask(Long ownerId, Long taskId) {
        return getTaskForOwner(ownerId, taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Task> listTasks(Long ownerId, TaskFilter filter, Pageable pageable) {
        if (filter == null) {
            return taskRepository.findByOwnerId(ownerId, pageable);
        }
        if (StringUtils.hasText(filter.q())) {
            String query = normalize(filter.q());
            return taskRepository.searchByTitle(ownerId, query, pageable);
        }
        if (filter.status() != null) {
            return taskRepository.findByOwnerIdAndStatus(ownerId, filter.status(), pageable);
        }
        if (filter.priority() != null) {
            return taskRepository.findByOwnerIdAndPriority(ownerId, filter.priority(), pageable);
        }
        if (filter.dueOnOrBefore() != null) {
            LocalDate due = filter.dueOnOrBefore();
            return taskRepository.findByOwnerIdAndDueDateLessThanEqual(ownerId, due, pageable);
        }
        return taskRepository.findByOwnerId(ownerId, pageable);
    }

    private void applyTaskCreate(TaskCreateDto dto, Task task) {
        task.setTitle(normalizeRequired(dto.title(), "title"));
        task.setDescription(normalizeToNull(dto.description()));
        task.setPriority(dto.priority());
        task.setDueDate(dto.dueDate());
        task.setStatus(TaskStatus.TODO);
    }

    private void applyTaskUpdate(TaskUpdateDto dto, Task task) {
        task.setTitle(normalizeRequired(dto.title(), "title"));
        task.setDescription(normalizeToNull(dto.description()));
        task.setPriority(dto.priority());
        task.setDueDate(dto.dueDate());
        task.setStatus(dto.status());
    }

    private Task getTaskForOwner(Long ownerId, Long taskId) {
        return taskRepository.findByIdAndOwnerId(taskId, ownerId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
    }

    private void assertVersion(Long currentVersion, Long expectedVersion) {
        if (expectedVersion == null) {
            return;
        }
        if (!Objects.equals(currentVersion, expectedVersion)) {
            throw new OptimisticLockingFailureException("Task has been modified by another transaction");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Task " + fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeToNull(String value) {
        String normalized = normalize(value);
        return StringUtils.hasText(normalized) ? normalized : null;
    }
}

