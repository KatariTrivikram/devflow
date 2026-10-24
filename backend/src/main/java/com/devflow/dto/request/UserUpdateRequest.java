package com.devflow.dto.request;

import com.devflow.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating existing user details")
public class UserUpdateRequest {

    @Size(max = 100)
    @Schema(description = "Updated full name of the user", example = "Jane Smith")
    private String name;

    @Schema(description = "Updated platform role of the user", example = "PROJECT_MANAGER")
    private UserRole role;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
