package org.example.todolist.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.todolist.domain.enums.SubtaskStatus;

public record SubtaskUpdateDto(
    @NotBlank
    @Size(max = 255)
    String title,

    @NotNull
    SubtaskStatus status
) {
}
