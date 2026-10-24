package com.devflow.dto.request;

import com.devflow.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for user registration or administrative user creation")
public class UserCreateRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Full name of the user", example = "Jane Doe")
    private String name;

    @NotBlank
    @Email
    @Size(max = 100)
    @Schema(description = "Unique email address used for login", example = "jane.doe@example.com")
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    @Schema(description = "User password (minimum 8 characters). Encrypted before persistence and never returned in API responses.", example = "SecurePass123!")
    private String password;

    @NotNull
    @Schema(description = "Assigned platform role", example = "DEVELOPER")
    private UserRole role;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
