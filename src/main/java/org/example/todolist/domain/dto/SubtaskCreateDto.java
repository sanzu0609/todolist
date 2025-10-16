package org.example.todolist.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubtaskCreateDto(
    @NotBlank(message="{NotBlank.subtask.title}")
    @Size(max = 255)
    String title
) {
}
