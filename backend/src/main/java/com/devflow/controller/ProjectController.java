package com.devflow.controller;

import com.devflow.dto.request.ProjectCreateRequest;
import com.devflow.dto.request.ProjectUpdateRequest;
import com.devflow.dto.response.ErrorResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.dto.response.ProjectSummaryResponse;
import com.devflow.dto.response.SprintResponse;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.service.ProjectService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Project management, filtering, and sprint retrieval endpoints")
public class ProjectController {

    private final ProjectService projectService;
    private final SprintService sprintService;

    public ProjectController(ProjectService projectService, SprintService sprintService) {
        this.projectService = projectService;
        this.sprintService = sprintService;
    }

    @PostMapping
    @Operation(
            summary = "Create project",
            description = "Creates a new project. The authenticated user becomes the creator and is automatically assigned as PROJECT_MANAGER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project successfully created"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Duplicate project name or key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Get paginated projects",
            description = "Retrieves paginated projects scoped by user role with optional status filter, case-insensitive name/key search, and sorting. Supported sort fields: createdAt, updatedAt, name, status. Default: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of projects retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<ProjectResponse>> getProjects(
            @Parameter(description = "Filter by project status (ACTIVE, ARCHIVED, COMPLETED)")
            @RequestParam(required = false) ProjectStatus status,
            @Parameter(description = "Case-insensitive search query matching name or description")
            @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index (default: 0, min: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, min: 1, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (allowed: createdAt, updatedAt, name, status; default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(projectService.getProjects(status, search, page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get project by ID",
            description = "Retrieves project details by unique project identifier. Requires access to the project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project details found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks access to this project",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProjectResponse> getProjectById(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @GetMapping("/{id}/summary")
    @Operation(
            summary = "Get project summary metrics",
            description = "Retrieves compact overview metrics for a project including issue counts by status, member count, and active sprint progress."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project summary retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks access to this project",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProjectSummaryResponse> getProjectSummary(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProjectSummary(id));
    }

    @GetMapping("/{projectId}/sprints")
    @Operation(
            summary = "Get sprints by project (paginated)",
            description = "Retrieves sprints belonging to a project with optional status filter and sorting. Supported sort fields: startDate, endDate, name, status, createdAt, updatedAt. Default: startDate,asc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of sprints retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks access to this project",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<SprintResponse>> getSprintsByProject(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Filter by sprint status (PLANNED, ACTIVE, COMPLETED)")
            @RequestParam(required = false) SprintStatus status,
            @Parameter(description = "Zero-based page index (default: 0, min: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, min: 1, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (allowed: startDate, endDate, name, status, createdAt, updatedAt; default: startDate,asc)")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(sprintService.getSprintsByProjectPaginated(projectId, status, page, size, sort));
    }

    @GetMapping("/creator/{creatorId}")
    @Operation(
            summary = "Get projects by creator",
            description = "Retrieves all projects created by a specific user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of projects created by user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProjectResponse>> getProjectsByCreator(
            @Parameter(description = "Creator user UUID", required = true)
            @PathVariable UUID creatorId) {
        return ResponseEntity.ok(projectService.getProjectsByCreator(creatorId));
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get projects by status",
            description = "Retrieves all projects matching a specific status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of projects matching status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProjectResponse>> getProjectsByStatus(
            @Parameter(description = "Project status enum value", required = true)
            @PathVariable ProjectStatus status) {
        return ResponseEntity.ok(projectService.getProjectsByStatus(status));
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search projects by name",
            description = "Finds projects matching a substring in their name."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of matching projects"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProjectResponse>> searchProjectsByName(
            @Parameter(description = "Project name search string", required = true)
            @RequestParam String name) {
        return ResponseEntity.ok(projectService.searchProjectsByName(name));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update project",
            description = "Updates project details. Requires ADMIN role or project manager permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project successfully updated"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions to modify project",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProjectResponse> updateProject(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete project",
            description = "Deletes a project and its associated data. Requires ADMIN role or project creator permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Project successfully deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions to delete project",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
