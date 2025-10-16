package org.example.todolist.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.entity.User;
import org.example.todolist.domain.enums.Priority;
import org.example.todolist.domain.enums.SubtaskStatus;
import org.example.todolist.domain.enums.TaskStatus;
import org.example.todolist.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest
@AutoConfigureMockMvc
class TaskCrudIT extends IntegrationTestSupport {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void list_showsOnlyCurrentUsersTasks() throws Exception {
        User owner = createUser("taskowner");
        User other = createUser("otheruser");
        createTask(owner, "Owner Task", TaskStatus.TODO, Priority.HIGH, LocalDate.now().plusDays(3));
        createTask(other, "Other Task", TaskStatus.TODO, Priority.MEDIUM, LocalDate.now().plusDays(5));

        MockHttpSession session = login(owner.getUsername(), DEFAULT_PASSWORD);

        mockMvc.perform(get("/tasks").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Owner Task")))
            .andExpect(content().string(not(containsString("Other Task"))));
    }

    @Test
    void createTask_valid_returnsRedirectAndVisibleInList() throws Exception {
        User owner = createUser("creator");
        MockHttpSession session = login(owner.getUsername(), DEFAULT_PASSWORD);

        mockMvc.perform(
                post("/tasks")
                    .session(session)
                    .with(csrf())
                    .param("title", "New Task")
                    .param("description", "Integration created task")
                    .param("priority", Priority.MEDIUM.name())
                    .param("dueDate", LocalDate.now().plusDays(2).toString())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/tasks"));

        mockMvc.perform(get("/tasks").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("New Task")));
    }

    @Test
    void updateTask_withValidVersion_updates() throws Exception {
        User owner = createUser("updater");
        Task task = createTask(owner, "Original Title", TaskStatus.TODO, Priority.LOW, LocalDate.now().plusDays(4));
        MockHttpSession session = login(owner.getUsername(), DEFAULT_PASSWORD);

        mockMvc.perform(
                post("/tasks/" + task.getId())
                    .session(session)
                    .with(csrf())
                    .param("title", "Updated Title")
                    .param("description", "Updated description")
                    .param("priority", Priority.URGENT.name())
                    .param("dueDate", LocalDate.now().plusDays(1).toString())
                    .param("status", TaskStatus.IN_PROGRESS.name())
                    .param("version", task.getVersion().toString())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/tasks"));

        Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Updated Title");
        assertThat(reloaded.getDescription()).isEqualTo("Updated description");
        assertThat(reloaded.getPriority()).isEqualTo(Priority.URGENT);
        assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateTask_withStaleVersion_returns409() throws Exception {
        User owner = createUser("staleuser");
        Task task = createTask(owner, "Stale Title", TaskStatus.TODO, Priority.LOW, LocalDate.now().plusDays(6));
        Long staleVersion = task.getVersion();
        task.setTitle("Concurrent Update");
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(task);

        MockHttpSession session = login(owner.getUsername(), DEFAULT_PASSWORD);

        mockMvc.perform(
                post("/tasks/" + task.getId())
                    .session(session)
                    .with(csrf())
                    .param("title", "Another Update")
                    .param("description", "Should fail due to stale version")
                    .param("priority", Priority.HIGH.name())
                    .param("dueDate", LocalDate.now().plusDays(2).toString())
                    .param("status", TaskStatus.DONE.name())
                    .param("version", staleVersion.toString())
            )
            .andExpect(status().isConflict())
            .andExpect(view().name("error/409"));

        Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Concurrent Update");
        assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void deleteTask_removesItsSubtasks() throws Exception {
        User owner = createUser("deleter");
        Task task = createTask(owner, "Delete Me", TaskStatus.TODO, Priority.MEDIUM, LocalDate.now().plusDays(5));
        createSubtask(task, "Child Subtask", SubtaskStatus.TODO);
        MockHttpSession session = login(owner.getUsername(), DEFAULT_PASSWORD);

        mockMvc.perform(
                post("/tasks/" + task.getId() + "/delete")
                    .session(session)
                    .with(csrf())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/tasks"));

        assertThat(taskRepository.findById(task.getId())).isEmpty();
        assertThat(subtaskRepository.findByTaskIdAndOwnerId(task.getId(), owner.getId())).isEmpty();
    }
}
