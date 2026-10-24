package com.devflow.controller;

import com.devflow.dto.response.ActivityResponse;
import com.devflow.dto.response.DashboardSummaryResponse;
import com.devflow.entity.enums.ActivityAction;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.service.DashboardService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @Test
    @DisplayName("GET /api/dashboard/summary - returns 200 with dashboard summary metrics")
    void testGetDashboardSummary_Success() throws Exception {
        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setUserRole(UserRole.PROJECT_MANAGER);

        DashboardSummaryResponse.ProjectSummaryDto projectSummary = new DashboardSummaryResponse.ProjectSummaryDto();
        projectSummary.setTotal(5);
        projectSummary.setActive(3);
        projectSummary.setCompleted(1);
        projectSummary.setArchived(1);
        response.setProjectSummary(projectSummary);

        DashboardSummaryResponse.IssueSummaryDto issueSummary = new DashboardSummaryResponse.IssueSummaryDto();
        issueSummary.setTotal(10);
        issueSummary.setTodo(4);
        issueSummary.setInProgress(3);
        issueSummary.setCodeReview(1);
        issueSummary.setDone(2);
        issueSummary.setLowPriority(1);
        issueSummary.setMediumPriority(5);
        issueSummary.setHighPriority(3);
        issueSummary.setCriticalPriority(1);
        response.setIssueSummary(issueSummary);

        DashboardSummaryResponse.AssignedIssueSummaryDto assignedSummary = new DashboardSummaryResponse.AssignedIssueSummaryDto();
        assignedSummary.setTotal(3);
        assignedSummary.setTodo(1);
        assignedSummary.setInProgress(2);
        assignedSummary.setCodeReview(0);
        assignedSummary.setDone(0);
        response.setAssignedIssueSummary(assignedSummary);

        DashboardSummaryResponse.SprintSummaryDto sprintSummary = new DashboardSummaryResponse.SprintSummaryDto();
        sprintSummary.setTotal(4);
        sprintSummary.setActive(1);
        sprintSummary.setPlanned(2);
        sprintSummary.setCompleted(1);
        response.setSprintSummary(sprintSummary);

        ActivityResponse activity = new ActivityResponse();
        activity.setId(UUID.randomUUID());
        activity.setAction(ActivityAction.STATUS_CHANGED);
        activity.setIssueId(UUID.randomUUID());
        activity.setIssueTitle("Fix navbar layout");
        activity.setUserName("Alice Developer");
        activity.setCreatedAt(Instant.now());
        response.setRecentActivity(List.of(activity));

        when(dashboardService.getDashboardSummary()).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userRole").value("PROJECT_MANAGER"))
                .andExpect(jsonPath("$.projectSummary.total").value(5))
                .andExpect(jsonPath("$.projectSummary.active").value(3))
                .andExpect(jsonPath("$.issueSummary.total").value(10))
                .andExpect(jsonPath("$.issueSummary.todo").value(4))
                .andExpect(jsonPath("$.issueSummary.highPriority").value(3))
                .andExpect(jsonPath("$.assignedIssueSummary.total").value(3))
                .andExpect(jsonPath("$.sprintSummary.total").value(4))
                .andExpect(jsonPath("$.recentActivity[0].action").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$.recentActivity[0].issueTitle").value("Fix navbar layout"))
                .andExpect(jsonPath("$.recentActivity[0].userName").value("Alice Developer"));

        verify(dashboardService).getDashboardSummary();
    }

    @Test
    @DisplayName("GET /api/dashboard/summary - returns 200 with empty zero-valued summary")
    void testGetDashboardSummary_Empty() throws Exception {
        DashboardSummaryResponse empty = DashboardSummaryResponse.empty(UserRole.DEVELOPER);
        when(dashboardService.getDashboardSummary()).thenReturn(empty);

        mockMvc.perform(get("/api/dashboard/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userRole").value("DEVELOPER"))
                .andExpect(jsonPath("$.projectSummary.total").value(0))
                .andExpect(jsonPath("$.issueSummary.total").value(0))
                .andExpect(jsonPath("$.assignedIssueSummary.total").value(0))
                .andExpect(jsonPath("$.sprintSummary.total").value(0))
                .andExpect(jsonPath("$.recentActivity").isArray())
                .andExpect(jsonPath("$.recentActivity").isEmpty());

        verify(dashboardService).getDashboardSummary();
    }
}
