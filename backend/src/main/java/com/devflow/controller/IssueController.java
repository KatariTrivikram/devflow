package com.devflow.controller;

import com.devflow.dto.request.IssueCreateRequest;
import com.devflow.dto.request.IssueUpdateRequest;
import com.devflow.dto.response.ActivityResponse;
import com.devflow.dto.response.CommentResponse;
import com.devflow.dto.response.ErrorResponse;
import com.devflow.dto.response.IssueResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.service.ActivityService;
import com.devflow.service.CommentService;
import com.devflow.service.IssueService;
import com.devflow.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
@Tag(name = "Issues", description = "Issue tracking, lifecycle workflows, search, filtering, comments, and activity audit trail")
public class IssueController {

    private final IssueService issueService;
    private final CommentService commentService;
    private final ActivityService activityService;

    public IssueController(IssueService issueService,
                           CommentService commentService,
                           ActivityService activityService) {
        this.issueService = issueService;
        this.commentService = commentService;
        this.activityService = activityService;
    }

    @PostMapping
    @Operation(
            summary = "Create issue",
            description = "Creates a new issue in a project. Caller must be a member of the project or an ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Issue created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project membership",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project, sprint, or assignee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<IssueResponse> createIssue(@Valid @RequestBody IssueCreateRequest request) {
        IssueResponse response = issueService.createIssue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Get paginated issues with dynamic filters",
            description = "Retrieves issues using multi-criteria filtering, case-insensitive substring search (title/description), and safe sorting. Scoped to projects accessible to the authenticated user. Allowed sort fields: createdAt, updatedAt, title, priority, status, type. Default sort: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of issues retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, pagination, or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks access to the requested project",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<IssueResponse>> getIssues(
            @Parameter(description = "Filter by project UUID")
            @RequestParam(required = false) UUID projectId,
            @Parameter(description = "Filter by sprint UUID")
            @RequestParam(required = false) UUID sprintId,
            @Parameter(description = "Filter issues with no sprint (backlog)")
            @RequestParam(required = false) Boolean noSprint,
            @Parameter(description = "Filter by assignee user UUID")
            @RequestParam(required = false) UUID assignedToId,
            @Parameter(description = "Filter by issue status (TODO, IN_PROGRESS, CODE_REVIEW, DONE)")
            @RequestParam(required = false) IssueStatus status,
            @Parameter(description = "Filter by issue priority (LOW, MEDIUM, HIGH, CRITICAL)")
            @RequestParam(required = false) IssuePriority priority,
            @Parameter(description = "Filter by issue type (BUG, FEATURE, TASK, IMPROVEMENT)")
            @RequestParam(required = false) IssueType type,
            @Parameter(description = "Case-insensitive substring search matching title or description")
            @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index (default: 0, min: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, min: 1, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (allowed: createdAt, updatedAt, title, priority, status, type; default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(issueService.getIssues(
                projectId, sprintId, noSprint, assignedToId, status, priority, type, search, page, size, sort
        ));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get issue by ID",
            description = "Retrieves issue details by unique issue identifier. Requires project access."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issue found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Issue not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<IssueResponse> getIssueById(
            @Parameter(description = "Unique issue UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(issueService.getIssueById(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "Get issues by project (paginated)",
            description = "Retrieves issues belonging to a project. Supported sort fields: createdAt, updatedAt, title, priority, status, type. Default: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of issues retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<IssueResponse>> getIssuesByProject(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.ISSUE_SORT_FIELDS, "createdAt,desc"
        );
        return ResponseEntity.ok(issueService.getIssuesByProject(projectId, pageable));
    }

    @GetMapping("/sprint/{sprintId}")
    @Operation(
            summary = "Get issues by sprint (paginated)",
            description = "Retrieves issues assigned to a sprint. Supported sort fields: createdAt, updatedAt, title, priority, status, type. Default: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of issues retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<IssueResponse>> getIssuesBySprint(
            @Parameter(description = "Unique sprint UUID", required = true)
            @PathVariable UUID sprintId,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.ISSUE_SORT_FIELDS, "createdAt,desc"
        );
        return ResponseEntity.ok(issueService.getIssuesBySprint(sprintId, pageable));
    }

    @GetMapping("/assignee/{assigneeId}")
    @Operation(
            summary = "Get issues by assignee (paginated)",
            description = "Retrieves issues assigned to a specific user across accessible projects. Default sort: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of issues retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<IssueResponse>> getIssuesByAssignee(
            @Parameter(description = "Assignee user UUID", required = true)
            @PathVariable UUID assigneeId,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.ISSUE_SORT_FIELDS, "createdAt,desc"
        );
        return ResponseEntity.ok(issueService.getIssuesByAssignee(assigneeId, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get issues by status (paginated)",
            description = "Retrieves issues filtered by status across accessible projects. Default sort: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of issues retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<IssueResponse>> getIssuesByStatus(
            @Parameter(description = "Issue status enum (TODO, IN_PROGRESS, CODE_REVIEW, DONE)", required = true)
            @PathVariable IssueStatus status,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.ISSUE_SORT_FIELDS, "createdAt,desc"
        );
        return ResponseEntity.ok(issueService.getIssuesByStatus(status, pageable));
    }

    @GetMapping("/priority/{priority}")
    @Operation(
            summary = "Get issues by priority (paginated)",
            description = "Retrieves issues filtered by priority across accessible projects. Default sort: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of issues retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<IssueResponse>> getIssuesByPriority(
            @Parameter(description = "Issue priority enum (LOW, MEDIUM, HIGH, CRITICAL)", required = true)
            @PathVariable IssuePriority priority,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.ISSUE_SORT_FIELDS, "createdAt,desc"
        );
        return ResponseEntity.ok(issueService.getIssuesByPriority(priority, pageable));
    }

    @GetMapping("/project/{projectId}/status/{status}")
    @Operation(
            summary = "Get issues by project and status (paginated)",
            description = "Retrieves issues belonging to a project with specific status. Default sort: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of issues retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<IssueResponse>> getIssuesByProjectAndStatus(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Issue status enum", required = true)
            @PathVariable IssueStatus status,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.ISSUE_SORT_FIELDS, "createdAt,desc"
        );
        return ResponseEntity.ok(issueService.getIssuesByProjectAndStatus(projectId, status, pageable));
    }

    @GetMapping("/project/{projectId}/assignee/{assigneeId}")
    @Operation(
            summary = "Get issues by project and assignee (paginated)",
            description = "Retrieves issues belonging to a project assigned to a specific user. Default sort: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of issues retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<IssueResponse>> getIssuesByProjectAndAssignee(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Assignee user UUID", required = true)
            @PathVariable UUID assigneeId,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (default: createdAt,desc)")
            @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.ISSUE_SORT_FIELDS, "createdAt,desc"
        );
        return ResponseEntity.ok(issueService.getIssuesByProjectAndAssignee(projectId, assigneeId, pageable));
    }

    @GetMapping("/{issueId}/comments")
    @Operation(
            summary = "Get comments for an issue (paginated)",
            description = "Retrieves paginated comments for an issue. Supported sort fields: createdAt, updatedAt. Default sort: createdAt,asc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of comments retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Issue not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<CommentResponse>> getCommentsByIssue(
            @Parameter(description = "Unique issue UUID", required = true)
            @PathVariable UUID issueId,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction format: field,asc|desc (allowed: createdAt, updatedAt; default: createdAt,asc)")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(commentService.getCommentsByIssue(issueId, page, size, sort));
    }

    @GetMapping("/{issueId}/activities")
    @Operation(
            summary = "Get activity history for an issue (paginated)",
            description = "Retrieves paginated audit trail of changes made to an issue. Supported sort fields: createdAt, action. Default sort: createdAt,desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of activity events retrieved successfully"),
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

    @PutMapping("/{id}")
    @Operation(
            summary = "Update issue",
            description = "Updates issue details (status, priority, assignee, sprint, title, description). Caller must have project access."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issue updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Issue, sprint, or assignee not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<IssueResponse> updateIssue(
            @Parameter(description = "Unique issue UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody IssueUpdateRequest request) {
        return ResponseEntity.ok(issueService.updateIssue(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete issue",
            description = "Deletes an issue and its associated comments and activity entries. Requires ADMIN or PROJECT_MANAGER permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Issue deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or PROJECT_MANAGER permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Issue not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteIssue(
            @Parameter(description = "Unique issue UUID", required = true)
            @PathVariable UUID id) {
        issueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }
}
