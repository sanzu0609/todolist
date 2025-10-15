package org.example.todolist.domain.dto;

import java.time.LocalDate;
import org.example.todolist.domain.enums.Priority;
import org.example.todolist.domain.enums.TaskStatus;
import org.springframework.format.annotation.DateTimeFormat;

public record TaskFilter(
    TaskStatus status,
    Priority priority,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate dueOnOrBefore,
    String q
) {
}
