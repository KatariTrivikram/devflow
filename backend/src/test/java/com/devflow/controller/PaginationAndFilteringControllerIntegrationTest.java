package com.devflow.controller;

import com.devflow.dto.response.ActivityResponse;
import com.devflow.dto.response.CommentResponse;
import com.devflow.dto.response.IssueResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.dto.response.SprintResponse;
import com.devflow.entity.enums.ActivityAction;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.security.CustomAccessDeniedHandler;
import com.devflow.security.CustomAuthenticationEntryPoint;
import com.devflow.security.CustomUserDetails;
import com.devflow.security.CustomUserDetailsService;
import com.devflow.security.JwtService;
import com.devflow.security.SecurityConfig;
import com.devflow.service.ActivityService;
import com.devflow.service.CommentService;
import com.devflow.service.IssueService;
import com.devflow.service.ProjectService;
import com.devflow.service.SprintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        IssueController.class,
        ProjectController.class,
        SprintController.class,
        CommentController.class,
        ActivityController.class
})
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        JwtService.class,
        GlobalExceptionHandler.class
})
class PaginationAndFilteringControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private IssueService issueService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private SprintService sprintService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private ActivityService activityService;

    private String devToken;
    private UUID devId;

    @BeforeEach
    void setUp() {
        devId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(devId, "developer@example.com", "password", UserRole.DEVELOPER);

        when(userDetailsService.loadUserByUsername("developer@example.com")).thenReturn(userDetails);
        devToken = jwtService.generateToken(userDetails);
    }

    @Test
    @DisplayName("GET /api/issues - valid filtering and pagination returns 200 PageResponse")
    void testGetIssues_ValidFiltering_Returns200() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();

        IssueResponse issueResp = new IssueResponse();
        issueResp.setId(UUID.randomUUID());
        issueResp.setTitle("Fix auth flow");
        issueResp.setStatus(IssueStatus.IN_PROGRESS);
        issueResp.setPriority(IssuePriority.HIGH);
        issueResp.setType(IssueType.BUG);

        PageResponse<IssueResponse> pageResponse = new PageResponse<>(
                List.of(issueResp), 0, 10, 1L, 1, true, true
        );

        when(issueService.getIssues(
                eq(projectId), eq(sprintId), isNull(), eq(assignedToId),
                eq(IssueStatus.IN_PROGRESS), eq(IssuePriority.HIGH), eq(IssueType.BUG),
                eq("auth"), eq(0), eq(10), eq("createdAt,desc")
        )).thenReturn(pageResponse);

        mockMvc.perform(get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("projectId", projectId.toString())
                        .param("sprintId", sprintId.toString())
                        .param("assignedToId", assignedToId.toString())
                        .param("status", "IN_PROGRESS")
                        .param("priority", "HIGH")
                        .param("type", "BUG")
                        .param("search", "auth")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Fix auth flow"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /api/issues - negative page returns 400 Bad Request")
    void testGetIssues_NegativePage_Returns400() throws Exception {
        when(issueService.getIssues(any(), any(), any(), any(), any(), any(), any(), any(), eq(-1), anyInt(), any()))
                .thenThrow(new BusinessRuleException("Page index must not be less than zero"));

        mockMvc.perform(get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
    }

    @Test
    @DisplayName("GET /api/issues - zero size returns 400 Bad Request")
    void testGetIssues_ZeroSize_Returns400() throws Exception {
        when(issueService.getIssues(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), eq(0), any()))
                .thenThrow(new BusinessRuleException("Page size must be between 1 and 100"));

        mockMvc.perform(get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"));
    }

    @Test
    @DisplayName("GET /api/issues - size above 100 returns 400 Bad Request")
    void testGetIssues_SizeAbove100_Returns400() throws Exception {
        when(issueService.getIssues(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), eq(101), any()))
                .thenThrow(new BusinessRuleException("Page size must be between 1 and 100"));

        mockMvc.perform(get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"));
    }

    @Test
    @DisplayName("GET /api/issues - invalid sort property returns 400 Bad Request")
    void testGetIssues_InvalidSortProperty_Returns400() throws Exception {
        when(issueService.getIssues(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), eq("password,desc")))
                .thenThrow(new BusinessRuleException("Invalid sort property: password. Allowed fields: [createdAt, updatedAt, title, priority, status, type]"));

        mockMvc.perform(get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("sort", "password,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid sort property: password. Allowed fields: [createdAt, updatedAt, title, priority, status, type]"));
    }

    @Test
    @DisplayName("GET /api/issues - invalid status enum returns 400 Bad Request")
    void testGetIssues_InvalidStatusEnum_Returns400() throws Exception {
        mockMvc.perform(get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/issues - invalid priority enum returns 400 Bad Request")
    void testGetIssues_InvalidPriorityEnum_Returns400() throws Exception {
        mockMvc.perform(get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("priority", "URGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/issues - invalid type enum returns 400 Bad Request")
    void testGetIssues_InvalidTypeEnum_Returns400() throws Exception {
        mockMvc.perform(get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("type", "RANDOM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/issues - unauthorized project access returns 403 Forbidden")
    void testGetIssues_UnauthorizedProject_Returns403() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(issueService.getIssues(eq(projectId), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenThrow(new AccessDeniedException("Access denied: You do not have access to this project"));

        mockMvc.perform(get("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("projectId", projectId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("GET /api/projects - paginated project search and filter returns 200 PageResponse")
    void testGetProjects_SearchAndFilter_Returns200() throws Exception {
        ProjectResponse projectResp = new ProjectResponse();
        projectResp.setId(UUID.randomUUID());
        projectResp.setName("DevFlow Core");
        projectResp.setStatus(ProjectStatus.ACTIVE);

        PageResponse<ProjectResponse> pageResponse = new PageResponse<>(
                List.of(projectResp), 0, 10, 1L, 1, true, true
        );

        when(projectService.getProjects(eq(ProjectStatus.ACTIVE), eq("Core"), eq(0), eq(10), eq("name,asc")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("status", "ACTIVE")
                        .param("search", "Core")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("DevFlow Core"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/projects/{projectId}/sprints - paginated sprint list returns 200 PageResponse")
    void testGetSprintsByProject_Returns200() throws Exception {
        UUID projectId = UUID.randomUUID();
        SprintResponse sprintResp = new SprintResponse();
        sprintResp.setId(UUID.randomUUID());
        sprintResp.setName("Sprint Alpha");
        sprintResp.setStatus(SprintStatus.ACTIVE);

        PageResponse<SprintResponse> pageResponse = new PageResponse<>(
                List.of(sprintResp), 0, 20, 1L, 1, true, true
        );

        when(sprintService.getSprintsByProjectPaginated(eq(projectId), eq(SprintStatus.ACTIVE), eq(0), eq(20), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/projects/{projectId}/sprints", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Sprint Alpha"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/projects/{projectId}/sprints - unauthorized project returns 403 Forbidden")
    void testGetSprintsByProject_Unauthorized_Returns403() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(sprintService.getSprintsByProjectPaginated(eq(projectId), any(), anyInt(), anyInt(), any()))
                .thenThrow(new AccessDeniedException("Access denied: You do not have access to this project"));

        mockMvc.perform(get("/api/projects/{projectId}/sprints", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("GET /api/issues/{issueId}/comments - paginated comments returns 200 PageResponse")
    void testGetCommentsByIssue_Returns200() throws Exception {
        UUID issueId = UUID.randomUUID();
        CommentResponse commentResp = new CommentResponse();
        commentResp.setId(UUID.randomUUID());
        commentResp.setContent("Good catch");

        PageResponse<CommentResponse> pageResponse = new PageResponse<>(
                List.of(commentResp), 0, 20, 1L, 1, true, true
        );

        when(commentService.getCommentsByIssue(eq(issueId), eq(0), eq(20), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/issues/{issueId}/comments", issueId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Good catch"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/issues/{issueId}/activities - paginated activities returns 200 PageResponse")
    void testGetActivitiesByIssue_Returns200() throws Exception {
        UUID issueId = UUID.randomUUID();
        ActivityResponse actResp = new ActivityResponse();
        actResp.setId(UUID.randomUUID());
        actResp.setAction(ActivityAction.STATUS_CHANGED);

        PageResponse<ActivityResponse> pageResponse = new PageResponse<>(
                List.of(actResp), 0, 20, 1L, 1, true, true
        );

        when(activityService.getActivitiesByIssue(eq(issueId), eq(0), eq(20), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/issues/{issueId}/activities", issueId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
