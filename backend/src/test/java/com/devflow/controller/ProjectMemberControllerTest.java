package com.devflow.controller;

import com.devflow.dto.request.ProjectMemberCreateRequest;
import com.devflow.dto.response.ProjectMemberResponse;
import com.devflow.exception.DuplicateResourceException;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.service.ProjectMemberService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProjectMemberController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProjectMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectMemberService projectMemberService;

    @Test
    @DisplayName("POST /api/project-members - successful add returns 201")
    void testAddMember_Success() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMemberCreateRequest request = new ProjectMemberCreateRequest();
        request.setProjectId(projectId);
        request.setUserId(userId);

        ProjectMemberResponse response = new ProjectMemberResponse();
        UUID memberId = UUID.randomUUID();
        response.setId(memberId);
        response.setProjectId(projectId);
        response.setUserId(userId);

        when(projectMemberService.addMember(any(ProjectMemberCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/project-members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("POST /api/project-members - duplicate membership returns 409")
    void testAddMember_Duplicate() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMemberCreateRequest request = new ProjectMemberCreateRequest();
        request.setProjectId(projectId);
        request.setUserId(userId);

        when(projectMemberService.addMember(any(ProjectMemberCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("User is already a member of this project"));

        mockMvc.perform(post("/api/project-members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/project-members/project/{projectId} - returns 200")
    void testGetProjectMembers_Success() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectMemberService.getProjectMembers(projectId)).thenReturn(List.of());

        mockMvc.perform(get("/api/project-members/project/{projectId}", projectId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/project-members/project/{projectId}/user/{userId} - returns 204")
    void testRemoveMember_Success() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doNothing().when(projectMemberService).removeMember(projectId, userId);

        mockMvc.perform(delete("/api/project-members/project/{projectId}/user/{userId}", projectId, userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/project-members/project/{projectId}/user/{userId} - not found returns 404")
    void testRemoveMember_NotFound() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Membership not found"))
                .when(projectMemberService).removeMember(projectId, userId);

        mockMvc.perform(delete("/api/project-members/project/{projectId}/user/{userId}", projectId, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
