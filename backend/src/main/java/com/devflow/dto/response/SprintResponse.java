package com.devflow.dto.response;

import com.devflow.entity.enums.SprintStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Sprint details and metadata")
public class SprintResponse {

    @Schema(description = "Unique sprint identifier", example = "d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a44")
    private UUID id;

    @Schema(description = "UUID of the owning project", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID projectId;

    @Schema(description = "Sprint name", example = "Sprint 1")
    private String name;

    @Schema(description = "Sprint start timestamp", example = "2026-10-15T00:00:00Z")
    private Instant startDate;

    @Schema(description = "Sprint end timestamp", example = "2026-10-29T23:59:59Z")
    private Instant endDate;

    @Schema(description = "Current lifecycle status of the sprint", example = "PLANNED")
    private SprintStatus status;

    @Schema(description = "Timestamp when sprint was created", example = "2026-10-14T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Timestamp when sprint was last updated", example = "2026-10-14T10:00:00Z")
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public SprintStatus getStatus() {
        return status;
    }

    public void setStatus(SprintStatus status) {
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
