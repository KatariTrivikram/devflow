package com.devflow.controller;

import com.devflow.dto.response.ErrorResponse;
import com.devflow.dto.response.SearchResponse;
import com.devflow.entity.enums.SearchResourceType;
import com.devflow.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Global search endpoints across projects, issues, and sprints with database-level access scoping")
@SecurityRequirement(name = "bearerAuth")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(
            summary = "Global multi-resource search",
            description = "Performs case-insensitive global search across accessible Projects, Issues, and Sprints. Enforces database-level authorization scoping based on the authenticated user's role and project memberships."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search query, type, pagination, or sort parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SearchResponse> search(
            @Parameter(description = "Search keyword (required, 1-100 characters)", required = true)
            @RequestParam(name = "q") String query,

            @Parameter(description = "Resource type filter (ALL, PROJECT, ISSUE, SPRINT; default: ALL)")
            @RequestParam(required = false, defaultValue = "ALL") SearchResourceType type,

            @Parameter(description = "Zero-based page index (min: 0; default: 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (1-100; default: 10)")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort property and direction (allowed: updatedAt, createdAt, title, name; default: updatedAt,desc)")
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {

        SearchResponse response = searchService.search(query, type, page, size, sort);
        return ResponseEntity.ok(response);
    }
}
