package com.devflow.dto.response;

import com.devflow.entity.enums.ActivityAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Activity audit trail record")
public class ActivityResponse {

    @Schema(description = "Unique activity identifier", example = "a1eebc99-9c0b-4ef8-bb6d-6bb9bd380a77")
    private UUID id;

    @Schema(description = "UUID of the associated issue", example = "e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a55")
    private UUID issueId;

    @Schema(description = "UUID of the user who performed the action", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID userId;

    @Schema(description = "Action performed on the issue", example = "STATUS_CHANGED")
    private ActivityAction action;

    @Schema(description = "Previous field value prior to change", example = "TODO")
    private String oldValue;

    @Schema(description = "New field value after change", example = "IN_PROGRESS")
    private String newValue;

    @Schema(description = "Timestamp when the activity occurred", example = "2026-10-14T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Title of the associated issue", example = "Resolve connection pool timeout")
    private String issueTitle;

    @Schema(description = "Display name of the user who performed the action", example = "Alice Developer")
    private String userName;

    @Schema(description = "Email of the user who performed the action", example = "alice@example.com")
    private String userEmail;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getIssueTitle() {
        return issueTitle;
    }

    public void setIssueTitle(String issueTitle) {
        this.issueTitle = issueTitle;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
