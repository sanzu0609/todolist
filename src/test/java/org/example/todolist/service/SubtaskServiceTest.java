package org.example.todolist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.example.todolist.domain.dto.SubtaskCreateDto;
import org.example.todolist.domain.dto.SubtaskUpdateDto;
import org.example.todolist.domain.entity.Subtask;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.enums.SubtaskStatus;
import org.example.todolist.repository.SubtaskRepository;
import org.example.todolist.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubtaskServiceTest {

    @Mock
    private SubtaskRepository subtaskRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private SubtaskServiceImpl subtaskService;

    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task();
        ReflectionTestUtils.setField(task, "id", 100L);
    }

    @Test
    void listByTaskDelegatesToRepository() {
        when(taskRepository.findByIdAndOwnerId(100L, 42L)).thenReturn(Optional.of(task));
        when(subtaskRepository.findByTaskIdAndOwnerId(100L, 42L)).thenReturn(List.of());

        List<Subtask> result = subtaskService.listByTask(42L, 100L);

        assertThat(result).isEmpty();
        verify(subtaskRepository).findByTaskIdAndOwnerId(100L, 42L);
    }

    @Test
    void createSubtaskAddsToParentTask() {
        when(taskRepository.findByIdAndOwnerId(100L, 42L)).thenReturn(Optional.of(task));
        when(subtaskRepository.save(any(Subtask.class))).thenAnswer(invocation -> {
            Subtask s = invocation.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 501L);
            return s;
        });

        SubtaskCreateDto dto = new SubtaskCreateDto("  Draft requirements  ");
        Subtask saved = subtaskService.createSubtask(42L, 100L, dto);

        ArgumentCaptor<Subtask> captor = ArgumentCaptor.forClass(Subtask.class);
        verify(subtaskRepository).save(captor.capture());
        Subtask persisted = captor.getValue();

        assertThat(saved.getId()).isEqualTo(501L);
        assertThat(saved.getStatus()).isEqualTo(SubtaskStatus.TODO);
        assertThat(persisted.getTitle()).isEqualTo("Draft requirements");
        assertThat(task.getSubtasks()).contains(saved);
    }

    @Test
    void createSubtaskRejectsBlankTitle() {
        when(taskRepository.findByIdAndOwnerId(100L, 42L)).thenReturn(Optional.of(task));

        SubtaskCreateDto dto = new SubtaskCreateDto("   ");
        assertThrows(IllegalArgumentException.class, () -> subtaskService.createSubtask(42L, 100L, dto));
        verify(subtaskRepository, never()).save(any());
    }

    @Test
    void updateSubtaskAppliesChanges() {
        Subtask subtask = new Subtask();
        subtask.setTask(task);
        when(subtaskRepository.findByIdAndOwnerId(10L, 42L)).thenReturn(Optional.of(subtask));

        SubtaskUpdateDto dto = new SubtaskUpdateDto("Update docs", SubtaskStatus.DONE);

        Subtask updated = subtaskService.updateSubtask(42L, 10L, dto);

        assertThat(updated.getTitle()).isEqualTo("Update docs");
        assertThat(updated.getStatus()).isEqualTo(SubtaskStatus.DONE);
    }

    @Test
    void changeStatusUpdatesEntity() {
        Subtask subtask = new Subtask();
        subtask.setTask(task);
        when(subtaskRepository.findByIdAndOwnerId(50L, 42L)).thenReturn(Optional.of(subtask));

        Subtask result = subtaskService.changeStatus(42L, 50L, SubtaskStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(SubtaskStatus.IN_PROGRESS);
    }

    @Test
    void changeStatusRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> subtaskService.changeStatus(42L, 1L, null));
    }

    @Test
    void deleteSubtaskDelegatesToRepository() {
        Subtask subtask = new Subtask();
        subtask.setTask(task);
        when(subtaskRepository.findByIdAndOwnerId(70L, 42L)).thenReturn(Optional.of(subtask));

        subtaskService.deleteSubtask(42L, 70L);

        verify(subtaskRepository).delete(subtask);
    }

    @Test
    void getSubtaskThrowsWhenNotFound() {
        when(subtaskRepository.findByIdAndOwnerId(5L, 42L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> subtaskService.getSubtask(42L, 5L));
    }
}
