package com.devflow.dto.response;

import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Issue item details and status")
public class IssueResponse {

    @Schema(description = "Unique issue identifier", example = "e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a55")
    private UUID id;

    @Schema(description = "UUID of the project containing the issue", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID projectId;

    @Schema(description = "UUID of the assigned sprint (nullable)", example = "d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a44")
    private UUID sprintId;

    @Schema(description = "UUID of the user assigned to this issue (nullable)", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID assignedToId;

    @Schema(description = "Title of the issue", example = "Implement user authentication filter")
    private String title;

    @Schema(description = "Detailed issue description", example = "Add JwtAuthenticationFilter to validate incoming Bearer tokens")
    private String description;

    @Schema(description = "Issue type", example = "FEATURE")
    private IssueType type;

    @Schema(description = "Priority level", example = "HIGH")
    private IssuePriority priority;

    @Schema(description = "Current lifecycle status", example = "IN_PROGRESS")
    private IssueStatus status;

    @Schema(description = "Timestamp when issue was created", example = "2026-10-14T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Timestamp when issue was last updated", example = "2026-10-14T10:00:00Z")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getSprintId() {
        return sprintId;
    }

    public void setSprintId(UUID sprintId) {
        this.sprintId = sprintId;
    }

    public UUID getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(UUID assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        this.type = type;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public void setPriority(IssuePriority priority) {
        this.priority = priority;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
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
