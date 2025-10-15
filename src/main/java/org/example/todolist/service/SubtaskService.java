package org.example.todolist.service;

import java.util.List;
import org.example.todolist.domain.dto.SubtaskCreateDto;
import org.example.todolist.domain.dto.SubtaskUpdateDto;
import org.example.todolist.domain.entity.Subtask;
import org.example.todolist.domain.enums.SubtaskStatus;

public interface SubtaskService {

    List<Subtask> listByTask(Long ownerId, Long taskId);

    Subtask createSubtask(Long ownerId, Long taskId, SubtaskCreateDto dto);

    Subtask updateSubtask(Long ownerId, Long subtaskId, SubtaskUpdateDto dto);

    Subtask changeStatus(Long ownerId, Long subtaskId, SubtaskStatus status);

    void deleteSubtask(Long ownerId, Long subtaskId);

    Subtask getSubtask(Long ownerId, Long subtaskId);
}
