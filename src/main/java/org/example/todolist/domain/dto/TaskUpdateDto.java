package org.example.todolist.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.example.todolist.domain.enums.Priority;
import org.example.todolist.domain.enums.TaskStatus;

public record TaskUpdateDto(
    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 5000)
    String description,

    @NotNull
    Priority priority,

    LocalDate dueDate,

    @NotNull
    TaskStatus status,

    @NotNull
    Long version
) {
}
