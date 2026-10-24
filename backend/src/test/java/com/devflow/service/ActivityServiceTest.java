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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ActivityService activityService;

    @Test
    @DisplayName("createActivity - success")
    void testCreateActivity_Success() {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ActivityCreateRequest request = new ActivityCreateRequest(
                issueId, userId, ActivityAction.STATUS_CHANGED, "TODO", "IN_PROGRESS");

        Issue issue = new Issue();
        User user = new User();
        Activity activity = new Activity();
        Activity savedActivity = new Activity();
        savedActivity.setId(UUID.randomUUID());

        ActivityResponse response = new ActivityResponse();
        response.setId(savedActivity.getId());
        response.setAction(ActivityAction.STATUS_CHANGED);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(securityUtils.isAuthenticated()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityMapper.toEntity(request)).thenReturn(activity);
        when(activityRepository.save(activity)).thenReturn(savedActivity);
        when(activityMapper.toResponse(savedActivity)).thenReturn(response);

        ActivityResponse result = activityService.createActivity(request);

        assertNotNull(result);
        assertEquals(response.getId(), result.getId());
        verify(authorizationService).requireProjectAccess(issue.getProject());
        verify(activityRepository).save(activity);
    }

    @Test
    @DisplayName("createActivity - missing issue throws ResourceNotFoundException")
    void testCreateActivity_MissingIssue() {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ActivityCreateRequest request = new ActivityCreateRequest(
                issueId, userId, ActivityAction.COMMENT_ADDED, null, null);

        when(issueRepository.findById(issueId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> activityService.createActivity(request));
    }

    @Test
    @DisplayName("createActivity - missing user throws ResourceNotFoundException")
    void testCreateActivity_MissingUser() {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ActivityCreateRequest request = new ActivityCreateRequest(
                issueId, userId, ActivityAction.COMMENT_ADDED, null, null);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(new Issue()));
        when(securityUtils.isAuthenticated()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> activityService.createActivity(request));
    }

    @Test
    @DisplayName("getActivitiesByIssue - success")
    void testGetActivitiesByIssue_Success() {
        UUID issueId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Issue issue = new Issue();
        issue.setProject(new Project());
        Activity activity = new Activity();
        ActivityResponse response = new ActivityResponse();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(activityRepository.findByIssueIdOrderByCreatedAtDesc(issueId, pageable))
                .thenReturn(new PageImpl<>(List.of(activity)));
        when(activityMapper.toResponse(activity)).thenReturn(response);

        PageResponse<ActivityResponse> result = activityService.getActivitiesByIssue(issueId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(authorizationService).requireProjectAccess(issue.getProject());
    }

    @Test
    @DisplayName("getActivitiesByIssue - missing issue throws ResourceNotFoundException")
    void testGetActivitiesByIssue_MissingIssue() {
        UUID issueId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(issueRepository.findById(issueId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> activityService.getActivitiesByIssue(issueId, pageable));
    }

    @Test
    @DisplayName("getRecentActivitiesByProject - success")
    void testGetRecentActivitiesByProject_Success() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);

        Activity activity = new Activity();
        ActivityResponse response = new ActivityResponse();
        response.setId(UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(activityRepository.findRecentActivitiesIn(java.util.Collections.singletonList(projectId), PageRequest.of(0, 10)))
                .thenReturn(List.of(activity));
        when(activityMapper.toResponse(activity)).thenReturn(response);

        List<ActivityResponse> result = activityService.getRecentActivitiesByProject(projectId, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(response.getId(), result.get(0).getId());
        verify(authorizationService).requireProjectAccess(project);
    }
}
