package org.example.todolist.web;

import jakarta.validation.Valid;
import org.example.todolist.domain.dto.SubtaskCreateDto;
import org.example.todolist.domain.dto.SubtaskUpdateDto;
import org.example.todolist.domain.enums.SubtaskStatus;
import org.example.todolist.security.AppUserDetails;
import org.example.todolist.service.SubtaskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SubtaskController {

    private final SubtaskService subtaskService;

    public SubtaskController(SubtaskService subtaskService) {
        this.subtaskService = subtaskService;
    }

    @PostMapping("/tasks/{taskId}/subtasks")
    public String createSubtask(
        @AuthenticationPrincipal AppUserDetails principal,
        @PathVariable Long taskId,
        @Valid SubtaskCreateDto form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("newSubtask", form);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.newSubtask", bindingResult);
            return "redirect:/tasks/" + taskId;
        }
        subtaskService.createSubtask(principal.getId(), taskId, form);
        redirectAttributes.addFlashAttribute("success", "Subtask added.");
        return "redirect:/tasks/" + taskId;
    }

    @PostMapping("/tasks/{taskId}/subtasks/{id}")
    public String updateSubtask(
        @AuthenticationPrincipal AppUserDetails principal,
        @PathVariable Long taskId,
        @PathVariable Long id,
        @Valid SubtaskUpdateDto form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid subtask data.");
            return "redirect:/tasks/" + taskId;
        }
        subtaskService.updateSubtask(principal.getId(), id, form);
        redirectAttributes.addFlashAttribute("success", "Subtask updated.");
        return "redirect:/tasks/" + taskId;
    }

    @PostMapping("/tasks/{taskId}/subtasks/{id}/status")
    public String toggleSubtaskStatus(
        @AuthenticationPrincipal AppUserDetails principal,
        @PathVariable Long taskId,
        @PathVariable Long id,
        @RequestParam("status") SubtaskStatus status,
        RedirectAttributes redirectAttributes
    ) {
        subtaskService.changeStatus(principal.getId(), id, status);
        redirectAttributes.addFlashAttribute("success", "Subtask status updated.");
        return "redirect:/tasks/" + taskId;
    }

    @PostMapping("/tasks/{taskId}/subtasks/{id}/delete")
    public String deleteSubtask(
        @AuthenticationPrincipal AppUserDetails principal,
        @PathVariable Long taskId,
        @PathVariable Long id,
        RedirectAttributes redirectAttributes
    ) {
        subtaskService.deleteSubtask(principal.getId(), id);
        redirectAttributes.addFlashAttribute("success", "Subtask deleted.");
        return "redirect:/tasks/" + taskId;
    }
}
