package com.devflow.service;

import com.devflow.dto.response.ActivityResponse;
import com.devflow.dto.response.DashboardSummaryResponse;
import com.devflow.entity.Activity;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.User;
import com.devflow.entity.enums.ActivityAction;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import com.devflow.mapper.ActivityMapper;
import com.devflow.repository.ActivityRepository;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DashboardService dashboardService;

    private UUID userId;
    private UUID projectId1;
    private UUID projectId2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId1 = UUID.randomUUID();
        projectId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("ADMIN role receives platform-wide aggregated metrics across all projects")
    void testAdminDashboard() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);

        // Project aggregates
        when(projectRepository.countProjectsByStatus()).thenReturn(List.of(
                new Object[]{ProjectStatus.ACTIVE, 3L},
                new Object[]{ProjectStatus.COMPLETED, 1L},
                new Object[]{ProjectStatus.ARCHIVED, 1L}
        ));

        // Issue aggregates
        when(issueRepository.countIssuesByStatus()).thenReturn(List.of(
                new Object[]{IssueStatus.TODO, 5L},
                new Object[]{IssueStatus.IN_PROGRESS, 3L},
                new Object[]{IssueStatus.CODE_REVIEW, 2L},
                new Object[]{IssueStatus.DONE, 4L}
        ));
        when(issueRepository.countIssuesByPriority()).thenReturn(List.of(
                new Object[]{IssuePriority.LOW, 2L},
                new Object[]{IssuePriority.MEDIUM, 4L},
                new Object[]{IssuePriority.HIGH, 6L},
                new Object[]{IssuePriority.CRITICAL, 2L}
        ));

        // Assigned issues for caller
        when(issueRepository.countAssignedIssuesByStatus(userId)).thenReturn(List.of(
                new Object[]{IssueStatus.TODO, 1L},
                new Object[]{IssueStatus.IN_PROGRESS, 2L}
        ));

        // Sprints
        when(sprintRepository.countSprintsByStatus()).thenReturn(List.of(
                new Object[]{SprintStatus.PLANNED, 1L},
                new Object[]{SprintStatus.ACTIVE, 2L},
                new Object[]{SprintStatus.COMPLETED, 3L}
        ));

        // Activity
        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        ActivityResponse actResponse = new ActivityResponse();
        actResponse.setId(activity.getId());
        actResponse.setAction(ActivityAction.STATUS_CHANGED);

        when(activityRepository.findRecentActivities(any(Pageable.class))).thenReturn(List.of(activity));
        when(activityMapper.toResponse(activity)).thenReturn(actResponse);

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(UserRole.ADMIN, response.getUserRole());

        // Project verification
        assertEquals(5, response.getProjectSummary().getTotal());
        assertEquals(3, response.getProjectSummary().getActive());
        assertEquals(1, response.getProjectSummary().getCompleted());
        assertEquals(1, response.getProjectSummary().getArchived());

        // Issue status verification
        assertEquals(14, response.getIssueSummary().getTotal());
        assertEquals(5, response.getIssueSummary().getTodo());
        assertEquals(3, response.getIssueSummary().getInProgress());
        assertEquals(2, response.getIssueSummary().getCodeReview());
        assertEquals(4, response.getIssueSummary().getDone());

        // Issue priority verification
        assertEquals(2, response.getIssueSummary().getLowPriority());
        assertEquals(4, response.getIssueSummary().getMediumPriority());
        assertEquals(6, response.getIssueSummary().getHighPriority());
        assertEquals(2, response.getIssueSummary().getCriticalPriority());

        // Assigned issues verification
        assertEquals(3, response.getAssignedIssueSummary().getTotal());
        assertEquals(1, response.getAssignedIssueSummary().getTodo());
        assertEquals(2, response.getAssignedIssueSummary().getInProgress());
        assertEquals(0, response.getAssignedIssueSummary().getCodeReview());
        assertEquals(0, response.getAssignedIssueSummary().getDone());

        // Sprint verification
        assertEquals(6, response.getSprintSummary().getTotal());
        assertEquals(1, response.getSprintSummary().getPlanned());
        assertEquals(2, response.getSprintSummary().getActive());
        assertEquals(3, response.getSprintSummary().getCompleted());

        // Activity verification
        assertEquals(1, response.getRecentActivity().size());
        assertEquals(ActivityAction.STATUS_CHANGED, response.getRecentActivity().get(0).getAction());
    }

    @Test
    @DisplayName("PROJECT_MANAGER role receives metrics strictly scoped to their managed projects")
    void testProjectManagerDashboard() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);

        List<UUID> pmProjects = List.of(projectId1, projectId2);
        when(projectRepository.findAccessibleProjectIdsForManager(userId)).thenReturn(pmProjects);

        when(projectRepository.countProjectsByStatusIn(pmProjects)).thenReturn(List.<Object[]>of(
                new Object[]{ProjectStatus.ACTIVE, 2L}
        ));
        when(issueRepository.countIssuesByStatusIn(pmProjects)).thenReturn(List.<Object[]>of(
                new Object[]{IssueStatus.TODO, 3L},
                new Object[]{IssueStatus.IN_PROGRESS, 1L}
        ));
        when(issueRepository.countIssuesByPriorityIn(pmProjects)).thenReturn(List.<Object[]>of(
                new Object[]{IssuePriority.HIGH, 4L}
        ));
        when(issueRepository.countAssignedIssuesByStatusIn(userId, pmProjects)).thenReturn(List.<Object[]>of(
                new Object[]{IssueStatus.IN_PROGRESS, 1L}
        ));
        when(sprintRepository.countSprintsByStatusIn(pmProjects)).thenReturn(List.<Object[]>of(
                new Object[]{SprintStatus.ACTIVE, 1L}
        ));
        when(activityRepository.findRecentActivitiesIn(eq(pmProjects), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(UserRole.PROJECT_MANAGER, response.getUserRole());
        assertEquals(2, response.getProjectSummary().getTotal());
        assertEquals(2, response.getProjectSummary().getActive());
        assertEquals(4, response.getIssueSummary().getTotal());
        assertEquals(3, response.getIssueSummary().getTodo());
        assertEquals(1, response.getIssueSummary().getInProgress());
        assertEquals(4, response.getIssueSummary().getHighPriority());
        assertEquals(1, response.getAssignedIssueSummary().getTotal());
        assertEquals(1, response.getSprintSummary().getTotal());
        assertTrue(response.getRecentActivity().isEmpty());

        verify(projectRepository, never()).countProjectsByStatus();
        verify(issueRepository, never()).countIssuesByStatus();
    }

    @Test
    @DisplayName("DEVELOPER role receives metrics strictly scoped to their member projects")
    void testDeveloperDashboard() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);

        List<UUID> devProjects = List.of(projectId1);
        when(projectRepository.findAccessibleProjectIdsForMember(userId)).thenReturn(devProjects);

        when(projectRepository.countProjectsByStatusIn(devProjects)).thenReturn(List.<Object[]>of(
                new Object[]{ProjectStatus.ACTIVE, 1L}
        ));
        when(issueRepository.countIssuesByStatusIn(devProjects)).thenReturn(List.<Object[]>of(
                new Object[]{IssueStatus.IN_PROGRESS, 2L},
                new Object[]{IssueStatus.DONE, 2L}
        ));
        when(issueRepository.countIssuesByPriorityIn(devProjects)).thenReturn(List.<Object[]>of(
                new Object[]{IssuePriority.MEDIUM, 4L}
        ));
        when(issueRepository.countAssignedIssuesByStatusIn(userId, devProjects)).thenReturn(List.<Object[]>of(
                new Object[]{IssueStatus.IN_PROGRESS, 2L}
        ));
        when(sprintRepository.countSprintsByStatusIn(devProjects)).thenReturn(List.<Object[]>of(
                new Object[]{SprintStatus.ACTIVE, 1L}
        ));
        when(activityRepository.findRecentActivitiesIn(eq(devProjects), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(UserRole.DEVELOPER, response.getUserRole());
        assertEquals(1, response.getProjectSummary().getTotal());
        assertEquals(4, response.getIssueSummary().getTotal());
        assertEquals(2, response.getAssignedIssueSummary().getTotal());
        assertEquals(2, response.getAssignedIssueSummary().getInProgress());
    }

    @Test
    @DisplayName("User with zero accessible projects receives clean zero-valued summary without error")
    void testEmptyStateWhenNoAccessibleProjects() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);
        when(projectRepository.findAccessibleProjectIdsForMember(userId)).thenReturn(Collections.emptyList());

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(UserRole.DEVELOPER, response.getUserRole());
        assertEquals(0, response.getProjectSummary().getTotal());
        assertEquals(0, response.getIssueSummary().getTotal());
        assertEquals(0, response.getAssignedIssueSummary().getTotal());
        assertEquals(0, response.getSprintSummary().getTotal());
        assertTrue(response.getRecentActivity().isEmpty());

        // Verify no queries executed on empty scope
        verify(projectRepository, never()).countProjectsByStatus();
        verify(projectRepository, never()).countProjectsByStatusIn(any());
        verify(issueRepository, never()).countIssuesByStatus();
        verify(sprintRepository, never()).countSprintsByStatus();
    }
}
