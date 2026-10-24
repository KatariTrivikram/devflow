package com.devflow.dto.request;

import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request body for creating a new issue")
public class IssueCreateRequest {

    @NotNull
    @Schema(description = "UUID of the project containing the issue", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID projectId;

    @Schema(description = "Optional UUID of the sprint to assign this issue to", example = "d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a44")
    private UUID sprintId;

    @Schema(description = "Optional UUID of the user assigned to this issue", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID assignedToId;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Issue title or concise summary", example = "Implement user authentication filter")
    private String title;

    @Schema(description = "Detailed issue description, acceptance criteria, or repro steps", example = "Add JwtAuthenticationFilter to validate incoming Bearer tokens")
    private String description;

    @NotNull
    @Schema(description = "Type of the issue (BUG, FEATURE, TASK, IMPROVEMENT)", example = "FEATURE")
    private IssueType type;

    @NotNull
    @Schema(description = "Priority level (LOW, MEDIUM, HIGH, CRITICAL)", example = "HIGH")
    private IssuePriority priority;

    @NotNull
    @Schema(description = "Initial status of the issue (TODO, IN_PROGRESS, CODE_REVIEW, DONE)", example = "TODO")
    private IssueStatus status;

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
}
