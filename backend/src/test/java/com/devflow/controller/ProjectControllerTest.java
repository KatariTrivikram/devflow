package com.devflow.controller;

import com.devflow.dto.request.ProjectCreateRequest;
import com.devflow.dto.request.ProjectUpdateRequest;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.service.ProjectService;
import com.devflow.service.SprintService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProjectController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private SprintService sprintService;

    @Test
    @DisplayName("POST /api/projects - successful create returns 201")
    void testCreateProject_Success() throws Exception {
        UUID creatorId = UUID.randomUUID();
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("DevFlow Core");
        request.setDescription("Management platform");
        request.setStatus(ProjectStatus.ACTIVE);
        request.setCreatedById(creatorId);

        ProjectResponse response = new ProjectResponse();
        UUID projectId = UUID.randomUUID();
        response.setId(projectId);
        response.setName("DevFlow Core");
        response.setStatus(ProjectStatus.ACTIVE);
        response.setCreatedById(creatorId);

        when(projectService.createProject(any(ProjectCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("DevFlow Core"));
    }

    @Test
    @DisplayName("GET /api/projects/{id} - successful get returns 200")
    void testGetProjectById_Success() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectResponse response = new ProjectResponse();
        response.setId(projectId);
        response.setName("DevFlow Core");

        when(projectService.getProjectById(projectId)).thenReturn(response);

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("DevFlow Core"));
    }

    @Test
    @DisplayName("GET /api/projects/{id} - not found returns 404")
    void testGetProjectById_NotFound() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.getProjectById(projectId))
                .thenThrow(new ResourceNotFoundException("Project not found with id: " + projectId));

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/projects/creator/{creatorId} - returns 200")
    void testGetProjectsByCreator_Success() throws Exception {
        UUID creatorId = UUID.randomUUID();
        when(projectService.getProjectsByCreator(creatorId)).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/creator/{creatorId}", creatorId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/projects/search - returns 200")
    void testSearchProjectsByName_Success() throws Exception {
        when(projectService.searchProjectsByName("DevFlow")).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/search").param("name", "DevFlow"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/projects/{id} - successful update returns 200")
    void testUpdateProject_Success() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("DevFlow 2.0");

        ProjectResponse response = new ProjectResponse();
        response.setId(projectId);
        response.setName("DevFlow 2.0");

        when(projectService.updateProject(eq(projectId), any(ProjectUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("DevFlow 2.0"));
    }

    @Test
    @DisplayName("DELETE /api/projects/{id} - successful delete returns 204")
    void testDeleteProject_Success() throws Exception {
        UUID projectId = UUID.randomUUID();
        doNothing().when(projectService).deleteProject(projectId);

        mockMvc.perform(delete("/api/projects/{id}", projectId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/projects/{id} - not found returns 404")
    void testDeleteProject_NotFound() throws Exception {
        UUID projectId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Project not found with id: " + projectId))
                .when(projectService).deleteProject(projectId);

        mockMvc.perform(delete("/api/projects/{id}", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
