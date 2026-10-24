package com.devflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request body for adding a user as a member of a project")
public class ProjectMemberCreateRequest {

    @NotNull
    @Schema(description = "UUID of the project", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID projectId;

    @NotNull
    @Schema(description = "UUID of the user to add as a member", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID userId;

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
