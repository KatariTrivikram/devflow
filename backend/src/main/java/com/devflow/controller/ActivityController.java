package com.devflow.controller;

import com.devflow.dto.request.ActivityCreateRequest;
import com.devflow.dto.response.ActivityResponse;
import com.devflow.dto.response.ErrorResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.service.ActivityService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Activities", description = "Activity audit trail, history, and change logging endpoints")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    @Operation(
            summary = "Record an activity event",
            description = "Logs an activity event for an issue. Caller must have access to the parent project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Activity event recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Issue not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ActivityResponse> createActivity(
            @Valid @RequestBody ActivityCreateRequest request) {
        ActivityResponse response = activityService.createActivity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/issue/{issueId}")
    @Operation(
            summary = "Get activity log for an issue (paginated)",
            description = "Retrieves paginated audit events for an issue. Supported sort fields: createdAt, action. Default sort: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of activities retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Issue not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<ActivityResponse>> getActivitiesByIssue(
            @Parameter(description = "Unique issue UUID", required = true)
            @PathVariable UUID issueId,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (allowed: createdAt, action; default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(activityService.getActivitiesByIssue(issueId, page, size, sort));
    }

    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "Get recent activities for a project",
            description = "Retrieves recent activity audit events across all issues in a project. Requires project access."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of project activities retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ActivityResponse>> getActivitiesByProject(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Number of recent activity entries to return (default: 10, max: 50)")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(activityService.getRecentActivitiesByProject(projectId, limit));
    }
}
