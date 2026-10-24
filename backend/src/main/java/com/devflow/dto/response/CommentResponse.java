package com.devflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Issue comment details")
public class CommentResponse {

    @Schema(description = "Unique comment identifier", example = "f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a66")
    private UUID id;

    @Schema(description = "UUID of the commented issue", example = "e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a55")
    private UUID issueId;

    @Schema(description = "UUID of the user who authored the comment", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID userId;

    @Schema(description = "Text content of the comment", example = "I have tested the fix locally and verified unit test coverage.")
    private String content;

    @Schema(description = "Timestamp when the comment was posted", example = "2026-10-14T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Timestamp when the comment was last updated", example = "2026-10-14T10:00:00Z")
    private Instant updatedAt;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
