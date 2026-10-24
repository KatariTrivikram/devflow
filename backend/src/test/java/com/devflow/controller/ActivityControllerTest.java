package com.devflow.controller;

import com.devflow.dto.request.ActivityCreateRequest;
import com.devflow.dto.response.ActivityResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.entity.enums.ActivityAction;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.service.ActivityService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ActivityController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActivityService activityService;

    @Test
    @DisplayName("POST /api/activities - successful create returns 201")
    void testCreateActivity_Success() throws Exception {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ActivityCreateRequest request = new ActivityCreateRequest(
                issueId, userId, ActivityAction.STATUS_CHANGED, "TODO", "IN_PROGRESS");

        ActivityResponse response = new ActivityResponse();
        UUID activityId = UUID.randomUUID();
        response.setId(activityId);
        response.setIssueId(issueId);
        response.setAction(ActivityAction.STATUS_CHANGED);

        when(activityService.createActivity(any(ActivityCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(activityId.toString()))
                .andExpect(jsonPath("$.action").value("STATUS_CHANGED"));
    }

    @Test
    @DisplayName("GET /api/activities/issue/{issueId} - successful retrieval returns 200")
    void testGetActivitiesByIssue_Success() throws Exception {
        UUID issueId = UUID.randomUUID();
        Page<ActivityResponse> page = new PageImpl<>(List.of(new ActivityResponse()));

        when(activityService.getActivitiesByIssue(eq(issueId), anyInt(), anyInt(), any()))
                .thenReturn(PageResponse.of(page));

        mockMvc.perform(get("/api/activities/issue/{issueId}", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/activities/issue/{issueId} - missing issue returns 404")
    void testGetActivitiesByIssue_MissingIssue() throws Exception {
        UUID issueId = UUID.randomUUID();
        when(activityService.getActivitiesByIssue(eq(issueId), anyInt(), anyInt(), any()))
                .thenThrow(new ResourceNotFoundException("Issue not found with id: " + issueId));

        mockMvc.perform(get("/api/activities/issue/{issueId}", issueId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
