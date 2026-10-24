package com.devflow.controller;

import com.devflow.dto.request.ProjectMemberCreateRequest;
import com.devflow.dto.response.ErrorResponse;
import com.devflow.dto.response.ProjectMemberResponse;
import com.devflow.service.ProjectMemberService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/project-members")
@Tag(name = "Project Members", description = "Project membership and team role assignment endpoints")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @PostMapping
    @Operation(
            summary = "Add member to project",
            description = "Adds a user to a project team. Requires ADMIN or PROJECT_MANAGER role for the project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member added successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or PROJECT_MANAGER permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or user not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - User is already a member of this project",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProjectMemberResponse> addMember(
            @Valid @RequestBody ProjectMemberCreateRequest request) {
        ProjectMemberResponse response = projectMemberService.addMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "Get project members",
            description = "Lists all members assigned to a project. Requires project access (membership or ADMIN)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of project members retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProjectMemberResponse>> getProjectMembers(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(projectMemberService.getProjectMembers(projectId));
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get user memberships",
            description = "Lists all project memberships for a given user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of memberships for the user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProjectMemberResponse>> getUserMemberships(
            @Parameter(description = "User unique UUID", required = true)
            @PathVariable UUID userId) {
        return ResponseEntity.ok(projectMemberService.getUserMemberships(userId));
    }

    @GetMapping("/project/{projectId}/user/{userId}")
    @Operation(
            summary = "Get specific membership",
            description = "Retrieves membership details for a specific user within a project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membership details found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks project access",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Membership not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProjectMemberResponse> getMembership(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "User unique UUID", required = true)
            @PathVariable UUID userId) {
        return ResponseEntity.ok(projectMemberService.getMembership(projectId, userId));
    }

    @DeleteMapping("/project/{projectId}/user/{userId}")
    @Operation(
            summary = "Remove member from project",
            description = "Removes a user's membership from a project. Requires ADMIN or PROJECT_MANAGER permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member successfully removed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or PROJECT_MANAGER permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project, user, or membership not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "Unique project UUID", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "User unique UUID", required = true)
            @PathVariable UUID userId) {
        projectMemberService.removeMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }
}
