package com.devflow.service;

import com.devflow.dto.request.CommentCreateRequest;
import com.devflow.dto.request.IssueCreateRequest;
import com.devflow.dto.request.IssueUpdateRequest;
import com.devflow.dto.request.ProjectMemberCreateRequest;
import com.devflow.dto.request.SprintUpdateRequest;
import com.devflow.entity.Comment;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.Sprint;
import com.devflow.entity.User;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.NotificationReferenceType;
import com.devflow.entity.enums.NotificationType;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.mapper.CommentMapper;
import com.devflow.mapper.IssueMapper;
import com.devflow.mapper.ProjectMemberMapper;
import com.devflow.mapper.SprintMapper;
import com.devflow.repository.CommentRepository;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.AuthorizationService;
import com.devflow.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationGenerationTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private IssueMapper issueMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private SprintMapper sprintMapper;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityUtils securityUtils;

    private IssueService issueService;
    private CommentService commentService;
    private ProjectMemberService projectMemberService;
    private SprintService sprintService;

    private User actor;
    private User assignee;
    private Project project;
    private Issue issue;

    @BeforeEach
    void setUp() {
        issueService = new IssueService(
                issueRepository, projectRepository, sprintRepository, userRepository,
                issueMapper, authorizationService, securityUtils, notificationService
        );

        commentService = new CommentService(
                commentRepository, issueRepository, userRepository,
                commentMapper, authorizationService, securityUtils, notificationService
        );

        projectMemberService = new ProjectMemberService(
                projectMemberRepository, projectRepository, userRepository,
                projectMemberMapper, authorizationService, securityUtils, notificationService
        );

        sprintService = new SprintService(
                sprintRepository, projectRepository, sprintMapper, authorizationService,
                notificationService, projectMemberRepository, userRepository, securityUtils
        );

        actor = new User();
        actor.setId(UUID.randomUUID());
        actor.setName("PM Actor");

        assignee = new User();
        assignee.setId(UUID.randomUUID());
        assignee.setName("Dev Assignee");

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("DevFlow Engine");
        project.setCreatedBy(actor);

        issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setTitle("Database optimization");
        issue.setProject(project);
        issue.setStatus(IssueStatus.TODO);
        issue.setPriority(IssuePriority.MEDIUM);
        issue.setAssignedTo(assignee);
    }

    @Test
    @DisplayName("Issue assignment on update creates ISSUE_ASSIGNED notification")
    void testIssueAssignment_CreatesNotification() {
        User newAssignee = new User();
        newAssignee.setId(UUID.randomUUID());
        newAssignee.setName("New Assignee");

        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(userRepository.findById(newAssignee.getId())).thenReturn(Optional.of(newAssignee));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);
        when(securityUtils.isAuthenticated()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(actor.getId());
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setAssignedToId(newAssignee.getId());

        issueService.updateIssue(issue.getId(), request);

        verify(notificationService).createNotification(
                eq(newAssignee),
                eq(actor),
                eq(NotificationType.ISSUE_ASSIGNED),
                eq("You were assigned an issue"),
                any(),
                eq(NotificationReferenceType.ISSUE),
                eq(issue.getId())
        );
    }

    @Test
    @DisplayName("Unchanged issue assignment creates NO assignment notification")
    void testUnchangedAssignment_CreatesNoNotification() {
        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);
        when(securityUtils.isAuthenticated()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(actor.getId());

        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setAssignedToId(assignee.getId()); // Same assignee

        issueService.updateIssue(issue.getId(), request);

        verify(notificationService, never()).createNotification(
                any(), any(), eq(NotificationType.ISSUE_ASSIGNED), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("Issue status change creates ISSUE_STATUS_CHANGED notification for assignee")
    void testStatusChange_CreatesNotification() {
        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> {
            issue.setStatus(IssueStatus.IN_PROGRESS);
            return issue;
        });
        when(securityUtils.isAuthenticated()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(actor.getId());
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setStatus(IssueStatus.IN_PROGRESS);

        issueService.updateIssue(issue.getId(), request);

        verify(notificationService).createNotification(
                eq(assignee),
                eq(actor),
                eq(NotificationType.ISSUE_STATUS_CHANGED),
                eq("Issue status updated"),
                argThat(msg -> msg.contains("IN_PROGRESS")),
                eq(NotificationReferenceType.ISSUE),
                eq(issue.getId())
        );
    }

    @Test
    @DisplayName("Issue priority change creates ISSUE_PRIORITY_CHANGED notification for assignee")
    void testPriorityChange_CreatesNotification() {
        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> {
            issue.setPriority(IssuePriority.CRITICAL);
            return issue;
        });
        when(securityUtils.isAuthenticated()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(actor.getId());
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setPriority(IssuePriority.CRITICAL);

        issueService.updateIssue(issue.getId(), request);

        verify(notificationService).createNotification(
                eq(assignee),
                eq(actor),
                eq(NotificationType.ISSUE_PRIORITY_CHANGED),
                eq("Issue priority changed"),
                argThat(msg -> msg.contains("CRITICAL")),
                eq(NotificationReferenceType.ISSUE),
                eq(issue.getId())
        );
    }

    @Test
    @DisplayName("Adding a comment notifies the issue assignee")
    void testCommentAdded_NotifiesAssignee() {
        when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
        when(securityUtils.getCurrentUserId()).thenReturn(actor.getId());
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        Comment comment = new Comment();
        comment.setIssue(issue);
        comment.setUser(actor);
        comment.setContent("Please review the query plan");
        when(commentMapper.toEntity(any())).thenReturn(comment);
        when(commentRepository.save(any())).thenReturn(comment);

        CommentCreateRequest request = new CommentCreateRequest();
        request.setIssueId(issue.getId());
        request.setContent("Please review the query plan");

        commentService.createComment(request);

        verify(notificationService).createNotification(
                eq(assignee),
                eq(actor),
                eq(NotificationType.COMMENT_ADDED),
                eq("New comment on issue"),
                any(),
                eq(NotificationReferenceType.ISSUE),
                eq(issue.getId())
        );
    }

    @Test
    @DisplayName("Adding a project member creates PROJECT_MEMBER_ADDED notification")
    void testProjectMemberAdded_CreatesNotification() {
        User newMember = new User();
        newMember.setId(UUID.randomUUID());
        newMember.setName("New Team Member");

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findById(newMember.getId())).thenReturn(Optional.of(newMember));
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), newMember.getId())).thenReturn(false);
        when(securityUtils.isAuthenticated()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(actor.getId());
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(newMember);
        when(projectMemberMapper.toEntity(any())).thenReturn(member);
        when(projectMemberRepository.save(any())).thenReturn(member);

        ProjectMemberCreateRequest request = new ProjectMemberCreateRequest();
        request.setProjectId(project.getId());
        request.setUserId(newMember.getId());

        projectMemberService.addMember(request);

        verify(notificationService).createNotification(
                eq(newMember),
                eq(actor),
                eq(NotificationType.PROJECT_MEMBER_ADDED),
                eq("Added to project"),
                argThat(msg -> msg.contains(project.getName())),
                eq(NotificationReferenceType.PROJECT),
                eq(project.getId())
        );
    }

    @Test
    @DisplayName("Sprint status change notifies project participants")
    void testSprintStatusChange_NotifiesParticipants() {
        Sprint sprint = new Sprint();
        sprint.setId(UUID.randomUUID());
        sprint.setName("Sprint 1");
        sprint.setStatus(SprintStatus.PLANNED);
        sprint.setStartDate(Instant.now());
        sprint.setEndDate(Instant.now().plus(14, java.time.temporal.ChronoUnit.DAYS));
        sprint.setProject(project);

        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(any())).thenAnswer(invocation -> {
            sprint.setStatus(SprintStatus.ACTIVE);
            return sprint;
        });

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(assignee);
        when(projectMemberRepository.findByProjectId(project.getId())).thenReturn(List.of(member));

        when(securityUtils.isAuthenticated()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(actor.getId());
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        SprintUpdateRequest request = new SprintUpdateRequest();
        request.setStatus(SprintStatus.ACTIVE);

        sprintService.updateSprint(sprint.getId(), request);

        verify(notificationService).createNotifications(
                argThat((Collection<User> users) -> users.contains(assignee) && users.contains(actor)),
                eq(actor),
                eq(NotificationType.SPRINT_STATUS_CHANGED),
                eq("Sprint status updated"),
                argThat(msg -> msg.contains("ACTIVE")),
                eq(NotificationReferenceType.SPRINT),
                eq(sprint.getId())
        );
    }
}
