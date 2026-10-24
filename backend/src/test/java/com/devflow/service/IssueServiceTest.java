package com.devflow.service;

import com.devflow.dto.request.IssueCreateRequest;
import com.devflow.dto.request.IssueUpdateRequest;
import com.devflow.dto.response.IssueResponse;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.Sprint;
import com.devflow.entity.User;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.IssueMapper;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.AuthorizationService;
import com.devflow.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssueMapper issueMapper;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private IssueService issueService;

    @Test
    @DisplayName("createIssue - success")
    void testCreateIssue_Success() {
        UUID projectId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        IssueCreateRequest request = new IssueCreateRequest();
        request.setProjectId(projectId);
        request.setSprintId(sprintId);
        request.setAssignedToId(userId);
        request.setTitle("Bug in auth");
        request.setType(IssueType.BUG);
        request.setPriority(IssuePriority.HIGH);
        request.setStatus(IssueStatus.TODO);

        Project project = new Project();
        project.setId(projectId);

        Sprint sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setProject(project);

        User user = new User();
        user.setId(userId);

        Issue issue = new Issue();
        Issue savedIssue = new Issue();
        savedIssue.setId(UUID.randomUUID());

        IssueResponse response = new IssueResponse();
        response.setId(savedIssue.getId());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(issueMapper.toEntity(request)).thenReturn(issue);
        when(issueRepository.save(issue)).thenReturn(savedIssue);
        when(issueMapper.toResponse(savedIssue)).thenReturn(response);

        IssueResponse result = issueService.createIssue(request);

        assertNotNull(result);
        assertEquals(response.getId(), result.getId());
        verify(authorizationService).requireProjectManagerOrAdmin(project);
        verify(authorizationService).validateIssueAssignment(project, userId);
        verify(issueRepository).save(issue);
    }

    @Test
    @DisplayName("createIssue - missing project throws ResourceNotFoundException")
    void testCreateIssue_MissingProject() {
        UUID projectId = UUID.randomUUID();
        IssueCreateRequest request = new IssueCreateRequest();
        request.setProjectId(projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> issueService.createIssue(request));
    }

    @Test
    @DisplayName("createIssue - missing sprint throws ResourceNotFoundException")
    void testCreateIssue_MissingSprint() {
        UUID projectId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();

        IssueCreateRequest request = new IssueCreateRequest();
        request.setProjectId(projectId);
        request.setSprintId(sprintId);

        Project project = new Project();
        project.setId(projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> issueService.createIssue(request));
        verify(authorizationService).requireProjectManagerOrAdmin(project);
    }

    @Test
    @DisplayName("createIssue - missing assignee throws ResourceNotFoundException")
    void testCreateIssue_MissingAssignee() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        IssueCreateRequest request = new IssueCreateRequest();
        request.setProjectId(projectId);
        request.setAssignedToId(userId);

        Project project = new Project();
        project.setId(projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> issueService.createIssue(request));
        verify(authorizationService).requireProjectManagerOrAdmin(project);
    }

    @Test
    @DisplayName("getIssueById - not found throws ResourceNotFoundException")
    void testGetIssueById_NotFound() {
        UUID id = UUID.randomUUID();
        when(issueRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> issueService.getIssueById(id));
    }

    @Test
    @DisplayName("updateIssue - not found throws ResourceNotFoundException")
    void testUpdateIssue_NotFound() {
        UUID id = UUID.randomUUID();
        IssueUpdateRequest request = new IssueUpdateRequest();
        when(issueRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> issueService.updateIssue(id, request));
    }

    @Test
    @DisplayName("deleteIssue - not found throws ResourceNotFoundException")
    void testDeleteIssue_NotFound() {
        UUID id = UUID.randomUUID();
        when(issueRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> issueService.deleteIssue(id));
    }

    @Test
    @DisplayName("deleteIssue - success")
    void testDeleteIssue_Success() {
        UUID id = UUID.randomUUID();
        Issue issue = new Issue();
        issue.setId(id);
        issue.setProject(new Project());

        when(issueRepository.findById(id)).thenReturn(Optional.of(issue));

        issueService.deleteIssue(id);

        verify(authorizationService).requireProjectManagerOrAdmin(issue.getProject());
        verify(issueRepository).delete(issue);
    }
}
