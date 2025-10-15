package org.example.todolist.domain.dto;

import org.example.todolist.domain.enums.Priority;
import org.example.todolist.domain.enums.TaskStatus;
import java.time.LocalDate;

public record TaskFilter(
    TaskStatus status,
    Priority priority,
    LocalDate dueOnOrBefore,
    String q
) {
}
