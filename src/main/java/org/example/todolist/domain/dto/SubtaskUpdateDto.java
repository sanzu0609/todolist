package org.example.todolist.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.todolist.domain.enums.SubtaskStatus;

public record SubtaskUpdateDto(
    @NotBlank(message="{NotBlank.subtask.title}")
    @Size(max = 255)
    String title,

    @NotNull(message="{NotNull.subtask.status}")
    SubtaskStatus status
) {
}
