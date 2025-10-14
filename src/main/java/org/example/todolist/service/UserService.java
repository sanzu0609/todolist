package org.example.todolist.service;

import java.util.Optional;
import org.example.todolist.domain.dto.PasswordChangeForm;
import org.example.todolist.domain.dto.ProfileForm;
import org.example.todolist.domain.dto.RegisterForm;
import org.example.todolist.domain.entity.User;
import org.example.todolist.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterForm form) {
        String username = normalizeUsername(form.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setDisplayName(normalizeDisplayName(form.getDisplayName()));

        return userRepository.save(user);
    }

    public User updateDisplayName(Long userId, ProfileForm form) {
        User user = getRequiredUser(userId);
        user.setDisplayName(normalizeDisplayName(form.getDisplayName()));
        return user;
    }

    public void changePassword(Long userId, PasswordChangeForm form) {
        User user = getRequiredUser(userId);
        if (!passwordEncoder.matches(form.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        if (!form.getNewPassword().equals(form.getConfirmNewPassword())) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(normalizeUsername(username));
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return getRequiredUser(userId);
    }

    private User getRequiredUser(Long userId) {
        return userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }

    private String normalizeDisplayName(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return null;
        }
        return displayName.trim();
    }
}
