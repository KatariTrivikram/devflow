package com.devflow.controller;

import com.devflow.dto.request.SprintCreateRequest;
import com.devflow.dto.request.SprintUpdateRequest;
import com.devflow.dto.response.ErrorResponse;
import com.devflow.dto.response.SprintResponse;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.service.SprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sprints")
@Tag(name = "Sprints", description = "Sprint planning, milestone tracking, and lifecycle management endpoints")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @PostMapping
    @Operation(
            summary = "Create sprint",
            description = "Creates a new sprint within a project. Requires PROJECT_MANAGER or ADMIN permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sprint successfully created"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires PROJECT_MANAGER or ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SprintResponse> createSprint(@Valid @RequestBody SprintCreateRequest request) {
        SprintResponse response = sprintService.createSprint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get sprint by ID",
            description = "Retrieves sprint details by unique sprint identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sprint details found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sprint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SprintResponse> getSprintById(
            @Parameter(description = "Unique sprint UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(sprintService.getSprintById(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "Get all sprints for project",
            description = "Retrieves all sprints belonging to a project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of project sprints retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SprintResponse>> getSprintsByProject(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(sprintService.getSprintsByProject(projectId));
    }

    @GetMapping("/project/{projectId}/status/{status}")
    @Operation(
            summary = "Get sprints for project by status",
            description = "Retrieves sprints belonging to a project filtered by sprint status (PLANNED, ACTIVE, COMPLETED)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of filtered sprints retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SprintResponse>> getSprintsByProjectAndStatus(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Sprint status filter", required = true)
            @PathVariable SprintStatus status) {
        return ResponseEntity.ok(sprintService.getSprintsByProjectAndStatus(projectId, status));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update sprint",
            description = "Updates sprint dates, name, or status. Requires PROJECT_MANAGER or ADMIN permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sprint updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires PROJECT_MANAGER or ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sprint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SprintResponse> updateSprint(
            @Parameter(description = "Unique sprint UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody SprintUpdateRequest request) {
        return ResponseEntity.ok(sprintService.updateSprint(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete sprint",
            description = "Deletes a sprint. Requires PROJECT_MANAGER or ADMIN permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sprint deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires PROJECT_MANAGER or ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sprint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteSprint(
            @Parameter(description = "Unique sprint UUID", required = true)
            @PathVariable UUID id) {
        sprintService.deleteSprint(id);
        return ResponseEntity.noContent().build();
    }
}
