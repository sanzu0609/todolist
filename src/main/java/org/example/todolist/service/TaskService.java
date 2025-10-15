package org.example.todolist.service;

import org.example.todolist.domain.dto.TaskCreateDto;
import org.example.todolist.domain.dto.TaskFilter;
import org.example.todolist.domain.dto.TaskUpdateDto;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    Task createTask(Long ownerId, TaskCreateDto dto);

    Task updateTask(Long ownerId, Long taskId, TaskUpdateDto dto);

    Task changeStatus(Long ownerId, Long taskId, TaskStatus status, Long expectedVersion);

    void deleteTask(Long ownerId, Long taskId);

    Task getTask(Long ownerId, Long taskId);

    Page<Task> listTasks(Long ownerId, TaskFilter filter, Pageable pageable);
}
