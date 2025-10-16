package org.example.todolist.integration;

import java.time.LocalDate;
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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    protected static final String DEFAULT_PASSWORD = "Password123!";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TaskRepository taskRepository;

    @Autowired
    protected SubtaskRepository subtaskRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        subtaskRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setDisplayName(username + " User");
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    protected Task createTask(
        User owner,
        String title,
        TaskStatus status,
        Priority priority,
        LocalDate dueDate
    ) {
        Task task = new Task();
        task.setOwner(owner);
        task.setTitle(title);
        task.setDescription(title + " description");
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDate(dueDate);
        return taskRepository.save(task);
    }

    protected Subtask createSubtask(Task task, String title, SubtaskStatus status) {
        Subtask subtask = new Subtask();
        subtask.setTask(task);
        subtask.setTitle(title);
        subtask.setStatus(status);
        Subtask saved = subtaskRepository.save(subtask);
        task.getSubtasks().add(saved);
        return saved;
    }

    protected MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/login")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .param("username", username)
                    .param("password", password)
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/tasks"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }
}
