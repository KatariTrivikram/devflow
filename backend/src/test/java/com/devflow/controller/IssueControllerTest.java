package com.devflow.controller;

import com.devflow.dto.request.IssueCreateRequest;
import com.devflow.dto.request.IssueUpdateRequest;
import com.devflow.dto.response.IssueResponse;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.dto.response.PageResponse;
import com.devflow.service.ActivityService;
import com.devflow.service.CommentService;
import com.devflow.service.IssueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IssueController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IssueService issueService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private ActivityService activityService;

    @Test
    @DisplayName("POST /api/issues - successful create returns 201")
    void testCreateIssue_Success() throws Exception {
        UUID projectId = UUID.randomUUID();
        IssueCreateRequest request = new IssueCreateRequest();
        request.setProjectId(projectId);
        request.setTitle("Login fix");
        request.setType(IssueType.BUG);
        request.setPriority(IssuePriority.HIGH);
        request.setStatus(IssueStatus.TODO);

        IssueResponse response = new IssueResponse();
        UUID issueId = UUID.randomUUID();
        response.setId(issueId);
        response.setTitle("Login fix");
        response.setProjectId(projectId);

        when(issueService.createIssue(any(IssueCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(issueId.toString()))
                .andExpect(jsonPath("$.title").value("Login fix"));
    }

    @Test
    @DisplayName("POST /api/issues - validation failure returns 400")
    void testCreateIssue_ValidationFailure() throws Exception {
        IssueCreateRequest request = new IssueCreateRequest();
        // missing required fields

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("GET /api/issues/{id} - returns 200")
    void testGetIssueById_Success() throws Exception {
        UUID issueId = UUID.randomUUID();
        IssueResponse response = new IssueResponse();
        response.setId(issueId);
        response.setTitle("Login fix");

        when(issueService.getIssueById(issueId)).thenReturn(response);

        mockMvc.perform(get("/api/issues/{id}", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(issueId.toString()));
    }

    @Test
    @DisplayName("GET /api/issues/{id} - not found returns 404")
    void testGetIssueById_NotFound() throws Exception {
        UUID issueId = UUID.randomUUID();
        when(issueService.getIssueById(issueId))
                .thenThrow(new ResourceNotFoundException("Issue not found with id: " + issueId));

        mockMvc.perform(get("/api/issues/{id}", issueId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/issues/project/{projectId} - returns 200 page")
    void testGetIssuesByProject_Success() throws Exception {
        UUID projectId = UUID.randomUUID();
        Page<IssueResponse> page = new PageImpl<>(List.of(new IssueResponse()));

        when(issueService.getIssuesByProject(eq(projectId), any(Pageable.class))).thenReturn(PageResponse.of(page));

        mockMvc.perform(get("/api/issues/project/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT /api/issues/{id} - returns 200")
    void testUpdateIssue_Success() throws Exception {
        UUID issueId = UUID.randomUUID();
        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setTitle("Updated Title");

        IssueResponse response = new IssueResponse();
        response.setId(issueId);
        response.setTitle("Updated Title");

        when(issueService.updateIssue(eq(issueId), any(IssueUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/issues/{id}", issueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @DisplayName("DELETE /api/issues/{id} - returns 204")
    void testDeleteIssue_Success() throws Exception {
        UUID issueId = UUID.randomUUID();
        doNothing().when(issueService).deleteIssue(issueId);

        mockMvc.perform(delete("/api/issues/{id}", issueId))
                .andExpect(status().isNoContent());
    }
}
