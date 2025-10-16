package org.example.todolist.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.example.todolist.domain.enums.Priority;
import org.springframework.format.annotation.DateTimeFormat;

public record TaskCreateDto(
    @NotBlank(message="{NotBlank.task.title}")
    @Size(max = 255)
    String title,

    @Size(max = 5000)
    String description,

    @NotNull(message="{NotNull.task.priority}")
    Priority priority,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate dueDate
) {
}
