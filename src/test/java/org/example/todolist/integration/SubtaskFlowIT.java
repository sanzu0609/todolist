package org.example.todolist.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import org.example.todolist.domain.entity.Subtask;
import org.example.todolist.domain.entity.Task;
import org.example.todolist.domain.entity.User;
import org.example.todolist.domain.enums.Priority;
import org.example.todolist.domain.enums.SubtaskStatus;
import org.example.todolist.domain.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest
@AutoConfigureMockMvc
class SubtaskFlowIT extends IntegrationTestSupport {

    @Test
    void createSubtask_forOwnTask_ok() throws Exception {
        User owner = createUser("subtasker");
        Task task = createTask(owner, "Parent Task", TaskStatus.TODO, Priority.MEDIUM, LocalDate.now().plusDays(3));
        MockHttpSession session = login(owner.getUsername(), DEFAULT_PASSWORD);

        mockMvc.perform(
                post("/tasks/" + task.getId() + "/subtasks")
                    .session(session)
                    .with(csrf())
                    .param("title", "New Subtask")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/tasks/" + task.getId()));

        assertThat(subtaskRepository.findByTaskIdAndOwnerId(task.getId(), owner.getId()))
            .anyMatch(subtask -> subtask.getTitle().equals("New Subtask"));
    }

    @Test
    void toggleSubtaskStatus_ok() throws Exception {
        User owner = createUser("toggleuser");
        Task task = createTask(owner, "Task With Subtask", TaskStatus.TODO, Priority.HIGH, LocalDate.now().plusDays(4));
        Subtask subtask = createSubtask(task, "Existing Subtask", SubtaskStatus.TODO);
        MockHttpSession session = login(owner.getUsername(), DEFAULT_PASSWORD);

        mockMvc.perform(
                post("/tasks/" + task.getId() + "/subtasks/" + subtask.getId() + "/status")
                    .session(session)
                    .with(csrf())
                    .param("status", SubtaskStatus.IN_PROGRESS.name())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/tasks/" + task.getId()));

        Subtask reloaded = subtaskRepository.findById(subtask.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubtaskStatus.IN_PROGRESS);
    }

    @Test
    void accessSubtask_ofOtherUser_returns404() throws Exception {
        User owner = createUser("owner");
        Task task = createTask(owner, "Owner Task", TaskStatus.TODO, Priority.LOW, LocalDate.now().plusDays(2));
        Subtask subtask = createSubtask(task, "Owner Subtask", SubtaskStatus.TODO);

        User intruder = createUser("intruder");
        MockHttpSession session = login(intruder.getUsername(), DEFAULT_PASSWORD);

        mockMvc.perform(
                post("/tasks/" + task.getId() + "/subtasks/" + subtask.getId() + "/status")
                    .session(session)
                    .with(csrf())
                    .param("status", SubtaskStatus.DONE.name())
            )
            .andExpect(status().isNotFound())
            .andExpect(view().name("error/404"));
    }
}
