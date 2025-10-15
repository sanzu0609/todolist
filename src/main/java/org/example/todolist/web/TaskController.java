package org.example.todolist.web;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Optional;
import org.example.todolist.domain.dto.TaskCreateDto;
import org.example.todolist.domain.dto.TaskFilter;
import org.example.todolist.domain.dto.TaskUpdateDto;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.enums.Priority;
import org.example.todolist.domain.enums.TaskStatus;
import org.example.todolist.security.AppUserDetails;
import org.example.todolist.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    public String list(
        @AuthenticationPrincipal AppUserDetails principal,
        @RequestParam(name = "status", required = false) TaskStatus status,
        @RequestParam(name = "priority", required = false) Priority priority,
        @RequestParam(name = "dueOnOrBefore", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueOnOrBefore,
        @RequestParam(name = "q", required = false) String query,
        @PageableDefault(size = 10) Pageable pageable,
        Model model
    ) {
        Long ownerId = principal.getId();
        TaskFilter filter = new TaskFilter(status, priority, dueOnOrBefore, normalize(query));
        Page<Task> page = taskService.listTasks(ownerId, filter, pageable);

        model.addAttribute("page", page);
        model.addAttribute("filter", filter);
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", TaskStatus.values());

        return "task/list";
    }

    @GetMapping("/tasks/new")
    public String newTaskForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new TaskCreateDto("", "", Priority.MEDIUM, null));
        }
        populateFormModel(model, false);
        model.addAttribute("formAction", "/tasks");
        model.addAttribute("formTitle", "Create Task");
        return "task/form";
    }

    @PostMapping("/tasks")
    public String createTask(
        @AuthenticationPrincipal AppUserDetails principal,
        @Valid @ModelAttribute("form") TaskCreateDto form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, false);
            model.addAttribute("formAction", "/tasks");
            model.addAttribute("formTitle", "Create Task");
            return "task/form";
        }

        taskService.createTask(principal.getId(), form);
        redirectAttributes.addFlashAttribute("success", "Task created successfully.");
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTaskForm(
        @AuthenticationPrincipal AppUserDetails principal,
        @PathVariable("id") Long id,
        Model model
    ) {
        if (!model.containsAttribute("form")) {
            Task task = taskService.getTask(principal.getId(), id);
            TaskUpdateDto dto = new TaskUpdateDto(
                task.getTitle(),
                Optional.ofNullable(task.getDescription()).orElse(""),
                task.getPriority(),
                task.getDueDate(),
                task.getStatus(),
                task.getVersion()
            );
            model.addAttribute("form", dto);
        }
        populateFormModel(model, true);
        model.addAttribute("formAction", "/tasks/" + id);
        model.addAttribute("formTitle", "Edit Task");
        model.addAttribute("taskId", id);
        return "task/form";
    }

    @PostMapping("/tasks/{id}")
    public String updateTask(
        @AuthenticationPrincipal AppUserDetails principal,
        @PathVariable("id") Long id,
        @Valid @ModelAttribute("form") TaskUpdateDto form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            populateFormModel(model, true);
            model.addAttribute("formAction", "/tasks/" + id);
            model.addAttribute("formTitle", "Edit Task");
            model.addAttribute("taskId", id);
            return "task/form";
        }

        taskService.updateTask(principal.getId(), id, form);
        redirectAttributes.addFlashAttribute("success", "Task updated successfully.");
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/status")
    public String changeStatus(
        @AuthenticationPrincipal AppUserDetails principal,
        @PathVariable("id") Long id,
        @RequestParam("status") TaskStatus status,
        @RequestParam(name = "version", required = false) Long version,
        RedirectAttributes redirectAttributes
    ) {
        taskService.changeStatus(principal.getId(), id, status, version);
        redirectAttributes.addFlashAttribute("success", "Task status updated.");
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(
        @AuthenticationPrincipal AppUserDetails principal,
        @PathVariable("id") Long id,
        RedirectAttributes redirectAttributes
    ) {
        taskService.deleteTask(principal.getId(), id);
        redirectAttributes.addFlashAttribute("success", "Task deleted.");
        return "redirect:/tasks";
    }

    private void populateFormModel(Model model, boolean isEdit) {
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("isEdit", isEdit);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
