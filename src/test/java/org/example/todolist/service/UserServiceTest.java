package org.example.todolist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.todolist.domain.dto.PasswordChangeForm;
import org.example.todolist.domain.dto.ProfileForm;
import org.example.todolist.domain.dto.RegisterForm;
import org.example.todolist.domain.entity.User;
import org.example.todolist.domain.enums.Role;
import org.example.todolist.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_success_encodesPassword_andAssignsRoleUser() {
        RegisterForm form = new RegisterForm();
        form.setUsername("newuser");
        form.setPassword("Password123");
        form.setConfirmPassword("Password123");
        form.setDisplayName(" New User ");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(form);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(persisted.getPasswordHash()).isEqualTo("ENCODED");
        assertThat(saved.getDisplayName()).isEqualTo("New User");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_rejects_duplicateUsername() {
        RegisterForm form = new RegisterForm();
        form.setUsername("existing");
        form.setPassword("Password123");
        form.setConfirmPassword("Password123");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(form));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_rejects_whenConfirmationMismatch() {
        RegisterForm form = new RegisterForm();
        form.setUsername("user");
        form.setPassword("Password123");
        form.setConfirmPassword("Mismatch123");

        when(userRepository.existsByUsername("user")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.register(form));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateDisplayNameTrimsAndSaves() {
        User user = new User();
        user.setUsername("user");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ProfileForm form = new ProfileForm();
        form.setDisplayName("  Alice  ");

        User updated = userService.updateDisplayName(1L, form);

        assertThat(updated.getDisplayName()).isEqualTo("Alice");
    }

    @Test
    void updateDisplayNameClearsWhenBlank() {
        User user = new User();
        user.setUsername("user");
        user.setDisplayName("Existing");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ProfileForm form = new ProfileForm();
        form.setDisplayName("   ");

        userService.updateDisplayName(1L, form);

        assertThat(user.getDisplayName()).isNull();
    }

    @Test
    void changePassword_success_updatesHash() {
        User user = new User();
        user.setUsername("user");
        user.setPasswordHash("OLD_HASH");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "OLD_HASH")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("NEW_HASH");

        PasswordChangeForm form = new PasswordChangeForm();
        form.setOldPassword("OldPass123");
        form.setNewPassword("NewPass456");
        form.setConfirmNewPassword("NewPass456");

        userService.changePassword(1L, form);

        assertThat(user.getPasswordHash()).isEqualTo("NEW_HASH");
    }

    @Test
    void changePassword_rejects_whenOldPasswordWrong() {
        User user = new User();
        user.setPasswordHash("OLD_HASH");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "OLD_HASH")).thenReturn(false);

        PasswordChangeForm form = new PasswordChangeForm();
        form.setOldPassword("wrong");
        form.setNewPassword("NewPass456");
        form.setConfirmNewPassword("NewPass456");

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1L, form));
    }

    @Test
    void changePassword_rejects_whenConfirmationMismatch() {
        User user = new User();
        user.setPasswordHash("OLD_HASH");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "OLD_HASH")).thenReturn(true);

        PasswordChangeForm form = new PasswordChangeForm();
        form.setOldPassword("OldPass123");
        form.setNewPassword("NewPass456");
        form.setConfirmNewPassword("Mismatch789");

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1L, form));
    }
}
