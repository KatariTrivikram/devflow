package com.devflow.dto.request;

import com.devflow.entity.enums.SprintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request body for updating sprint details")
public class SprintUpdateRequest {

    @Size(max = 100)
    @Schema(description = "Updated sprint name", example = "Sprint 1 - Hardening")
    private String name;

    @Schema(description = "Updated sprint start timestamp", example = "2026-10-15T00:00:00Z")
    private Instant startDate;

    @Schema(description = "Updated sprint end timestamp", example = "2026-10-31T23:59:59Z")
    private Instant endDate;

    @Schema(description = "Updated sprint status", example = "ACTIVE")
    private SprintStatus status;

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
