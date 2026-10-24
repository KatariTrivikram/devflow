package com.devflow.dto.response;

import com.devflow.entity.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Project details and metadata")
public class ProjectResponse {

    @Schema(description = "Unique project identifier", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID id;

    @Schema(description = "Project name", example = "DevFlow Platform")
    private String name;

    @Schema(description = "Detailed description of the project", example = "Development and project management platform")
    private String description;

    @Schema(description = "Current lifecycle status of the project", example = "ACTIVE")
    private ProjectStatus status;

    @Schema(description = "User identifier of the project creator", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID createdById;

    @Schema(description = "Timestamp when the project was created", example = "2026-10-14T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Timestamp when the project was last updated", example = "2026-10-14T10:00:00Z")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
