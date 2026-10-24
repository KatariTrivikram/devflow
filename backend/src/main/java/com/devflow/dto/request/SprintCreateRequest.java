package com.devflow.dto.request;

import com.devflow.entity.enums.SprintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request body for creating a sprint")
public class SprintCreateRequest {

    @NotNull
    @Schema(description = "UUID of the project", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    private UUID projectId;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Sprint name", example = "Sprint 1")
    private String name;

    @NotNull
    @Schema(description = "Sprint start timestamp", example = "2026-10-15T00:00:00Z")
    private Instant startDate;

    @NotNull
    @Schema(description = "Sprint end timestamp", example = "2026-10-29T23:59:59Z")
    private Instant endDate;

    @NotNull
    @Schema(description = "Initial sprint status", example = "PLANNED")
    private SprintStatus status;

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
}
