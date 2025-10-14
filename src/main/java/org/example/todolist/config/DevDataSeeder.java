package org.example.todolist.config;

import org.example.todolist.domain.dto.RegisterForm;
import org.example.todolist.repository.UserRepository;
import org.example.todolist.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    public DevDataSeeder(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("demo")) {
            return;
        }

        RegisterForm form = new RegisterForm();
        form.setUsername("demo");
        form.setPassword("demo1234");
        form.setConfirmPassword("demo1234");
        form.setDisplayName("Demo User");

        userService.register(form);
    }
}
