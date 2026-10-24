package com.devflow.service;

import com.devflow.dto.request.ProjectCreateRequest;
import com.devflow.dto.request.ProjectUpdateRequest;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.entity.Project;
import com.devflow.entity.User;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.ProjectMapper;
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

import com.devflow.dto.response.ProjectSummaryResponse;
import com.devflow.entity.Sprint;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.SprintRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    @DisplayName("createProject - success")
    void testCreateProject_Success() {
        UUID userId = UUID.randomUUID();
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("DevFlow");
        request.setCreatedById(userId);
        request.setStatus(ProjectStatus.ACTIVE);

        User user = new User();
        user.setId(userId);

        Project project = new Project();
        Project savedProject = new Project();
        savedProject.setId(UUID.randomUUID());

        ProjectResponse response = new ProjectResponse();
        response.setId(savedProject.getId());

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(savedProject);
        when(projectMapper.toResponse(savedProject)).thenReturn(response);

        ProjectResponse result = projectService.createProject(request);

        assertNotNull(result);
        assertEquals(response.getId(), result.getId());
        verify(authorizationService).requireProjectCreateAccess();
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("createProject - creator not found throws ResourceNotFoundException")
    void testCreateProject_CreatorNotFound() {
        UUID userId = UUID.randomUUID();
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setCreatedById(userId);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.createProject(request));
    }

    @Test
    @DisplayName("getProjectById - not found throws ResourceNotFoundException")
    void testGetProjectById_NotFound() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectById(id));
    }

    @Test
    @DisplayName("updateProject - success")
    void testUpdateProject_Success() {
        UUID id = UUID.randomUUID();
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("New Name");

        Project project = new Project();
        project.setId(id);

        ProjectResponse response = new ProjectResponse();
        response.setId(id);
        response.setName("New Name");

        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(response);

        ProjectResponse result = projectService.updateProject(id, request);

        assertNotNull(result);
        verify(authorizationService).requireProjectManagerOrAdmin(project);
        verify(projectMapper).updateEntity(request, project);
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("updateProject - not found throws ResourceNotFoundException")
    void testUpdateProject_NotFound() {
        UUID id = UUID.randomUUID();
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.updateProject(id, request));
    }

    @Test
    @DisplayName("deleteProject - success")
    void testDeleteProject_Success() {
        UUID id = UUID.randomUUID();
        Project project = new Project();
        project.setId(id);

        when(projectRepository.findById(id)).thenReturn(Optional.of(project));

        projectService.deleteProject(id);

        verify(authorizationService).requireProjectManagerOrAdmin(project);
        verify(projectRepository).delete(project);
    }

    @Test
    @DisplayName("deleteProject - not found throws ResourceNotFoundException")
    void testDeleteProject_NotFound() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.deleteProject(id));
    }

    @Test
    @DisplayName("getProjectSummary - success with active sprint and issues")
    void testGetProjectSummary_Success() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        project.setName("DevFlow Core");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        List<Object[]> statusCounts = List.<Object[]>of(
                new Object[]{IssueStatus.TODO, 3L},
                new Object[]{IssueStatus.IN_PROGRESS, 2L},
                new Object[]{IssueStatus.CODE_REVIEW, 1L},
                new Object[]{IssueStatus.DONE, 4L}
        );
        when(issueRepository.countIssuesByStatusIn(Collections.singletonList(projectId))).thenReturn(statusCounts);
        when(projectMemberRepository.countByProjectId(projectId)).thenReturn(5L);

        UUID sprintId = UUID.randomUUID();
        Sprint activeSprint = new Sprint();
        activeSprint.setId(sprintId);
        activeSprint.setName("Sprint 1");
        activeSprint.setStatus(SprintStatus.ACTIVE);
        activeSprint.setStartDate(Instant.parse("2026-10-01T00:00:00Z"));
        activeSprint.setEndDate(Instant.parse("2026-10-15T00:00:00Z"));

        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE))
                .thenReturn(List.of(activeSprint));

        List<Object[]> sprintStatusCounts = List.<Object[]>of(
                new Object[]{IssueStatus.TODO, 1L},
                new Object[]{IssueStatus.IN_PROGRESS, 1L},
                new Object[]{IssueStatus.DONE, 2L}
        );
        when(issueRepository.countIssuesByStatusAndSprintId(sprintId)).thenReturn(sprintStatusCounts);

        ProjectSummaryResponse summary = projectService.getProjectSummary(projectId);

        assertEquals(projectId, summary.getProjectId());
        assertEquals("DevFlow Core", summary.getProjectName());
        assertEquals(10L, summary.getTotalIssues());
        assertEquals(6L, summary.getOpenIssues());
        assertEquals(3L, summary.getTodoIssues());
        assertEquals(2L, summary.getInProgressIssues());
        assertEquals(1L, summary.getCodeReviewIssues());
        assertEquals(4L, summary.getDoneIssues());
        assertEquals(5L, summary.getMemberCount());

        assertNotNull(summary.getActiveSprint());
        assertEquals("Sprint 1", summary.getActiveSprint().getName());
        assertEquals(4L, summary.getActiveSprint().getTotalIssues());
        assertEquals(2L, summary.getActiveSprint().getCompletedIssues());
        assertEquals(50, summary.getActiveSprint().getProgressPercentage());

        verify(authorizationService).requireProjectAccess(project);
    }
}
