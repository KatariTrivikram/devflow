package com.devflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for updating a comment")
public class CommentUpdateRequest {

    @NotBlank
    @Schema(description = "Updated text content of the comment", example = "Updated analysis: problem solved after restarting the cache layer.")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
