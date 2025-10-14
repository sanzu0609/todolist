package org.example.todolist.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterForm {

    public static final String USERNAME_REGEX = "^[A-Za-z0-9._-]{4,32}$";
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    @NotBlank
    @Pattern(regexp = USERNAME_REGEX, message = "Username must be 4-32 characters and contain only letters, digits, dot, underscore, or hyphen.")
    private String username;

    @NotBlank
    @Pattern(regexp = PASSWORD_REGEX, message = "Password must be at least 8 characters and include letters and digits.")
    private String password;

    @NotBlank
    private String confirmPassword;

    @Size(max = 100, message = "Display name must be at most 100 characters.")
    private String displayName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
