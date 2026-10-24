package com.devflow.integration;

import com.devflow.entity.Activity;
import com.devflow.entity.Comment;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.Sprint;
import com.devflow.entity.User;
import com.devflow.entity.enums.ActivityAction;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import com.devflow.repository.ActivityRepository;
import com.devflow.repository.CommentRepository;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Issue, Comment, and Activity Repository Integration Test")
class IssueRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ActivityRepository activityRepository;

    private User createAndSaveUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hashedPassword123!");
        user.setRole(UserRole.DEVELOPER);
        return userRepository.saveAndFlush(user);
    }

    private Project createAndSaveProject(String name, User creator) {
        Project project = new Project();
        project.setName(name);
        project.setDescription("Project for issues");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(creator);
        return projectRepository.saveAndFlush(project);
    }

    private Sprint createAndSaveSprint(String name, Project project) {
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName(name);
        sprint.setStartDate(Instant.now());
        sprint.setEndDate(Instant.now().plus(14, ChronoUnit.DAYS));
        sprint.setStatus(SprintStatus.ACTIVE);
        return sprintRepository.saveAndFlush(sprint);
    }

    private Issue createAndSaveIssue(String title, Project project, Sprint sprint, User assignee,
                                     IssueType type, IssuePriority priority, IssueStatus status) {
        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setDescription("Detailed description for " + title);
        issue.setProject(project);
        issue.setSprint(sprint);
        issue.setAssignedTo(assignee);
        issue.setType(type);
        issue.setPriority(priority);
        issue.setStatus(status);
        return issueRepository.saveAndFlush(issue);
    }

    @Test
    @DisplayName("should save and retrieve issue with enum mappings and relational foreign keys")
    void shouldSaveAndRetrieveIssueWithRelationships() {
        User dev = createAndSaveUser("Dev User", "dev_issue@example.com");
        Project project = createAndSaveProject("Issue Tracking", dev);
        Sprint sprint = createAndSaveSprint("Sprint 1", project);

        Issue issue = createAndSaveIssue(
                "Fix memory leak in parser",
                project,
                sprint,
                dev,
                IssueType.BUG,
                IssuePriority.HIGH,
                IssueStatus.IN_PROGRESS
        );

        assertThat(issue.getId()).isNotNull();
        assertThat(issue.getCreatedAt()).isNotNull();
        assertThat(issue.getUpdatedAt()).isNotNull();
        assertThat(issue.getTitle()).isEqualTo("Fix memory leak in parser");
        assertThat(issue.getType()).isEqualTo(IssueType.BUG);
        assertThat(issue.getPriority()).isEqualTo(IssuePriority.HIGH);
        assertThat(issue.getStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
        assertThat(issue.getProject().getId()).isEqualTo(project.getId());
        assertThat(issue.getSprint().getId()).isEqualTo(sprint.getId());
        assertThat(issue.getAssignedTo().getId()).isEqualTo(dev.getId());
    }

    @Test
    @DisplayName("should paginate and filter issues by project, sprint, assignee, status, and priority")
    void shouldPaginateAndFilterIssues() {
        User dev1 = createAndSaveUser("Dev One", "dev1@example.com");
        User dev2 = createAndSaveUser("Dev Two", "dev2@example.com");
        Project project = createAndSaveProject("Core Platform", dev1);
        Sprint sprint = createAndSaveSprint("Sprint 42", project);

        createAndSaveIssue("Bug 1", project, sprint, dev1, IssueType.BUG, IssuePriority.CRITICAL, IssueStatus.TODO);
        createAndSaveIssue("Feature 1", project, sprint, dev1, IssueType.FEATURE, IssuePriority.MEDIUM, IssueStatus.IN_PROGRESS);
        createAndSaveIssue("Bug 2", project, sprint, dev2, IssueType.BUG, IssuePriority.HIGH, IssueStatus.DONE);

        Page<Issue> byProject = issueRepository.findByProjectId(project.getId(), PageRequest.of(0, 2));
        assertThat(byProject.getTotalElements()).isEqualTo(3);
        assertThat(byProject.getContent()).hasSize(2);
        assertThat(byProject.getTotalPages()).isEqualTo(2);

        Page<Issue> bySprint = issueRepository.findBySprintId(sprint.getId(), PageRequest.of(0, 10));
        assertThat(bySprint.getTotalElements()).isEqualTo(3);

        Page<Issue> byAssignee = issueRepository.findByAssignedToId(dev1.getId(), PageRequest.of(0, 10));
        assertThat(byAssignee.getTotalElements()).isEqualTo(2);

        Page<Issue> byStatus = issueRepository.findByStatus(IssueStatus.TODO, PageRequest.of(0, 10));
        assertThat(byStatus.getContent()).extracting(Issue::getTitle).contains("Bug 1");

        Page<Issue> byPriority = issueRepository.findByPriority(IssuePriority.CRITICAL, PageRequest.of(0, 10));
        assertThat(byPriority.getContent()).extracting(Issue::getTitle).contains("Bug 1");

        Page<Issue> byProjectAndStatus = issueRepository.findByProjectIdAndStatus(project.getId(), IssueStatus.DONE, PageRequest.of(0, 10));
        assertThat(byProjectAndStatus.getContent()).extracting(Issue::getTitle).containsExactly("Bug 2");

        Page<Issue> byProjectAndAssignee = issueRepository.findByProjectIdAndAssignedToId(project.getId(), dev2.getId(), PageRequest.of(0, 10));
        assertThat(byProjectAndAssignee.getContent()).extracting(Issue::getTitle).containsExactly("Bug 2");
    }

    @Test
    @DisplayName("should save comments and retrieve them in chronological order (createdAt ASC)")
    void shouldSaveAndRetrieveCommentsInChronologicalOrder() throws InterruptedException {
        User user = createAndSaveUser("Commenter", "commenter@example.com");
        Project project = createAndSaveProject("Comment Project", user);
        Issue issue = createAndSaveIssue("Comment Issue", project, null, user,
                IssueType.TASK, IssuePriority.LOW, IssueStatus.TODO);

        Comment comment1 = new Comment();
        comment1.setIssue(issue);
        comment1.setUser(user);
        comment1.setContent("Initial investigation completed.");
        commentRepository.saveAndFlush(comment1);

        // Small pause to guarantee distinct timestamp ordering
        Thread.sleep(50);

        Comment comment2 = new Comment();
        comment2.setIssue(issue);
        comment2.setUser(user);
        comment2.setContent("Follow-up: fix is ready for review.");
        commentRepository.saveAndFlush(comment2);

        Page<Comment> commentsPage = commentRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId(), PageRequest.of(0, 10));
        List<Comment> comments = commentsPage.getContent();

        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getContent()).isEqualTo("Initial investigation completed.");
        assertThat(comments.get(1).getContent()).isEqualTo("Follow-up: fix is ready for review.");
        assertThat(comments.get(0).getCreatedAt()).isBeforeOrEqualTo(comments.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("should save activities and retrieve them in reverse chronological order (createdAt DESC)")
    void shouldSaveAndRetrieveActivitiesInReverseChronologicalOrder() throws InterruptedException {
        User user = createAndSaveUser("Auditor", "auditor@example.com");
        Project project = createAndSaveProject("Audit Project", user);
        Issue issue = createAndSaveIssue("Audit Issue", project, null, user,
                IssueType.TASK, IssuePriority.MEDIUM, IssueStatus.TODO);

        Activity activity1 = new Activity();
        activity1.setIssue(issue);
        activity1.setUser(user);
        activity1.setAction(ActivityAction.STATUS_CHANGED);
        activity1.setOldValue("TODO");
        activity1.setNewValue("IN_PROGRESS");
        activityRepository.saveAndFlush(activity1);

        // Small pause to guarantee distinct timestamp ordering
        Thread.sleep(50);

        Activity activity2 = new Activity();
        activity2.setIssue(issue);
        activity2.setUser(user);
        activity2.setAction(ActivityAction.PRIORITY_CHANGED);
        activity2.setOldValue("MEDIUM");
        activity2.setNewValue("HIGH");
        activityRepository.saveAndFlush(activity2);

        Page<Activity> activitiesPage = activityRepository.findByIssueIdOrderByCreatedAtDesc(issue.getId(), PageRequest.of(0, 10));
        List<Activity> activities = activitiesPage.getContent();

        assertThat(activities).hasSize(2);
        // Latest action should come first
        assertThat(activities.get(0).getAction()).isEqualTo(ActivityAction.PRIORITY_CHANGED);
        assertThat(activities.get(1).getAction()).isEqualTo(ActivityAction.STATUS_CHANGED);
        assertThat(activities.get(0).getCreatedAt()).isAfterOrEqualTo(activities.get(1).getCreatedAt());
    }
}
