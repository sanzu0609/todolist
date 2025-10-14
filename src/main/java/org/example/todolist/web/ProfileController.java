package org.example.todolist.web;

import jakarta.validation.Valid;
import org.example.todolist.domain.dto.PasswordChangeForm;
import org.example.todolist.domain.dto.ProfileForm;
import org.example.todolist.domain.entity.User;
import org.example.todolist.security.AppUserDetails;
import org.example.todolist.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("profileForm")
    public ProfileForm profileForm() {
        return new ProfileForm();
    }

    @ModelAttribute("passwordChangeForm")
    public PasswordChangeForm passwordChangeForm() {
        return new PasswordChangeForm();
    }

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        User user = userService.getById(principal.getId());
        ProfileForm form = new ProfileForm();
        form.setDisplayName(user.getDisplayName());
        model.addAttribute("profileForm", form);
        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
        @AuthenticationPrincipal AppUserDetails principal,
        @Valid ProfileForm form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
            return "user/profile";
        }
        userService.updateDisplayName(principal.getId(), form);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(
        @AuthenticationPrincipal AppUserDetails principal,
        @Valid PasswordChangeForm form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profileForm", new ProfileForm());
            return "user/profile";
        }
        try {
            userService.changePassword(principal.getId(), form);
            redirectAttributes.addFlashAttribute("success", "Password updated successfully.");
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("oldPassword", "password.error", ex.getMessage());
            model.addAttribute("profileForm", new ProfileForm());
            model.addAttribute("passwordChangeForm", form);
            return "user/profile";
        }
    }
}