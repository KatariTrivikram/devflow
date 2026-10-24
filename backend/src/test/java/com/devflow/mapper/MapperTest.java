package com.devflow.mapper;

import com.devflow.dto.request.CommentCreateRequest;
import com.devflow.dto.request.CommentUpdateRequest;
import com.devflow.dto.request.IssueCreateRequest;
import com.devflow.dto.request.IssueUpdateRequest;
import com.devflow.dto.request.ProjectCreateRequest;
import com.devflow.dto.request.ProjectMemberCreateRequest;
import com.devflow.dto.request.ProjectUpdateRequest;
import com.devflow.dto.request.SprintCreateRequest;
import com.devflow.dto.request.SprintUpdateRequest;
import com.devflow.dto.request.UserCreateRequest;
import com.devflow.dto.request.UserUpdateRequest;
import com.devflow.dto.response.ActivityResponse;
import com.devflow.dto.response.CommentResponse;
import com.devflow.dto.response.IssueResponse;
import com.devflow.dto.response.ProjectMemberResponse;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.dto.response.SprintResponse;
import com.devflow.dto.response.UserResponse;
import com.devflow.entity.Activity;
import com.devflow.entity.Comment;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.Sprint;
import com.devflow.entity.User;
import com.devflow.entity.enums.ActivityAction;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapperTest {

    private UserMapper userMapper;
    private ProjectMapper projectMapper;
    private ProjectMemberMapper projectMemberMapper;
    private SprintMapper sprintMapper;
    private IssueMapper issueMapper;
    private CommentMapper commentMapper;
    private ActivityMapper activityMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        projectMapper = new ProjectMapper();
        projectMemberMapper = new ProjectMemberMapper();
        sprintMapper = new SprintMapper();
        issueMapper = new IssueMapper();
        commentMapper = new CommentMapper();
        activityMapper = new ActivityMapper();
    }

    @Test
    @DisplayName("UserMapper - toResponse and toEntity")
    void testUserMapper() {
        assertNull(userMapper.toResponse(null));
        assertNull(userMapper.toEntity(null));

        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword("secret123");
        user.setRole(UserRole.DEVELOPER);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserResponse response = userMapper.toResponse(user);
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("Alice", response.getName());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals(UserRole.DEVELOPER, response.getRole());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());

        UserCreateRequest createRequest = new UserCreateRequest();
        createRequest.setName("Bob");
        createRequest.setEmail("bob@example.com");
        createRequest.setPassword("pass12345");
        createRequest.setRole(UserRole.PROJECT_MANAGER);

        User created = userMapper.toEntity(createRequest);
        assertNotNull(created);
        assertEquals("Bob", created.getName());
        assertEquals("bob@example.com", created.getEmail());
        assertEquals("pass12345", created.getPassword());
        assertEquals(UserRole.PROJECT_MANAGER, created.getRole());

        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setName("Bob Updated");
        updateRequest.setRole(UserRole.ADMIN);
        userMapper.updateEntity(updateRequest, created);
        assertEquals("Bob Updated", created.getName());
        assertEquals(UserRole.ADMIN, created.getRole());
    }

    @Test
    @DisplayName("ProjectMapper - toResponse, toEntity, and updateEntity")
    void testProjectMapper() {
        assertNull(projectMapper.toResponse(null));
        assertNull(projectMapper.toEntity(null));

        User creator = new User();
        UUID creatorId = UUID.randomUUID();
        creator.setId(creatorId);

        Project project = new Project();
        UUID projectId = UUID.randomUUID();
        project.setId(projectId);
        project.setName("DevFlow Core");
        project.setDescription("Core platform");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(creator);
        Instant now = Instant.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        ProjectResponse response = projectMapper.toResponse(project);
        assertNotNull(response);
        assertEquals(projectId, response.getId());
        assertEquals("DevFlow Core", response.getName());
        assertEquals("Core platform", response.getDescription());
        assertEquals(ProjectStatus.ACTIVE, response.getStatus());
        assertEquals(creatorId, response.getCreatedById());
        assertEquals(now, response.getCreatedAt());

        ProjectCreateRequest createRequest = new ProjectCreateRequest();
        createRequest.setName("DevFlow Next");
        createRequest.setDescription("Next gen");
        createRequest.setStatus(ProjectStatus.ACTIVE);
        createRequest.setCreatedById(creatorId);

        Project created = projectMapper.toEntity(createRequest);
        assertNotNull(created);
        assertEquals("DevFlow Next", created.getName());
        assertEquals("Next gen", created.getDescription());
        assertEquals(ProjectStatus.ACTIVE, created.getStatus());

        ProjectUpdateRequest updateRequest = new ProjectUpdateRequest();
        updateRequest.setName("DevFlow Completed");
        updateRequest.setStatus(ProjectStatus.COMPLETED);
        projectMapper.updateEntity(updateRequest, created);
        assertEquals("DevFlow Completed", created.getName());
        assertEquals(ProjectStatus.COMPLETED, created.getStatus());
    }

    @Test
    @DisplayName("ProjectMemberMapper - toResponse and toEntity")
    void testProjectMemberMapper() {
        assertNull(projectMemberMapper.toResponse(null));
        assertNull(projectMemberMapper.toEntity(null));

        Project project = new Project();
        UUID projectId = UUID.randomUUID();
        project.setId(projectId);

        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);

        ProjectMember member = new ProjectMember();
        UUID memberId = UUID.randomUUID();
        member.setId(memberId);
        member.setProject(project);
        member.setUser(user);
        Instant now = Instant.now();
        member.setJoinedAt(now);

        ProjectMemberResponse response = projectMemberMapper.toResponse(member);
        assertNotNull(response);
        assertEquals(memberId, response.getId());
        assertEquals(projectId, response.getProjectId());
        assertEquals(userId, response.getUserId());
        assertEquals(now, response.getJoinedAt());

        ProjectMemberCreateRequest createRequest = new ProjectMemberCreateRequest();
        createRequest.setProjectId(projectId);
        createRequest.setUserId(userId);

        ProjectMember created = projectMemberMapper.toEntity(createRequest);
        assertNotNull(created);
    }

    @Test
    @DisplayName("SprintMapper - toResponse, toEntity, and updateEntity")
    void testSprintMapper() {
        assertNull(sprintMapper.toResponse(null));
        assertNull(sprintMapper.toEntity(null));

        Project project = new Project();
        UUID projectId = UUID.randomUUID();
        project.setId(projectId);

        Sprint sprint = new Sprint();
        UUID sprintId = UUID.randomUUID();
        sprint.setId(sprintId);
        sprint.setProject(project);
        sprint.setName("Sprint 1");
        Instant start = Instant.now();
        Instant end = start.plusSeconds(86400 * 14);
        sprint.setStartDate(start);
        sprint.setEndDate(end);
        sprint.setStatus(SprintStatus.PLANNED);
        sprint.setCreatedAt(start);
        sprint.setUpdatedAt(start);

        SprintResponse response = sprintMapper.toResponse(sprint);
        assertNotNull(response);
        assertEquals(sprintId, response.getId());
        assertEquals(projectId, response.getProjectId());
        assertEquals("Sprint 1", response.getName());
        assertEquals(start, response.getStartDate());
        assertEquals(end, response.getEndDate());
        assertEquals(SprintStatus.PLANNED, response.getStatus());

        SprintCreateRequest createRequest = new SprintCreateRequest();
        createRequest.setProjectId(projectId);
        createRequest.setName("Sprint 2");
        createRequest.setStartDate(start);
        createRequest.setEndDate(end);
        createRequest.setStatus(SprintStatus.ACTIVE);

        Sprint created = sprintMapper.toEntity(createRequest);
        assertNotNull(created);
        assertEquals("Sprint 2", created.getName());
        assertEquals(SprintStatus.ACTIVE, created.getStatus());

        SprintUpdateRequest updateRequest = new SprintUpdateRequest();
        updateRequest.setName("Sprint 2 Extended");
        updateRequest.setStatus(SprintStatus.COMPLETED);
        sprintMapper.updateEntity(updateRequest, created);
        assertEquals("Sprint 2 Extended", created.getName());
        assertEquals(SprintStatus.COMPLETED, created.getStatus());
    }

    @Test
    @DisplayName("IssueMapper - toResponse, toEntity, and updateEntity")
    void testIssueMapper() {
        assertNull(issueMapper.toResponse(null));
        assertNull(issueMapper.toEntity(null));

        Project project = new Project();
        UUID projectId = UUID.randomUUID();
        project.setId(projectId);

        Sprint sprint = new Sprint();
        UUID sprintId = UUID.randomUUID();
        sprint.setId(sprintId);

        User assignee = new User();
        UUID userId = UUID.randomUUID();
        assignee.setId(userId);

        Issue issue = new Issue();
        UUID issueId = UUID.randomUUID();
        issue.setId(issueId);
        issue.setProject(project);
        issue.setSprint(sprint);
        issue.setAssignedTo(assignee);
        issue.setTitle("Fix login bug");
        issue.setDescription("JWT validation edge case");
        issue.setType(IssueType.BUG);
        issue.setPriority(IssuePriority.HIGH);
        issue.setStatus(IssueStatus.IN_PROGRESS);
        Instant now = Instant.now();
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);

        IssueResponse response = issueMapper.toResponse(issue);
        assertNotNull(response);
        assertEquals(issueId, response.getId());
        assertEquals(projectId, response.getProjectId());
        assertEquals(sprintId, response.getSprintId());
        assertEquals(userId, response.getAssignedToId());
        assertEquals("Fix login bug", response.getTitle());
        assertEquals(IssueType.BUG, response.getType());
        assertEquals(IssuePriority.HIGH, response.getPriority());
        assertEquals(IssueStatus.IN_PROGRESS, response.getStatus());

        IssueCreateRequest createRequest = new IssueCreateRequest();
        createRequest.setProjectId(projectId);
        createRequest.setTitle("Add OAuth");
        createRequest.setDescription("Google login");
        createRequest.setType(IssueType.FEATURE);
        createRequest.setPriority(IssuePriority.CRITICAL);
        createRequest.setStatus(IssueStatus.TODO);

        Issue created = issueMapper.toEntity(createRequest);
        assertNotNull(created);
        assertEquals("Add OAuth", created.getTitle());
        assertEquals(IssueType.FEATURE, created.getType());

        IssueUpdateRequest updateRequest = new IssueUpdateRequest();
        updateRequest.setTitle("Add OAuth2 Support");
        updateRequest.setStatus(IssueStatus.CODE_REVIEW);
        issueMapper.updateEntity(updateRequest, created);
        assertEquals("Add OAuth2 Support", created.getTitle());
        assertEquals(IssueStatus.CODE_REVIEW, created.getStatus());
    }

    @Test
    @DisplayName("CommentMapper - toResponse, toEntity, and updateEntity")
    void testCommentMapper() {
        assertNull(commentMapper.toResponse(null));
        assertNull(commentMapper.toEntity(null));

        Issue issue = new Issue();
        UUID issueId = UUID.randomUUID();
        issue.setId(issueId);

        User author = new User();
        UUID authorId = UUID.randomUUID();
        author.setId(authorId);

        Comment comment = new Comment();
        UUID commentId = UUID.randomUUID();
        comment.setId(commentId);
        comment.setIssue(issue);
        comment.setUser(author);
        comment.setContent("Looking into this now.");
        Instant now = Instant.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        CommentResponse response = commentMapper.toResponse(comment);
        assertNotNull(response);
        assertEquals(commentId, response.getId());
        assertEquals(issueId, response.getIssueId());
        assertEquals(authorId, response.getUserId());
        assertEquals("Looking into this now.", response.getContent());

        CommentCreateRequest createRequest = new CommentCreateRequest();
        createRequest.setIssueId(issueId);
        createRequest.setUserId(authorId);
        createRequest.setContent("PR submitted.");

        Comment created = commentMapper.toEntity(createRequest);
        assertNotNull(created);
        assertEquals("PR submitted.", created.getContent());

        CommentUpdateRequest updateRequest = new CommentUpdateRequest();
        updateRequest.setContent("PR approved.");
        commentMapper.updateEntity(updateRequest, created);
        assertEquals("PR approved.", created.getContent());
    }

    @Test
    @DisplayName("ActivityMapper - toResponse")
    void testActivityMapper() {
        assertNull(activityMapper.toResponse(null));

        Issue issue = new Issue();
        UUID issueId = UUID.randomUUID();
        issue.setId(issueId);

        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);

        Activity activity = new Activity();
        UUID activityId = UUID.randomUUID();
        activity.setId(activityId);
        activity.setIssue(issue);
        activity.setUser(user);
        activity.setAction(ActivityAction.STATUS_CHANGED);
        activity.setOldValue("TODO");
        activity.setNewValue("IN_PROGRESS");
        Instant now = Instant.now();
        activity.setCreatedAt(now);

        ActivityResponse response = activityMapper.toResponse(activity);
        assertNotNull(response);
        assertEquals(activityId, response.getId());
        assertEquals(issueId, response.getIssueId());
        assertEquals(userId, response.getUserId());
        assertEquals(ActivityAction.STATUS_CHANGED, response.getAction());
        assertEquals("TODO", response.getOldValue());
        assertEquals("IN_PROGRESS", response.getNewValue());
        assertEquals(now, response.getCreatedAt());
    }
}
