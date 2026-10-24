package com.devflow.security;

import com.devflow.entity.Comment;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.User;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.BusinessRuleException;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private UUID adminId;
    private UUID pmId;
    private UUID devId;
    private UUID otherDevId;

    private User pmUser;
    private User devUser;
    private User otherDevUser;

    private Project project;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        pmId = UUID.randomUUID();
        devId = UUID.randomUUID();
        otherDevId = UUID.randomUUID();

        pmUser = new User();
        pmUser.setId(pmId);
        pmUser.setRole(UserRole.PROJECT_MANAGER);

        devUser = new User();
        devUser.setId(devId);
        devUser.setRole(UserRole.DEVELOPER);

        otherDevUser = new User();
        otherDevUser.setId(otherDevId);
        otherDevUser.setRole(UserRole.DEVELOPER);

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setCreatedBy(pmUser);
    }

    @Test
    @DisplayName("Role helpers identify correct roles")
    void testRoleHelpers() {
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        assertTrue(authorizationService.isAdmin());
        assertFalse(authorizationService.isProjectManager());
        assertFalse(authorizationService.isDeveloper());

        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);
        assertFalse(authorizationService.isAdmin());
        assertTrue(authorizationService.isProjectManager());
        assertFalse(authorizationService.isDeveloper());

        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);
        assertFalse(authorizationService.isAdmin());
        assertFalse(authorizationService.isProjectManager());
        assertTrue(authorizationService.isDeveloper());
    }

    @Test
    @DisplayName("requireAdmin succeeds for Admin, throws AccessDeniedException for Developer")
    void testRequireAdmin() {
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        assertDoesNotThrow(() -> authorizationService.requireAdmin());

        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireAdmin());
    }

    @Test
    @DisplayName("requireProjectCreateAccess allows Admin and PM, forbids Developer")
    void testRequireProjectCreateAccess() {
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        assertDoesNotThrow(() -> authorizationService.requireProjectCreateAccess());

        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);
        assertDoesNotThrow(() -> authorizationService.requireProjectCreateAccess());

        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireProjectCreateAccess());
    }

    @Test
    @DisplayName("requireProjectManagerOrAdmin allows Admin and creating PM, rejects unrelated PM and Developer")
    void testRequireProjectManagerOrAdmin() {
        // 1. Admin allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        assertDoesNotThrow(() -> authorizationService.requireProjectManagerOrAdmin(project));

        // 2. Creating PM allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);
        when(securityUtils.getCurrentUserId()).thenReturn(pmId);
        assertDoesNotThrow(() -> authorizationService.requireProjectManagerOrAdmin(project));

        // 3. Unrelated PM rejected
        UUID unrelatedPmId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(unrelatedPmId);
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireProjectManagerOrAdmin(project));

        // 4. Developer rejected
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireProjectManagerOrAdmin(project));
    }

    @Test
    @DisplayName("requireProjectAccess allows Admin, PM creator, and Developer member; rejects non-member")
    void testRequireProjectAccess() {
        // 1. Admin allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        assertDoesNotThrow(() -> authorizationService.requireProjectAccess(project));

        // 2. PM creator allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);
        when(securityUtils.getCurrentUserId()).thenReturn(pmId);
        assertDoesNotThrow(() -> authorizationService.requireProjectAccess(project));

        // 3. Developer member allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);
        when(securityUtils.getCurrentUserId()).thenReturn(devId);
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), devId)).thenReturn(true);
        assertDoesNotThrow(() -> authorizationService.requireProjectAccess(project));

        // 4. Developer non-member rejected
        when(securityUtils.getCurrentUserId()).thenReturn(otherDevId);
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), otherDevId)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireProjectAccess(project));
    }

    @Test
    @DisplayName("requireIssueUpdateAccess allows Admin, PM, and assigned Developer; rejects unassigned Developer")
    void testRequireIssueUpdateAccess() {
        Issue issue = new Issue();
        issue.setProject(project);
        issue.setAssignedTo(devUser);

        // 1. Admin allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        assertDoesNotThrow(() -> authorizationService.requireIssueUpdateAccess(issue));

        // 2. Managing PM allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);
        when(securityUtils.getCurrentUserId()).thenReturn(pmId);
        assertDoesNotThrow(() -> authorizationService.requireIssueUpdateAccess(issue));

        // 3. Assigned Developer allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);
        when(securityUtils.getCurrentUserId()).thenReturn(devId);
        assertDoesNotThrow(() -> authorizationService.requireIssueUpdateAccess(issue));

        // 4. Another Developer rejected
        when(securityUtils.getCurrentUserId()).thenReturn(otherDevId);
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireIssueUpdateAccess(issue));
    }

    @Test
    @DisplayName("requireCommentModifyAccess allows author, PM, and Admin; rejects unrelated user")
    void testRequireCommentModifyAccess() {
        Issue issue = new Issue();
        issue.setProject(project);

        Comment comment = new Comment();
        comment.setIssue(issue);
        comment.setUser(devUser);

        // 1. Admin allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.ADMIN);
        assertDoesNotThrow(() -> authorizationService.requireCommentModifyAccess(comment));

        // 2. Managing PM allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);
        when(securityUtils.getCurrentUserId()).thenReturn(pmId);
        assertDoesNotThrow(() -> authorizationService.requireCommentModifyAccess(comment));

        // 3. Comment author allowed
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.DEVELOPER);
        when(securityUtils.getCurrentUserId()).thenReturn(devId);
        assertDoesNotThrow(() -> authorizationService.requireCommentModifyAccess(comment));

        // 4. Unrelated developer rejected
        when(securityUtils.getCurrentUserId()).thenReturn(otherDevId);
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireCommentModifyAccess(comment));
    }

    @Test
    @DisplayName("validateIssueAssignment validates project membership or creator; rejects non-member")
    void testValidateIssueAssignment() {
        // 1. Project creator allowed
        assertDoesNotThrow(() -> authorizationService.validateIssueAssignment(project, pmId));

        // 2. Project member allowed
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), devId)).thenReturn(true);
        assertDoesNotThrow(() -> authorizationService.validateIssueAssignment(project, devId));

        // 3. Non-member rejected with BusinessRuleException
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), otherDevId)).thenReturn(false);
        assertThrows(BusinessRuleException.class, () -> authorizationService.validateIssueAssignment(project, otherDevId));
    }
}
