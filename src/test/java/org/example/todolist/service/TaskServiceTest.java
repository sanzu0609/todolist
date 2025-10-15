package org.example.todolist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import org.example.todolist.domain.dto.TaskCreateDto;
import org.example.todolist.domain.dto.TaskFilter;
import org.example.todolist.domain.dto.TaskUpdateDto;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.entity.User;
import org.example.todolist.domain.enums.Priority;
import org.example.todolist.domain.enums.TaskStatus;
import org.example.todolist.repository.TaskRepository;
import org.example.todolist.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        ReflectionTestUtils.setField(owner, "id", 42L);
    }

    @Test
    void createTaskSetsOwnerAndDefaults() {
        TaskCreateDto dto = new TaskCreateDto(
            "  Plan sprint  ",
            " Discuss with the team ",
            Priority.HIGH,
            LocalDate.now()
        );

        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            ReflectionTestUtils.setField(task, "id", 100L);
            ReflectionTestUtils.setField(task, "version", 0L);
            return task;
        });

        Task result = taskService.createTask(42L, dto);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task persisted = captor.getValue();

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getOwner()).isEqualTo(owner);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(persisted.getTitle()).isEqualTo("Plan sprint");
        assertThat(persisted.getDescription()).isEqualTo("Discuss with the team");
        assertThat(persisted.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(persisted.getDueDate()).isEqualTo(dto.dueDate());
    }

    @Test
    void createTaskThrowsWhenOwnerMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        TaskCreateDto dto = new TaskCreateDto("Title", null, Priority.LOW, null);

        assertThrows(EntityNotFoundException.class, () -> taskService.createTask(99L, dto));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTaskAppliesChangesWhenVersionMatches() {
        Task task = new Task();
        task.setOwner(owner);
        ReflectionTestUtils.setField(task, "version", 5L);

        when(taskRepository.findByIdAndOwnerId(10L, 42L)).thenReturn(Optional.of(task));

        TaskUpdateDto dto = new TaskUpdateDto(
            "Updated title",
            "Updated description",
            Priority.URGENT,
            LocalDate.now().plusDays(3),
            TaskStatus.IN_PROGRESS,
            5L
        );

        Task updated = taskService.updateTask(42L, 10L, dto);

        assertThat(updated.getTitle()).isEqualTo("Updated title");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getPriority()).isEqualTo(Priority.URGENT);
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(updated.getDueDate()).isEqualTo(dto.dueDate());
    }

    @Test
    void updateTaskThrowsWhenVersionMismatch() {
        Task task = new Task();
        task.setOwner(owner);
        ReflectionTestUtils.setField(task, "version", 3L);
        when(taskRepository.findByIdAndOwnerId(5L, 42L)).thenReturn(Optional.of(task));

        TaskUpdateDto dto = new TaskUpdateDto(
            "Title",
            "Desc",
            Priority.MEDIUM,
            null,
            TaskStatus.DONE,
            2L
        );

        assertThrows(OptimisticLockingFailureException.class, () -> taskService.updateTask(42L, 5L, dto));
    }

    @Test
    void changeStatusRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> taskService.changeStatus(42L, 9L, null, 1L));
    }

    @Test
    void changeStatusUpdatesWhenVersionMatches() {
        Task task = new Task();
        ReflectionTestUtils.setField(task, "version", 1L);
        when(taskRepository.findByIdAndOwnerId(7L, 42L)).thenReturn(Optional.of(task));

        Task result = taskService.changeStatus(42L, 7L, TaskStatus.DONE, 1L);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void deleteTaskRemovesEntity() {
        Task task = new Task();
        when(taskRepository.findByIdAndOwnerId(3L, 42L)).thenReturn(Optional.of(task));

        taskService.deleteTask(42L, 3L);

        verify(taskRepository).delete(task);
    }

    @Test
    void listTasksUsesSearchWhenQueryPresent() {
        TaskFilter filter = new TaskFilter(null, null, null, " sprint ");
        when(taskRepository.searchByTitle(42L, "sprint", PageRequest.of(0, 10)))
            .thenReturn(Page.empty());

        taskService.listTasks(42L, filter, PageRequest.of(0, 10));

        verify(taskRepository).searchByTitle(42L, "sprint", PageRequest.of(0, 10));
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void listTasksReturnsByOwnerWhenFilterNull() {
        Page<Task> page = new PageImpl<>(java.util.List.of());
        when(taskRepository.findByOwnerId(42L, PageRequest.of(0, 5))).thenReturn(page);

        Page<Task> result = taskService.listTasks(42L, null, PageRequest.of(0, 5));

        assertThat(result).isSameAs(page);
        verify(taskRepository).findByOwnerId(42L, PageRequest.of(0, 5));
    }

    @Test
    void getTaskThrowsWhenMissing() {
        when(taskRepository.findByIdAndOwnerId(55L, 42L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> taskService.getTask(42L, 55L));
    }
}
