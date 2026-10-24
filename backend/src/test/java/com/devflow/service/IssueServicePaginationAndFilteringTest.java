package com.devflow.service;

import com.devflow.dto.response.IssueResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.entity.Issue;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.UserRole;
import com.devflow.mapper.IssueMapper;
import com.devflow.repository.IssueRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServicePaginationAndFilteringTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueMapper issueMapper;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private com.devflow.repository.UserRepository userRepository;

    @Mock
    private com.devflow.repository.ProjectRepository projectRepository;

    @Mock
    private com.devflow.repository.SprintRepository sprintRepository;

    @InjectMocks
    private IssueService issueService;

    @Test
    @DisplayName("getIssues - all filters supplied passes to specification and returns PageResponse")
    void testGetIssues_AllFiltersSupplied() {
        UUID projectId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);

        Issue issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setTitle("Filter test issue");

        IssueResponse response = new IssueResponse();
        response.setId(issue.getId());
        response.setTitle("Filter test issue");

        Page<Issue> page = new PageImpl<>(List.of(issue));
        when(issueRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(issueMapper.toResponse(issue)).thenReturn(response);

        PageResponse<IssueResponse> result = issueService.getIssues(
                projectId,
                sprintId,
                assignedToId,
                IssueStatus.IN_PROGRESS,
                IssuePriority.HIGH,
                IssueType.BUG,
                "auth",
                0,
                10,
                "createdAt,desc"
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(0, result.getPage());
        assertEquals(1, result.getSize());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertEquals("Filter test issue", result.getContent().get(0).getTitle());

        verify(authorizationService).requireProjectAccess(projectId);
    }

    @Test
    @DisplayName("getIssues - unauthorized project access throws AccessDeniedException")
    void testGetIssues_UnauthorizedProject() {
        UUID projectId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Access denied: You do not have access to this project"))
                .when(authorizationService).requireProjectAccess(projectId);

        assertThrows(AccessDeniedException.class, () -> issueService.getIssues(
                projectId, null, null, null, null, null, null, 0, 20, "createdAt,desc"
        ));
    }

    @Test
    @DisplayName("getIssues - no filters returns all accessible issues for user")
    void testGetIssues_NoFilters() {
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);

        Issue issue1 = new Issue();
        issue1.setId(UUID.randomUUID());
        Issue issue2 = new Issue();
        issue2.setId(UUID.randomUUID());

        IssueResponse resp1 = new IssueResponse();
        resp1.setId(issue1.getId());
        IssueResponse resp2 = new IssueResponse();
        resp2.setId(issue2.getId());

        Page<Issue> page = new PageImpl<>(List.of(issue1, issue2));
        when(issueRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(issueMapper.toResponse(issue1)).thenReturn(resp1);
        when(issueMapper.toResponse(issue2)).thenReturn(resp2);

        PageResponse<IssueResponse> result = issueService.getIssues(
                null, null, null, null, null, null, null, 0, 20, null
        );

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    @DisplayName("getIssues - empty result returns empty PageResponse")
    void testGetIssues_EmptyResult() {
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);

        Page<Issue> emptyPage = new PageImpl<>(Collections.emptyList());
        when(issueRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<IssueResponse> result = issueService.getIssues(
                null, null, null, IssueStatus.DONE, null, null, "nonexistent", 0, 20, "title,asc"
        );

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("getIssuesByStatus - scopes queries to user's accessible projects via specification")
    void testGetIssuesByStatus_ScopesToAccessibleProjects() {
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);

        Issue issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setStatus(IssueStatus.IN_PROGRESS);

        IssueResponse response = new IssueResponse();
        response.setId(issue.getId());
        response.setStatus(IssueStatus.IN_PROGRESS);

        Page<Issue> page = new PageImpl<>(List.of(issue));
        when(issueRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(issueMapper.toResponse(issue)).thenReturn(response);

        PageResponse<IssueResponse> result = issueService.getIssuesByStatus(IssueStatus.IN_PROGRESS, org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(IssueStatus.IN_PROGRESS, result.getContent().get(0).getStatus());
        verify(issueRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getIssuesByPriority - scopes queries to user's accessible projects via specification")
    void testGetIssuesByPriority_ScopesToAccessibleProjects() {
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);

        Issue issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setPriority(IssuePriority.HIGH);

        IssueResponse response = new IssueResponse();
        response.setId(issue.getId());
        response.setPriority(IssuePriority.HIGH);

        Page<Issue> page = new PageImpl<>(List.of(issue));
        when(issueRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(issueMapper.toResponse(issue)).thenReturn(response);

        PageResponse<IssueResponse> result = issueService.getIssuesByPriority(IssuePriority.HIGH, org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(IssuePriority.HIGH, result.getContent().get(0).getPriority());
        verify(issueRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getIssuesByAssignee - scopes queries to user's accessible projects via specification")
    void testGetIssuesByAssignee_ScopesToAccessibleProjects() {
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);
        when(userRepository.existsById(currentUserId)).thenReturn(true);

        Issue issue = new Issue();
        issue.setId(UUID.randomUUID());

        IssueResponse response = new IssueResponse();
        response.setId(issue.getId());

        Page<Issue> page = new PageImpl<>(List.of(issue));
        when(issueRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(issueMapper.toResponse(issue)).thenReturn(response);

        PageResponse<IssueResponse> result = issueService.getIssuesByAssignee(currentUserId, org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(issueRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
