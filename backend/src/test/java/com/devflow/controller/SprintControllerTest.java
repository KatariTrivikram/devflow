package com.devflow.controller;

import com.devflow.dto.request.SprintCreateRequest;
import com.devflow.dto.request.SprintUpdateRequest;
import com.devflow.dto.response.SprintResponse;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.exception.ResourceNotFoundException;
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

import java.time.Instant;
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

@WebMvcTest(controllers = SprintController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SprintService sprintService;

    @Test
    @DisplayName("POST /api/sprints - successful create returns 201")
    void testCreateSprint_Success() throws Exception {
        UUID projectId = UUID.randomUUID();
        SprintCreateRequest request = new SprintCreateRequest();
        request.setProjectId(projectId);
        request.setName("Sprint 1");
        Instant start = Instant.now();
        Instant end = start.plusSeconds(86400 * 14);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setStatus(SprintStatus.PLANNED);

        SprintResponse response = new SprintResponse();
        UUID sprintId = UUID.randomUUID();
        response.setId(sprintId);
        response.setName("Sprint 1");
        response.setProjectId(projectId);
        response.setStatus(SprintStatus.PLANNED);

        when(sprintService.createSprint(any(SprintCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sprintId.toString()))
                .andExpect(jsonPath("$.name").value("Sprint 1"));
    }

    @Test
    @DisplayName("POST /api/sprints - invalid request dates throws 400")
    void testCreateSprint_InvalidDates() throws Exception {
        UUID projectId = UUID.randomUUID();
        SprintCreateRequest request = new SprintCreateRequest();
        request.setProjectId(projectId);
        request.setName("Sprint 1");
        Instant start = Instant.now();
        Instant end = start.minusSeconds(86400);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setStatus(SprintStatus.PLANNED);

        when(sprintService.createSprint(any(SprintCreateRequest.class)))
                .thenThrow(new BusinessRuleException("Sprint end date cannot be before start date"));

        mockMvc.perform(post("/api/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Sprint end date cannot be before start date"));
    }

    @Test
    @DisplayName("GET /api/sprints/{id} - not found returns 404")
    void testGetSprintById_NotFound() throws Exception {
        UUID sprintId = UUID.randomUUID();
        when(sprintService.getSprintById(sprintId))
                .thenThrow(new ResourceNotFoundException("Sprint not found with id: " + sprintId));

        mockMvc.perform(get("/api/sprints/{id}", sprintId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/sprints/{id} - successful update returns 200")
    void testUpdateSprint_Success() throws Exception {
        UUID sprintId = UUID.randomUUID();
        SprintUpdateRequest request = new SprintUpdateRequest();
        request.setName("Sprint 1 Updated");

        SprintResponse response = new SprintResponse();
        response.setId(sprintId);
        response.setName("Sprint 1 Updated");

        when(sprintService.updateSprint(eq(sprintId), any(SprintUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/sprints/{id}", sprintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sprint 1 Updated"));
    }

    @Test
    @DisplayName("DELETE /api/sprints/{id} - returns 204")
    void testDeleteSprint_Success() throws Exception {
        UUID sprintId = UUID.randomUUID();
        doNothing().when(sprintService).deleteSprint(sprintId);

        mockMvc.perform(delete("/api/sprints/{id}", sprintId))
                .andExpect(status().isNoContent());
    }
}
