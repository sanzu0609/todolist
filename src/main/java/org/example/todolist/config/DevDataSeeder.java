package org.example.todolist.config;

import java.time.LocalDate;
import java.util.List;
import org.example.todolist.domain.dto.RegisterForm;
import org.example.todolist.domain.dto.SubtaskCreateDto;
import org.example.todolist.domain.dto.TaskCreateDto;
import org.example.todolist.domain.enums.Priority;
import org.example.todolist.service.SubtaskService;
import org.example.todolist.service.TaskService;
import org.example.todolist.service.UserService;
import org.example.todolist.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final TaskService taskService;
    private final SubtaskService subtaskService;

    public DevDataSeeder(
        UserRepository userRepository,
        UserService userService,
        TaskService taskService,
        SubtaskService subtaskService
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.taskService = taskService;
        this.subtaskService = subtaskService;
    }

    @Override
    public void run(String... args) {
        Long demoUserId = ensureDemoUser();
        seedTasks(demoUserId);
    }

    private Long ensureDemoUser() {
        return userRepository.findByUsername("demo")
            .map(user -> user.getId())
            .orElseGet(() -> {
                RegisterForm form = new RegisterForm();
                form.setUsername("demo");
                form.setPassword("demo1234");
                form.setConfirmPassword("demo1234");
                form.setDisplayName("Demo User");
                return userService.register(form).getId();
            });
    }

    private void seedTasks(Long ownerId) {
        if (!taskService.listTasks(ownerId, null, org.springframework.data.domain.PageRequest.of(0, 1)).isEmpty()) {
            return;
        }

        TaskCreateDto todayTask = new TaskCreateDto(
            "Plan sprint backlog",
            "Review tasks with the team and prioritize the next sprint backlog.",
            Priority.HIGH,
            LocalDate.now()
        );
        Long planningTaskId = taskService.createTask(ownerId, todayTask).getId();

        List.of("Review previous sprint", "Collect new requirements", "Draft sprint goal")
            .forEach(title -> subtaskService.createSubtask(ownerId, planningTaskId, new SubtaskCreateDto(title)));

        TaskCreateDto upcomingTask = new TaskCreateDto(
            "Build task management module",
            "Implement task CRUD, filtering, and ownership enforcement.",
            Priority.MEDIUM,
            LocalDate.now().plusDays(5)
        );
        Long buildTaskId = taskService.createTask(ownerId, upcomingTask).getId();

        subtaskService.createSubtask(ownerId, buildTaskId, new SubtaskCreateDto("Design database schema"));
        subtaskService.createSubtask(ownerId, buildTaskId, new SubtaskCreateDto("Implement repository layer"));
        subtaskService.createSubtask(ownerId, buildTaskId, new SubtaskCreateDto("Wire service layer"));
    }
}