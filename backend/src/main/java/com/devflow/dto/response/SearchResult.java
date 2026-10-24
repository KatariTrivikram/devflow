package com.devflow.dto.response;

import com.devflow.entity.enums.SearchResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Compact search result representing a Project, Issue, or Sprint")
public class SearchResult {

    @Schema(description = "Resource UUID", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID id;

    @Schema(description = "Resource type (PROJECT, ISSUE, SPRINT)", example = "ISSUE")
    private SearchResourceType type;

    @Schema(description = "Resource title or name", example = "Fix authentication race condition")
    private String title;

    @Schema(description = "Resource description or summary", example = "JWT token expiry causes premature logout")
    private String description;

    @Schema(description = "Parent or associated project UUID", example = "c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID projectId;

    @Schema(description = "Associated project name", example = "DevFlow Platform")
    private String projectName;

    @Schema(description = "Resource status (e.g. ACTIVE, IN_PROGRESS, PLANNED)", example = "IN_PROGRESS")
    private String status;

    @Schema(description = "Issue priority if applicable (e.g. HIGH, CRITICAL)", example = "HIGH")
    private String priority;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;

    @Schema(description = "Reference UUID (identical to id or target entity)")
    private UUID referenceId;

    @Schema(description = "Sprint start date if resource is a SPRINT")
    private Instant startDate;

    @Schema(description = "Sprint end date if resource is a SPRINT")
    private Instant endDate;

    public SearchResult() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SearchResourceType getType() {
        return type;
    }

    public void setType(SearchResourceType type) {
        this.type = type;
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

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }
}
