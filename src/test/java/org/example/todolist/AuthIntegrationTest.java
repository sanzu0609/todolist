package org.example.todolist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.todolist.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void unauthenticatedRequestIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/tasks"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void fullRegistrationLoginAndProfileFlow() throws Exception {
        String username = "integrationUser";
        String password = "Password123";

        mockMvc.perform(
                post("/register")
                    .with(csrf())
                    .param("username", username)
                    .param("password", password)
                    .param("confirmPassword", password)
                    .param("displayName", "Integration Tester")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));

        MvcResult loginResult = mockMvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", username)
                    .param("password", password)
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/tasks"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/profile").session(session))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("profileForm", "passwordChangeForm"));

        mockMvc.perform(
                post("/profile")
                    .session(session)
                    .with(csrf())
                    .param("displayName", "Updated Name")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile"));

        mockMvc.perform(
                post("/profile/password")
                    .session(session)
                    .with(csrf())
                    .param("oldPassword", password)
                    .param("newPassword", "NewPass456")
                    .param("confirmNewPassword", "NewPass456")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile"));

        mockMvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", username)
                    .param("password", "NewPass456")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/tasks"));
    }
}
