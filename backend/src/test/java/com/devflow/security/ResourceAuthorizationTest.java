package com.devflow.security;

import com.devflow.dto.request.CommentCreateRequest;
import com.devflow.dto.request.IssueCreateRequest;
import com.devflow.dto.request.IssueUpdateRequest;
import com.devflow.dto.request.ProjectCreateRequest;
import com.devflow.dto.request.ProjectMemberCreateRequest;
import com.devflow.dto.request.SprintCreateRequest;
import com.devflow.entity.Comment;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.Sprint;
import com.devflow.entity.User;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.BusinessRuleException;
import com.devflow.mapper.CommentMapper;
import com.devflow.mapper.IssueMapper;
import com.devflow.mapper.ProjectMapper;
import com.devflow.mapper.ProjectMemberMapper;
import com.devflow.mapper.SprintMapper;
import com.devflow.repository.CommentRepository;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.repository.UserRepository;
import com.devflow.service.CommentService;
import com.devflow.service.IssueService;
import com.devflow.service.ProjectMemberService;
import com.devflow.service.ProjectService;
import com.devflow.service.SprintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceAuthorizationTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private SprintMapper sprintMapper;

    @Mock
    private IssueMapper issueMapper;

    @Mock
    private CommentMapper commentMapper;

    private SecurityUtils securityUtils;
    private AuthorizationService authorizationService;

    private ProjectService projectService;
    private ProjectMemberService projectMemberService;
    private SprintService sprintService;
    private IssueService issueService;
    private CommentService commentService;

    private UUID adminId;
    private UUID pmId;
    private UUID devAId;
    private UUID devBId;

    private User pmUser;
    private User devAUser;
    private User devBUser;

    private Project project;

    @BeforeEach
    void setUp() {
        securityUtils = new SecurityUtils();
        authorizationService = new AuthorizationService(securityUtils, projectRepository, projectMemberRepository);

        projectService = new ProjectService(projectRepository, userRepository, projectMapper, authorizationService, securityUtils);
        projectMemberService = new ProjectMemberService(projectMemberRepository, projectRepository, userRepository, projectMemberMapper, authorizationService, securityUtils);
        sprintService = new SprintService(sprintRepository, projectRepository, sprintMapper, authorizationService);
        issueService = new IssueService(issueRepository, projectRepository, sprintRepository, userRepository, issueMapper, authorizationService, securityUtils);
        commentService = new CommentService(commentRepository, issueRepository, userRepository, commentMapper, authorizationService, securityUtils);

        adminId = UUID.randomUUID();
        pmId = UUID.randomUUID();
        devAId = UUID.randomUUID();
        devBId = UUID.randomUUID();

        pmUser = new User();
        pmUser.setId(pmId);
        pmUser.setRole(UserRole.PROJECT_MANAGER);

        devAUser = new User();
        devAUser.setId(devAId);
        devAUser.setRole(UserRole.DEVELOPER);

        devBUser = new User();
        devBUser.setId(devBId);
        devBUser.setRole(UserRole.DEVELOPER);

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Project Alpha");
        project.setCreatedBy(pmUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(UUID id, String email, UserRole role) {
        CustomUserDetails userDetails = new CustomUserDetails(id, email, "password", role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("IDOR Prevention: Developer B cannot access Project Alpha if not a member")
    void testIdor_DeveloperCannotAccessNonMemberProject() {
        authenticateUser(devBId, "devb@example.com", UserRole.DEVELOPER);

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), devBId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> projectService.getProjectById(project.getId()));
    }

    @Test
    @DisplayName("IDOR Prevention: Developer B cannot update Issue assigned to Developer A")
    void testIdor_DeveloperCannotUpdateAnotherDevelopersIssue() {
        authenticateUser(devBId, "devb@example.com", UserRole.DEVELOPER);

        Issue issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setProject(project);
        issue.setAssignedTo(devAUser);

        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));

        IssueUpdateRequest updateRequest = new IssueUpdateRequest();
        updateRequest.setTitle("Hijacked Title");

        assertThrows(AccessDeniedException.class, () -> issueService.updateIssue(issue.getId(), updateRequest));
    }

    @Test
    @DisplayName("Authorized Access: Developer A can update issue assigned to self")
    void testAuthorized_DeveloperCanUpdateOwnIssue() {
        authenticateUser(devAId, "deva@example.com", UserRole.DEVELOPER);

        Issue issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setProject(project);
        issue.setAssignedTo(devAUser);

        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(issueRepository.save(issue)).thenReturn(issue);

        IssueUpdateRequest updateRequest = new IssueUpdateRequest();
        updateRequest.setTitle("Updated Title");

        assertDoesNotThrow(() -> issueService.updateIssue(issue.getId(), updateRequest));
    }

    @Test
    @DisplayName("IDOR Prevention: Developer B cannot delete Comment authored by Developer A")
    void testIdor_DeveloperCannotDeleteAnotherDevelopersComment() {
        authenticateUser(devBId, "devb@example.com", UserRole.DEVELOPER);

        Issue issue = new Issue();
        issue.setProject(project);

        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setIssue(issue);
        comment.setUser(devAUser);

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThrows(AccessDeniedException.class, () -> commentService.deleteComment(comment.getId()));
    }

    @Test
    @DisplayName("Authorized Access: Comment author can delete own comment")
    void testAuthorized_AuthorCanDeleteOwnComment() {
        authenticateUser(devAId, "deva@example.com", UserRole.DEVELOPER);

        Issue issue = new Issue();
        issue.setProject(project);

        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setIssue(issue);
        comment.setUser(devAUser);

        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertDoesNotThrow(() -> commentService.deleteComment(comment.getId()));
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Assignment Validation: Cannot assign issue to user who is not a member of project")
    void testAssignmentValidation_NonMemberRejected() {
        authenticateUser(pmId, "pm@example.com", UserRole.PROJECT_MANAGER);

        IssueCreateRequest request = new IssueCreateRequest();
        request.setProjectId(project.getId());
        request.setTitle("Task 1");
        request.setType(IssueType.TASK);
        request.setPriority(IssuePriority.MEDIUM);
        request.setStatus(IssueStatus.TODO);
        request.setAssignedToId(devBId);

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findById(devBId)).thenReturn(Optional.of(devBUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), devBId)).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> issueService.createIssue(request));
        assertEquals("Assigned user does not belong to project", ex.getMessage());
    }

    @Test
    @DisplayName("Membership Management: Developer cannot add or remove project members")
    void testMembershipManagement_DeveloperForbidden() {
        authenticateUser(devAId, "deva@example.com", UserRole.DEVELOPER);

        ProjectMemberCreateRequest request = new ProjectMemberCreateRequest();
        request.setProjectId(project.getId());
        request.setUserId(devBId);

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class, () -> projectMemberService.addMember(request));
        assertThrows(AccessDeniedException.class, () -> projectMemberService.removeMember(project.getId(), devBId));
    }

    @Test
    @DisplayName("Sprint Management: Developer cannot create or delete sprints")
    void testSprintManagement_DeveloperForbidden() {
        authenticateUser(devAId, "deva@example.com", UserRole.DEVELOPER);

        SprintCreateRequest request = new SprintCreateRequest();
        request.setProjectId(project.getId());
        request.setName("Sprint 1");
        request.setStartDate(Instant.now());
        request.setEndDate(Instant.now().plusSeconds(86400));
        request.setStatus(SprintStatus.PLANNED);

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class, () -> sprintService.createSprint(request));
    }

    @Test
    @DisplayName("Trusted Identity: Comment actor is set to authenticated user, ignoring client-supplied userId")
    void testCommentActor_UsesAuthenticatedUser() {
        authenticateUser(devAId, "deva@example.com", UserRole.DEVELOPER);

        Issue issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setProject(project);

        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), devAId)).thenReturn(true);
        when(userRepository.findById(devAId)).thenReturn(Optional.of(devAUser));

        Comment comment = new Comment();
        when(commentMapper.toEntity(any())).thenReturn(comment);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // Client maliciously supplies devBId as userId in request body
        CommentCreateRequest request = new CommentCreateRequest();
        request.setIssueId(issue.getId());
        request.setUserId(devBId);
        request.setContent("Authentic comment");

        commentService.createComment(request);

        // Verified that user is devAUser (the authenticated actor), NOT devBUser
        assertEquals(devAUser, comment.getUser());
    }

    @Test
    @DisplayName("Trusted Identity: Project creator is set to authenticated user, ignoring client-supplied createdById")
    void testProjectCreator_UsesAuthenticatedUser() {
        authenticateUser(pmId, "pm@example.com", UserRole.PROJECT_MANAGER);

        when(userRepository.findById(pmId)).thenReturn(Optional.of(pmUser));

        Project createdProject = new Project();
        when(projectMapper.toEntity(any())).thenReturn(createdProject);
        when(projectRepository.save(any(Project.class))).thenReturn(createdProject);

        // Client maliciously supplies adminId as createdById
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("New Project");
        request.setCreatedById(adminId);
        request.setStatus(ProjectStatus.ACTIVE);

        projectService.createProject(request);

        // Verified that project creator is pmUser (the authenticated actor)
        assertEquals(pmUser, createdProject.getCreatedBy());
    }
}
