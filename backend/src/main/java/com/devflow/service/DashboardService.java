package com.devflow.service;

import com.devflow.dto.response.ActivityResponse;
import com.devflow.dto.response.DashboardSummaryResponse;
import com.devflow.dto.response.DashboardSummaryResponse.AssignedIssueSummaryDto;
import com.devflow.dto.response.DashboardSummaryResponse.IssueSummaryDto;
import com.devflow.dto.response.DashboardSummaryResponse.ProjectSummaryDto;
import com.devflow.dto.response.DashboardSummaryResponse.SprintSummaryDto;
import com.devflow.entity.Activity;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;
    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final SecurityUtils securityUtils;

    public DashboardService(
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            SprintRepository sprintRepository,
            ActivityRepository activityRepository,
            ActivityMapper activityMapper,
            SecurityUtils securityUtils) {
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.sprintRepository = sprintRepository;
        this.activityRepository = activityRepository;
        this.activityMapper = activityMapper;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        UserRole currentUserRole = securityUtils.getCurrentUserRole();

        log.debug("Generating dashboard summary for user={}, role={}", currentUserId, currentUserRole);

        // 1. Resolve accessible project scope based on role
        List<UUID> accessibleProjectIds = null;

        if (currentUserRole == UserRole.ADMIN) {
            // ADMIN has platform-wide visibility across all projects
            accessibleProjectIds = null;
        } else if (currentUserRole == UserRole.PROJECT_MANAGER) {
            // PM is scoped to projects they manage/created or belong to
            accessibleProjectIds = projectRepository.findAccessibleProjectIdsForManager(currentUserId);
        } else {
            // DEVELOPER is scoped to projects they are members of
            accessibleProjectIds = projectRepository.findAccessibleProjectIdsForMember(currentUserId);
        }

        // If non-admin user has no accessible projects, return clean empty dashboard response immediately
        if (currentUserRole != UserRole.ADMIN && (accessibleProjectIds == null || accessibleProjectIds.isEmpty())) {
            log.debug("User {} has no accessible projects, returning zero-valued summary", currentUserId);
            return DashboardSummaryResponse.empty(currentUserRole);
        }

        // 2. Aggregate Project metrics
        ProjectSummaryDto projectSummary = aggregateProjectSummary(accessibleProjectIds);

        // 3. Aggregate Issue metrics (Status and Priority)
        IssueSummaryDto issueSummary = aggregateIssueSummary(accessibleProjectIds);

        // 4. Aggregate Assigned Issue metrics for the authenticated caller
        AssignedIssueSummaryDto assignedIssueSummary = aggregateAssignedIssueSummary(currentUserId, accessibleProjectIds);

        // 5. Aggregate Sprint metrics
        SprintSummaryDto sprintSummary = aggregateSprintSummary(accessibleProjectIds);

        // 6. Retrieve recent activity
        List<ActivityResponse> recentActivity = getRecentActivities(accessibleProjectIds);

        return new DashboardSummaryResponse(
                currentUserRole,
                projectSummary,
                issueSummary,
                assignedIssueSummary,
                sprintSummary,
                recentActivity
        );
    }

    private ProjectSummaryDto aggregateProjectSummary(Collection<UUID> projectIds) {
        List<Object[]> rows = (projectIds == null)
                ? projectRepository.countProjectsByStatus()
                : projectRepository.countProjectsByStatusIn(projectIds);

        Map<ProjectStatus, Long> counts = new EnumMap<>(ProjectStatus.class);
        for (Object[] row : rows) {
            if (row[0] instanceof ProjectStatus status && row[1] instanceof Number count) {
                counts.put(status, count.longValue());
            }
        }

        long active = counts.getOrDefault(ProjectStatus.ACTIVE, 0L);
        long completed = counts.getOrDefault(ProjectStatus.COMPLETED, 0L);
        long archived = counts.getOrDefault(ProjectStatus.ARCHIVED, 0L);
        long total = active + completed + archived;

        return new ProjectSummaryDto(total, active, completed, archived);
    }

    private IssueSummaryDto aggregateIssueSummary(Collection<UUID> projectIds) {
        // Status counts
        List<Object[]> statusRows = (projectIds == null)
                ? issueRepository.countIssuesByStatus()
                : issueRepository.countIssuesByStatusIn(projectIds);

        Map<IssueStatus, Long> statusCounts = new EnumMap<>(IssueStatus.class);
        for (Object[] row : statusRows) {
            if (row[0] instanceof IssueStatus status && row[1] instanceof Number count) {
                statusCounts.put(status, count.longValue());
            }
        }

        long todo = statusCounts.getOrDefault(IssueStatus.TODO, 0L);
        long inProgress = statusCounts.getOrDefault(IssueStatus.IN_PROGRESS, 0L);
        long codeReview = statusCounts.getOrDefault(IssueStatus.CODE_REVIEW, 0L);
        long done = statusCounts.getOrDefault(IssueStatus.DONE, 0L);
        long total = todo + inProgress + codeReview + done;

        // Priority counts
        List<Object[]> priorityRows = (projectIds == null)
                ? issueRepository.countIssuesByPriority()
                : issueRepository.countIssuesByPriorityIn(projectIds);

        Map<IssuePriority, Long> priorityCounts = new EnumMap<>(IssuePriority.class);
        for (Object[] row : priorityRows) {
            if (row[0] instanceof IssuePriority priority && row[1] instanceof Number count) {
                priorityCounts.put(priority, count.longValue());
            }
        }

        long low = priorityCounts.getOrDefault(IssuePriority.LOW, 0L);
        long medium = priorityCounts.getOrDefault(IssuePriority.MEDIUM, 0L);
        long high = priorityCounts.getOrDefault(IssuePriority.HIGH, 0L);
        long critical = priorityCounts.getOrDefault(IssuePriority.CRITICAL, 0L);

        return new IssueSummaryDto(total, todo, inProgress, codeReview, done, low, medium, high, critical);
    }

    private AssignedIssueSummaryDto aggregateAssignedIssueSummary(UUID userId, Collection<UUID> projectIds) {
        List<Object[]> rows = (projectIds == null)
                ? issueRepository.countAssignedIssuesByStatus(userId)
                : issueRepository.countAssignedIssuesByStatusIn(userId, projectIds);

        Map<IssueStatus, Long> counts = new EnumMap<>(IssueStatus.class);
        for (Object[] row : rows) {
            if (row[0] instanceof IssueStatus status && row[1] instanceof Number count) {
                counts.put(status, count.longValue());
            }
        }

        long todo = counts.getOrDefault(IssueStatus.TODO, 0L);
        long inProgress = counts.getOrDefault(IssueStatus.IN_PROGRESS, 0L);
        long codeReview = counts.getOrDefault(IssueStatus.CODE_REVIEW, 0L);
        long done = counts.getOrDefault(IssueStatus.DONE, 0L);
        long total = todo + inProgress + codeReview + done;

        return new AssignedIssueSummaryDto(total, todo, inProgress, codeReview, done);
    }

    private SprintSummaryDto aggregateSprintSummary(Collection<UUID> projectIds) {
        List<Object[]> rows = (projectIds == null)
                ? sprintRepository.countSprintsByStatus()
                : sprintRepository.countSprintsByStatusIn(projectIds);

        Map<SprintStatus, Long> counts = new EnumMap<>(SprintStatus.class);
        for (Object[] row : rows) {
            if (row[0] instanceof SprintStatus status && row[1] instanceof Number count) {
                counts.put(status, count.longValue());
            }
        }

        long planned = counts.getOrDefault(SprintStatus.PLANNED, 0L);
        long active = counts.getOrDefault(SprintStatus.ACTIVE, 0L);
        long completed = counts.getOrDefault(SprintStatus.COMPLETED, 0L);
        long total = planned + active + completed;

        return new SprintSummaryDto(total, planned, active, completed);
    }

    private List<ActivityResponse> getRecentActivities(Collection<UUID> projectIds) {
        Pageable limit = PageRequest.of(0, RECENT_ACTIVITY_LIMIT);
        List<Activity> activities = (projectIds == null)
                ? activityRepository.findRecentActivities(limit)
                : activityRepository.findRecentActivitiesIn(projectIds, limit);

        if (activities == null || activities.isEmpty()) {
            return Collections.emptyList();
        }

        return activities.stream()
                .map(activityMapper::toResponse)
                .toList();
    }
}
