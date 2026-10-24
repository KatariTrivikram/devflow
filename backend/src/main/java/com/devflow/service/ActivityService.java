package com.devflow.service;

import com.devflow.dto.request.ActivityCreateRequest;
import com.devflow.dto.response.ActivityResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.entity.Activity;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.User;
import com.devflow.entity.enums.ActivityAction;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.ActivityMapper;
import com.devflow.repository.ActivityRepository;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.AuthorizationService;
import com.devflow.security.SecurityUtils;
import com.devflow.util.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ActivityMapper activityMapper;
    private final AuthorizationService authorizationService;
    private final SecurityUtils securityUtils;
    private final ProjectRepository projectRepository;

    public ActivityService(ActivityRepository activityRepository,
                           IssueRepository issueRepository,
                           UserRepository userRepository,
                           ActivityMapper activityMapper,
                           AuthorizationService authorizationService,
                           SecurityUtils securityUtils) {
        this(activityRepository, issueRepository, userRepository, activityMapper, authorizationService, securityUtils, null);
    }

    @Autowired
    public ActivityService(ActivityRepository activityRepository,
                           IssueRepository issueRepository,
                           UserRepository userRepository,
                           ActivityMapper activityMapper,
                           AuthorizationService authorizationService,
                           SecurityUtils securityUtils,
                           ProjectRepository projectRepository) {
        this.activityRepository = activityRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.activityMapper = activityMapper;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ActivityResponse createActivity(ActivityCreateRequest request) {
        Issue issue = issueRepository.findById(request.getIssueId())
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + request.getIssueId()));

        authorizationService.requireProjectAccess(issue.getProject());

        // Always resolve actor from SecurityContext when authenticated, preventing impersonation in audit logs
        UUID actorId = securityUtils.isAuthenticated()
                ? securityUtils.getCurrentUserId()
                : request.getUserId();

        User user = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + actorId));

        Activity activity = activityMapper.toEntity(request);
        activity.setIssue(issue);
        activity.setUser(user);

        Activity saved = activityRepository.save(activity);
        return activityMapper.toResponse(saved);
    }

    @Transactional
    public ActivityResponse createActivity(UUID issueId, UUID userId, ActivityAction action, String oldValue, String newValue) {
        return createActivity(new ActivityCreateRequest(issueId, userId, action, oldValue, newValue));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> getActivitiesByIssue(UUID issueId, int page, int size, String sort) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + issueId));

        authorizationService.requireProjectAccess(issue.getProject());

        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.ACTIVITY_SORT_FIELDS, "createdAt,desc"
        );

        return PageResponse.of(activityRepository.findByIssueId(issueId, pageable)
                .map(activityMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> getActivitiesByIssue(UUID issueId, Pageable pageable) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + issueId));

        authorizationService.requireProjectAccess(issue.getProject());

        return PageResponse.of(activityRepository.findByIssueIdOrderByCreatedAtDesc(issueId, pageable)
                .map(activityMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> getRecentActivitiesByProject(UUID projectId, int limit) {
        if (projectRepository != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
            authorizationService.requireProjectAccess(project);
        }

        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 50));
        List<Activity> activities = activityRepository.findRecentActivitiesIn(Collections.singletonList(projectId), pageable);
        if (activities == null || activities.isEmpty()) {
            return Collections.emptyList();
        }
        return activities.stream().map(activityMapper::toResponse).toList();
    }
}
