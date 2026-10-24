package com.devflow.dto.request;

import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request body for updating an issue")
public class IssueUpdateRequest {

    @Schema(description = "Optional updated sprint UUID (null to unassign from sprint)", example = "d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a44")
    private UUID sprintId;

    @Schema(description = "Optional updated assignee UUID (null to unassign)", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID assignedToId;

    @Size(max = 200)
    @Schema(description = "Updated issue title", example = "Refactor authentication filter to handle edge cases")
    private String title;

    @Schema(description = "Updated detailed description", example = "Enhanced token parsing logic")
    private String description;

    @Schema(description = "Updated issue type", example = "IMPROVEMENT")
    private IssueType type;

    @Schema(description = "Updated priority level", example = "MEDIUM")
    private IssuePriority priority;

    @Schema(description = "Updated status of the issue", example = "IN_PROGRESS")
    private IssueStatus status;

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
