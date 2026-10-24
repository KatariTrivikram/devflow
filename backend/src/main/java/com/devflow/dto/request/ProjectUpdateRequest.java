package com.devflow.dto.request;

import com.devflow.entity.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating project details")
public class ProjectUpdateRequest {

    @Size(max = 100)
    @Schema(description = "Updated project name", example = "DevFlow Enterprise Platform")
    private String name;

    @Schema(description = "Updated project description", example = "Enhanced project tracking platform")
    private String description;

    @Schema(description = "Updated project status", example = "ACTIVE")
    private ProjectStatus status;

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
}
