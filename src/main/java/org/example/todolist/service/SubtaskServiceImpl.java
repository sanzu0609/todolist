package org.example.todolist.service;

import org.example.todolist.exception.EntityNotFoundException;
import org.example.todolist.exception.BusinessException;
import java.util.List;
import org.example.todolist.domain.dto.SubtaskCreateDto;
import org.example.todolist.domain.dto.SubtaskUpdateDto;
import org.example.todolist.domain.entity.Subtask;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.enums.SubtaskStatus;
import org.example.todolist.repository.SubtaskRepository;
import org.example.todolist.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class SubtaskServiceImpl implements SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;

    public SubtaskServiceImpl(SubtaskRepository subtaskRepository, TaskRepository taskRepository) {
        this.subtaskRepository = subtaskRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subtask> listByTask(Long ownerId, Long taskId) {
        ensureTaskExists(ownerId, taskId);
        return subtaskRepository.findByTaskIdAndOwnerId(taskId, ownerId);
    }

    @Override
    public Subtask createSubtask(Long ownerId, Long taskId, SubtaskCreateDto dto) {
        Task task = ensureTaskExists(ownerId, taskId);
        Subtask subtask = new Subtask();
        subtask.setTask(task);
        subtask.setTitle(normalize(dto.title()));
        subtask.setStatus(SubtaskStatus.TODO);
        Subtask saved = subtaskRepository.save(subtask);
        task.getSubtasks().add(saved);
        return saved;
    }

    @Override
    public Subtask updateSubtask(Long ownerId, Long subtaskId, SubtaskUpdateDto dto) {
        Subtask subtask = getSubtaskForOwner(ownerId, subtaskId);
        subtask.setTitle(normalize(dto.title()));
        subtask.setStatus(dto.status());
        return subtask;
    }

    @Override
    public Subtask changeStatus(Long ownerId, Long subtaskId, SubtaskStatus status) {
        if (status == null) {
            throw new BusinessException("Subtask status must not be null");
        }
        Subtask subtask = getSubtaskForOwner(ownerId, subtaskId);
        subtask.setStatus(status);
        return subtask;
    }

    @Override
    public void deleteSubtask(Long ownerId, Long subtaskId) {
        Subtask subtask = getSubtaskForOwner(ownerId, subtaskId);
        subtaskRepository.delete(subtask);
    }

    @Override
    @Transactional(readOnly = true)
    public Subtask getSubtask(Long ownerId, Long subtaskId) {
        return getSubtaskForOwner(ownerId, subtaskId);
    }

    private Task ensureTaskExists(Long ownerId, Long taskId) {
        return taskRepository.findByIdAndOwnerId(taskId, ownerId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
    }

    private Subtask getSubtaskForOwner(Long ownerId, Long subtaskId) {
        return subtaskRepository.findByIdAndOwnerId(subtaskId, ownerId)
            .orElseThrow(() -> new EntityNotFoundException("Subtask not found: " + subtaskId));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BusinessException("Subtask title must not be blank");
        }
        return trimmed;
    }
}
