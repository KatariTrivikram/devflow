package com.devflow.service;

import com.devflow.dto.request.ProjectMemberCreateRequest;
import com.devflow.dto.response.ProjectMemberResponse;
import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.User;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.DuplicateResourceException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.ProjectMemberMapper;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectMemberRepository;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private NotificationService notificationService;

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    @Test
    @DisplayName("addMember - success")
    void testAddMember_Success() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProjectMemberCreateRequest request = new ProjectMemberCreateRequest();
        request.setProjectId(projectId);
        request.setUserId(userId);

        Project project = new Project();
        project.setId(projectId);

        User user = new User();
        user.setId(userId);

        ProjectMember member = new ProjectMember();
        ProjectMember savedMember = new ProjectMember();
        savedMember.setId(UUID.randomUUID());

        ProjectMemberResponse response = new ProjectMemberResponse();
        response.setId(savedMember.getId());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);
        when(projectMemberMapper.toEntity(request)).thenReturn(member);
        when(projectMemberRepository.save(member)).thenReturn(savedMember);
        when(projectMemberMapper.toResponse(savedMember)).thenReturn(response);

        ProjectMemberResponse result = projectMemberService.addMember(request);

        assertNotNull(result);
        assertEquals(response.getId(), result.getId());
        verify(authorizationService).requireProjectManagerOrAdmin(project);
        verify(projectMemberRepository).save(member);
    }

    @Test
    @DisplayName("addMember - duplicate membership throws DuplicateResourceException")
    void testAddMember_Duplicate() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProjectMemberCreateRequest request = new ProjectMemberCreateRequest();
        request.setProjectId(projectId);
        request.setUserId(userId);

        Project project = new Project();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> projectMemberService.addMember(request));
        verify(authorizationService).requireProjectManagerOrAdmin(project);
    }

    @Test
    @DisplayName("addMember - missing project throws ResourceNotFoundException")
    void testAddMember_MissingProject() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProjectMemberCreateRequest request = new ProjectMemberCreateRequest();
        request.setProjectId(projectId);
        request.setUserId(userId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectMemberService.addMember(request));
    }

    @Test
    @DisplayName("addMember - missing user throws ResourceNotFoundException")
    void testAddMember_MissingUser() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProjectMemberCreateRequest request = new ProjectMemberCreateRequest();
        request.setProjectId(projectId);
        request.setUserId(userId);

        Project project = new Project();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectMemberService.addMember(request));
        verify(authorizationService).requireProjectManagerOrAdmin(project);
    }

    @Test
    @DisplayName("removeMember - success")
    void testRemoveMember_Success() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Project project = new Project();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(issueRepository.countByProjectIdAndAssignedToId(projectId, userId)).thenReturn(0L);

        ProjectMember member = new ProjectMember();
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        projectMemberService.removeMember(projectId, userId);

        verify(authorizationService).requireProjectManagerOrAdmin(project);
        verify(issueRepository).countByProjectIdAndAssignedToId(projectId, userId);
        verify(projectMemberRepository).delete(member);
    }

    @Test
    @DisplayName("removeMember - throws BusinessRuleException when user has assigned issues")
    void testRemoveMember_AssignedIssues_ThrowsBusinessRuleException() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Project project = new Project();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(issueRepository.countByProjectIdAndAssignedToId(projectId, userId)).thenReturn(3L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> projectMemberService.removeMember(projectId, userId));

        assertEquals("Cannot remove member who is assigned to 3 issue(s) in this project. Please reassign or unassign their issues first.",
                ex.getMessage());
        verify(authorizationService).requireProjectManagerOrAdmin(project);
    }
}
