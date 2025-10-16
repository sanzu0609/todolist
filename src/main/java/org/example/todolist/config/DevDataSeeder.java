package org.example.todolist.config;

import java.time.LocalDate;
import java.util.List;
import org.example.todolist.domain.entity.Subtask;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.entity.User;
import org.example.todolist.domain.enums.Priority;
import org.example.todolist.domain.enums.Role;
import org.example.todolist.domain.enums.SubtaskStatus;
import org.example.todolist.domain.enums.TaskStatus;
import org.example.todolist.repository.SubtaskRepository;
import org.example.todolist.repository.TaskRepository;
import org.example.todolist.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@Transactional
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
        UserRepository userRepository,
        TaskRepository taskRepository,
        SubtaskRepository subtaskRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.subtaskRepository = subtaskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User demo = ensureUser("demo", "demo1234", "Demo User");
        seedSampleTasks(demo);

        User alice = ensureUser("alice", "alice1234", "Alice");
        seedSampleTasks(alice);
    }

    private User ensureUser(String username, String rawPassword, String displayName) {
        return userRepository.findByUsername(username.trim())
            .orElseGet(() -> {
                User user = new User();
                user.setUsername(username.trim());
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                user.setDisplayName(displayName);
                user.setRole(Role.USER);
                return userRepository.save(user);
            });
    }

    private void seedSampleTasks(User owner) {
        LocalDate today = LocalDate.now();
        List<TaskSeed> seeds = List.of(
            new TaskSeed(
                "Hoc Spring",
                "Lam quen Spring Framework va cac khai niem cot loi.",
                Priority.HIGH,
                TaskStatus.IN_PROGRESS,
                today.plusDays(3),
                List.of(
                    new SubtaskSeed("Doc ve DI", SubtaskStatus.DONE),
                    new SubtaskSeed("Viet demo @Service", SubtaskStatus.TODO)
                )
            ),
            new TaskSeed(
                "Don CV",
                "Chuan bi CV moi nhat de apply cong viec.",
                Priority.MEDIUM,
                TaskStatus.TODO,
                today.plusDays(7),
                List.of(new SubtaskSeed("Cap nhat du an Todo", SubtaskStatus.TODO))
            )
        );

        seeds.forEach(seed -> ensureTask(owner, seed));
    }

    private void ensureTask(User owner, TaskSeed seed) {
        Task task = taskRepository.findByOwnerIdAndTitle(owner.getId(), seed.title())
            .orElseGet(() -> createTask(owner, seed));

        seed.subtasks().forEach(subtaskSeed -> ensureSubtask(task, subtaskSeed));
    }

    private Task createTask(User owner, TaskSeed seed) {
        Task task = new Task();
        task.setOwner(owner);
        task.setTitle(seed.title());
        task.setDescription(seed.description());
        task.setPriority(seed.priority());
        task.setStatus(seed.status());
        task.setDueDate(seed.dueDate());
        return taskRepository.save(task);
    }

    private void ensureSubtask(Task task, SubtaskSeed seed) {
        if (subtaskRepository.existsByTaskIdAndTitle(task.getId(), seed.title())) {
            return;
        }
        Subtask subtask = new Subtask();
        subtask.setTask(task);
        subtask.setTitle(seed.title());
        subtask.setStatus(seed.status());
        Subtask saved = subtaskRepository.save(subtask);
        task.getSubtasks().add(saved);
    }

    private record TaskSeed(
        String title,
        String description,
        Priority priority,
        TaskStatus status,
        LocalDate dueDate,
        List<SubtaskSeed> subtasks
    ) {}

    private record SubtaskSeed(String title, SubtaskStatus status) {}
}
