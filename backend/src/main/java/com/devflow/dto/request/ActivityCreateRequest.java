package com.devflow.dto.request;

import com.devflow.entity.enums.ActivityAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request body for manually logging an issue activity event")
public class ActivityCreateRequest {

    @NotNull
    @Schema(description = "UUID of the associated issue", example = "e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a55")
    private UUID issueId;

    @NotNull
    @Schema(description = "UUID of the actor user performing the action", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID userId;

    @NotNull
    @Schema(description = "Action performed (e.g., STATUS_CHANGED, ASSIGNEE_CHANGED)", example = "STATUS_CHANGED")
    private ActivityAction action;

    @Schema(description = "Previous value before the change", example = "TODO")
    private String oldValue;

    @Schema(description = "New value after the change", example = "IN_PROGRESS")
    private String newValue;

    public ActivityCreateRequest() {
    }

    public ActivityCreateRequest(UUID issueId, UUID userId, ActivityAction action, String oldValue, String newValue) {
        this.issueId = issueId;
        this.userId = userId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public UUID getIssueId() {
        return issueId;
    }

    public void setIssueId(UUID issueId) {
        this.issueId = issueId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public ActivityAction getAction() {
        return action;
    }

    public void setAction(ActivityAction action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}
