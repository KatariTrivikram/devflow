package com.devflow.integration;

import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.Sprint;
import com.devflow.entity.User;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.repository.UserRepository;
import com.devflow.specification.IssueSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Issue Dynamic Specification Integration Test with Real PostgreSQL")
class IssueSpecificationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private SprintRepository sprintRepository;

    private User admin;
    private User pmCreator;
    private User devMember;
    private User devOther;

    private Project projectA;
    private Project projectB;

    private Sprint sprintA;

    private Issue issue1;
    private Issue issue2;
    private Issue issue3;
    private Issue issue4;

    @BeforeEach
    void setUpData() {
        admin = createUser("Admin User", "admin_spec@example.com", UserRole.ADMIN);
        pmCreator = createUser("PM Creator", "pm_creator@example.com", UserRole.PROJECT_MANAGER);
        devMember = createUser("Dev Member", "dev_member@example.com", UserRole.DEVELOPER);
        devOther = createUser("Dev Other", "dev_other@example.com", UserRole.DEVELOPER);

        projectA = createProject("DevFlow Core Platform", pmCreator);
        projectB = createProject("Internal Analytics", admin);

        // devMember is member of projectA only
        createProjectMember(projectA, devMember);

        sprintA = createSprint("Sprint 101", projectA);

        // Seed issues
        // Issue 1: Project A, Sprint A, Assignee: devMember, BUG, HIGH, TODO
        issue1 = createIssue("Fix login timeout exception",
                "Users report OAuth token refresh times out intermittently",
                projectA, sprintA, devMember, IssueType.BUG, IssuePriority.HIGH, IssueStatus.TODO);

        // Issue 2: Project A, Sprint A, Assignee: devMember, FEATURE, MEDIUM, IN_PROGRESS
        issue2 = createIssue("Implement audit logging export",
                "Export activity logs as CSV for compliance audits",
                projectA, sprintA, devMember, IssueType.FEATURE, IssuePriority.MEDIUM, IssueStatus.IN_PROGRESS);

        // Issue 3: Project A, No sprint, No assignee, TASK, LOW, DONE
        issue3 = createIssue("Update README documentation",
                "Refresh installation and configuration guides for local setup",
                projectA, null, null, IssueType.TASK, IssuePriority.LOW, IssueStatus.DONE);

        // Issue 4: Project B (Admin project), No sprint, Assignee: devOther, BUG, CRITICAL, TODO
        issue4 = createIssue("Data pipeline crash on high volume",
                "Kafka consumer terminates unexpectedly on memory spike",
                projectB, null, devOther, IssueType.BUG, IssuePriority.CRITICAL, IssueStatus.TODO);
    }

    private User createUser(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hashedPass123!");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private Project createProject(String name, User creator) {
        Project project = new Project();
        project.setName(name);
        project.setDescription("Project description for " + name);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(creator);
        return projectRepository.saveAndFlush(project);
    }

    private ProjectMember createProjectMember(Project project, User user) {
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        return projectMemberRepository.saveAndFlush(member);
    }

    private Sprint createSprint(String name, Project project) {
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName(name);
        sprint.setStartDate(Instant.now());
        sprint.setEndDate(Instant.now().plus(14, ChronoUnit.DAYS));
        sprint.setStatus(SprintStatus.ACTIVE);
        return sprintRepository.saveAndFlush(sprint);
    }

    private Issue createIssue(String title, String description, Project project, Sprint sprint,
                              User assignee, IssueType type, IssuePriority priority, IssueStatus status) {
        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setProject(project);
        issue.setSprint(sprint);
        issue.setAssignedTo(assignee);
        issue.setType(type);
        issue.setPriority(priority);
        issue.setStatus(status);
        return issueRepository.saveAndFlush(issue);
    }

    @Test
    @DisplayName("should filter issues by projectId specification")
    void shouldFilterByProjectId() {
        Specification<Issue> spec = IssueSpecification.hasProjectId(projectA.getId());
        List<Issue> results = issueRepository.findAll(spec);

        assertThat(results).hasSize(3);
        assertThat(results).extracting(Issue::getId)
                .containsExactlyInAnyOrder(issue1.getId(), issue2.getId(), issue3.getId());
    }

    @Test
    @DisplayName("should filter issues by status and priority specifications")
    void shouldFilterByStatusAndPriority() {
        Specification<Issue> spec = Specification
                .where(IssueSpecification.hasStatus(IssueStatus.TODO))
                .and(IssueSpecification.hasPriority(IssuePriority.HIGH));

        List<Issue> results = issueRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(issue1.getId());
    }

    @Test
    @DisplayName("should combine project, sprint, assignee, and type specifications with pagination")
    void shouldCombineMultipleSpecificationsWithPagination() {
        Specification<Issue> spec = Specification
                .where(IssueSpecification.hasProjectId(projectA.getId()))
                .and(IssueSpecification.hasSprintId(sprintA.getId()))
                .and(IssueSpecification.hasAssignedToId(devMember.getId()))
                .and(IssueSpecification.hasType(IssueType.FEATURE));

        Page<Issue> page = issueRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(issue2.getId());
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Implement audit logging export");
    }

    @Test
    @DisplayName("should execute case-insensitive search across title and description in PostgreSQL")
    void shouldSearchTitleAndDescriptionCaseInsensitively() {
        // Search matching title: "login"
        Specification<Issue> titleSearchSpec = IssueSpecification.search("LOGIN");
        List<Issue> titleResults = issueRepository.findAll(titleSearchSpec);
        assertThat(titleResults).extracting(Issue::getId).containsExactly(issue1.getId());

        // Search matching description: "kafka"
        Specification<Issue> descSearchSpec = IssueSpecification.search("Kafka");
        List<Issue> descResults = issueRepository.findAll(descSearchSpec);
        assertThat(descResults).extracting(Issue::getId).containsExactly(issue4.getId());

        // Search with whitespace: "  OAuth  "
        Specification<Issue> trimmedSearchSpec = IssueSpecification.search("  OAuth  ");
        List<Issue> trimmedResults = issueRepository.findAll(trimmedSearchSpec);
        assertThat(trimmedResults).extracting(Issue::getId).containsExactly(issue1.getId());

        // Search non-matching term
        Specification<Issue> noMatchSpec = IssueSpecification.search("nonexistentterm");
        List<Issue> noMatchResults = issueRepository.findAll(noMatchSpec);
        assertThat(noMatchResults).isEmpty();
    }

    @Test
    @DisplayName("should enforce role-based access control subquery in PostgreSQL")
    void shouldEnforceRoleBasedAccessControlSubquery() {
        // 1. ADMIN should see all issues across all projects
        Specification<Issue> adminSpec = IssueSpecification.isAccessibleByUser(admin.getId(), UserRole.ADMIN);
        List<Issue> adminResults = issueRepository.findAll(adminSpec);
        assertThat(adminResults).hasSize(4);

        // 2. PROJECT_MANAGER who created projectA should see projectA's issues (3 issues), but not projectB
        Specification<Issue> pmSpec = IssueSpecification.isAccessibleByUser(pmCreator.getId(), UserRole.PROJECT_MANAGER);
        List<Issue> pmResults = issueRepository.findAll(pmSpec);
        assertThat(pmResults).hasSize(3);
        assertThat(pmResults).extracting(Issue::getId)
                .containsExactlyInAnyOrder(issue1.getId(), issue2.getId(), issue3.getId());

        // 3. DEVELOPER who is a member of projectA should see projectA's issues (3 issues), but not projectB
        Specification<Issue> devMemberSpec = IssueSpecification.isAccessibleByUser(devMember.getId(), UserRole.DEVELOPER);
        List<Issue> devMemberResults = issueRepository.findAll(devMemberSpec);
        assertThat(devMemberResults).hasSize(3);
        assertThat(devMemberResults).extracting(Issue::getId)
                .containsExactlyInAnyOrder(issue1.getId(), issue2.getId(), issue3.getId());

        // 4. DEVELOPER who is NOT a member of any project should see 0 issues
        User outsiderDev = createUser("Outsider", "outsider@example.com", UserRole.DEVELOPER);
        Specification<Issue> outsiderSpec = IssueSpecification.isAccessibleByUser(outsiderDev.getId(), UserRole.DEVELOPER);
        List<Issue> outsiderResults = issueRepository.findAll(outsiderSpec);
        assertThat(outsiderResults).isEmpty();
    }
}
