package com.devflow.dto.request;

import com.devflow.entity.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request body for creating a new project")
public class ProjectCreateRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Project name", example = "DevFlow Platform")
    private String name;

    @Schema(description = "Project description", example = "Development and project management platform")
    private String description;

    @NotNull
    @Schema(description = "Initial project status", example = "ACTIVE")
    private ProjectStatus status;

    @Schema(description = "User ID of the project creator (optional; server derives creator from authenticated user)", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID createdById;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }
}
