package com.devflow.security;

import com.devflow.controller.CommentController;
import com.devflow.controller.IssueController;
import com.devflow.controller.ProjectController;
import com.devflow.dto.request.IssueCreateRequest;
import com.devflow.dto.request.IssueUpdateRequest;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.service.ActivityService;
import com.devflow.service.CommentService;
import com.devflow.service.IssueService;
import com.devflow.service.ProjectService;
import com.devflow.service.SprintService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProjectController.class, IssueController.class, CommentController.class})
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        JwtService.class,
        GlobalExceptionHandler.class
})
class IdorSecurityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private IssueService issueService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private SprintService sprintService;

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
    @DisplayName("HTTP 401: Unauthenticated request to GET /api/projects/{id}")
    void testUnauthenticated_Returns401() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("HTTP 403: IDOR prevention on GET /api/projects/{id} returns 403 when access denied")
    void testIdor_GetProjectAccessDenied_Returns403() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.getProjectById(projectId))
                .thenThrow(new AccessDeniedException("Access denied: You do not have access to this project"));

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("HTTP 200: Authorized access to GET /api/projects/{id} returns 200")
    void testAuthorized_GetProject_Returns200() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectResponse response = new ProjectResponse();
        response.setId(projectId);
        response.setName("Accessible Project");

        when(projectService.getProjectById(projectId)).thenReturn(response);

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Accessible Project"));
    }

    @Test
    @DisplayName("HTTP 403: IDOR prevention on PUT /api/issues/{id} returns 403 when modifying unassigned issue")
    void testIdor_UpdateIssueAccessDenied_Returns403() throws Exception {
        UUID issueId = UUID.randomUUID();
        IssueUpdateRequest updateRequest = new IssueUpdateRequest();
        updateRequest.setTitle("Modified Title");

        when(issueService.updateIssue(eq(issueId), any(IssueUpdateRequest.class)))
                .thenThrow(new AccessDeniedException("Access denied: You are not authorized to update this issue"));

        mockMvc.perform(put("/api/issues/{id}", issueId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("HTTP 403: IDOR prevention on DELETE /api/comments/{id} returns 403 when deleting another user's comment")
    void testIdor_DeleteCommentAccessDenied_Returns403() throws Exception {
        UUID commentId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Access denied: You are not authorized to modify this comment"))
                .when(commentService).deleteComment(commentId);

        mockMvc.perform(delete("/api/comments/{id}", commentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("HTTP 400: Assignment validation rejection returns 400 with BusinessRuleException message")
    void testAssignmentValidation_NonMemberRejected_Returns400() throws Exception {
        UUID nonMemberId = UUID.randomUUID();
        IssueCreateRequest request = new IssueCreateRequest();
        request.setProjectId(UUID.randomUUID());
        request.setTitle("Task for outsider");
        request.setType(IssueType.TASK);
        request.setPriority(IssuePriority.MEDIUM);
        request.setStatus(IssueStatus.TODO);
        request.setAssignedToId(nonMemberId);

        when(issueService.createIssue(any(IssueCreateRequest.class)))
                .thenThrow(new BusinessRuleException("Assigned user does not belong to project"));

        mockMvc.perform(post("/api/issues")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + devToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Assigned user does not belong to project"));
    }
}
