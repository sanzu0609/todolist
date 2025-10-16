package org.example.todolist.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.todolist.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIT extends IntegrationTestSupport {

    @Test
    void whenNotLoggedIn_accessTasks_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/tasks"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void login_withValidUser_redirectsToTasks() throws Exception {
        User user = createUser("authuser");

        mockMvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", user.getUsername())
                    .param("password", DEFAULT_PASSWORD)
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/tasks"));
    }

    @Test
    void logout_clearsSession() throws Exception {
        User user = createUser("logoutuser");
        MockHttpSession session = login(user.getUsername(), DEFAULT_PASSWORD);

        mockMvc.perform(
                post("/logout")
                    .with(csrf())
                    .session(session)
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));

        mockMvc.perform(get("/tasks").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"))
            .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
    }
}
