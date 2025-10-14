package org.example.todolist.web;

import jakarta.validation.Valid;
import org.example.todolist.domain.dto.RegisterForm;
import org.example.todolist.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("registerForm")
    public RegisterForm registerForm() {
        return new RegisterForm();
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
        @Valid RegisterForm form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("registerForm", form);
            return "auth/register";
        }
        try {
            userService.register(form);
            redirectAttributes.addFlashAttribute("success", "Registration successful. Please log in.");
            return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("username", "register.error", ex.getMessage());
            model.addAttribute("registerForm", form);
            return "auth/register";
        }
    }
}