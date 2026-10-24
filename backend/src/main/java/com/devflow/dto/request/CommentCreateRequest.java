package com.devflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request body for adding a comment to an issue")
public class CommentCreateRequest {

    @NotNull
    @Schema(description = "UUID of the issue being commented on", example = "e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a55")
    private UUID issueId;

    @NotNull
    @Schema(description = "UUID of the comment author", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID userId;

    @NotBlank
    @Schema(description = "Text content of the comment", example = "I have tested the fix locally and verified unit test coverage.")
    private String content;

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
}
