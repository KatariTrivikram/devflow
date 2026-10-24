package com.devflow.integration;

import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.Sprint;
import com.devflow.entity.User;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Project, Member, and Sprint Repository Integration Test")
class ProjectRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private SprintRepository sprintRepository;

    private User createAndSaveUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("passwordHash123!");
        user.setRole(UserRole.PROJECT_MANAGER);
        return userRepository.saveAndFlush(user);
    }

    private Project createAndSaveProject(String name, User creator) {
        Project project = new Project();
        project.setName(name);
        project.setDescription("Project description for " + name);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(creator);
        return projectRepository.saveAndFlush(project);
    }

    @Test
    @DisplayName("should save, retrieve and update project with creator relationship")
    void shouldSaveRetrieveAndUpdateProject() {
        User creator = createAndSaveUser("Project Creator", "creator@example.com");
        Project project = createAndSaveProject("DevFlow Core", creator);

        assertThat(project.getId()).isNotNull();
        assertThat(project.getCreatedAt()).isNotNull();
        assertThat(project.getUpdatedAt()).isNotNull();
        assertThat(project.getCreatedBy().getId()).isEqualTo(creator.getId());

        // Update project status
        project.setStatus(ProjectStatus.COMPLETED);
        Project updated = projectRepository.saveAndFlush(project);
        assertThat(updated.getStatus()).isEqualTo(ProjectStatus.COMPLETED);

        Optional<Project> found = projectRepository.findById(project.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("DevFlow Core");
    }

    @Test
    @DisplayName("should query projects by creator, status, and name search")
    void shouldQueryProjectsByCreatorStatusAndName() {
        User creator = createAndSaveUser("PM User", "pm@example.com");
        Project project1 = createAndSaveProject("Alpha Backend", creator);

        Project project2 = new Project();
        project2.setName("Beta Frontend");
        project2.setDescription("Frontend UI project");
        project2.setStatus(ProjectStatus.ARCHIVED);
        project2.setCreatedBy(creator);
        projectRepository.saveAndFlush(project2);

        List<Project> byCreator = projectRepository.findByCreatedById(creator.getId());
        assertThat(byCreator).hasSize(2);

        List<Project> activeProjects = projectRepository.findByStatus(ProjectStatus.ACTIVE);
        assertThat(activeProjects).extracting(Project::getName).contains("Alpha Backend");

        List<Project> searchResults = projectRepository.findByNameContainingIgnoreCase("alpha");
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getId()).isEqualTo(project1.getId());
    }

    @Test
    @DisplayName("should enforce unique constraint uk_project_user on project members")
    void shouldEnforceProjectMemberUniqueConstraint() {
        User creator = createAndSaveUser("Project Owner", "owner@example.com");
        User developer = createAndSaveUser("Dev Member", "dev@example.com");
        Project project = createAndSaveProject("Secret Project", creator);

        ProjectMember member1 = new ProjectMember();
        member1.setProject(project);
        member1.setUser(developer);
        projectMemberRepository.saveAndFlush(member1);

        assertThat(member1.getId()).isNotNull();
        assertThat(member1.getJoinedAt()).isNotNull();

        boolean exists = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), developer.getId());
        assertThat(exists).isTrue();

        // Attempt duplicate membership
        ProjectMember duplicateMember = new ProjectMember();
        duplicateMember.setProject(project);
        duplicateMember.setUser(developer);

        assertThrows(DataIntegrityViolationException.class, () -> {
            projectMemberRepository.saveAndFlush(duplicateMember);
        });
    }

    @Test
    @DisplayName("should enforce sprints_end_date_check constraint when end_date < start_date")
    void shouldEnforceSprintEndDateCheckConstraint() {
        User creator = createAndSaveUser("Sprint Master", "sm@example.com");
        Project project = createAndSaveProject("Sprint Project", creator);

        Instant now = Instant.now();
        Sprint invalidSprint = new Sprint();
        invalidSprint.setProject(project);
        invalidSprint.setName("Invalid Timeline Sprint");
        invalidSprint.setStartDate(now);
        invalidSprint.setEndDate(now.minus(2, ChronoUnit.DAYS)); // Invalid: end before start
        invalidSprint.setStatus(SprintStatus.PLANNED);

        assertThrows(DataIntegrityViolationException.class, () -> {
            sprintRepository.saveAndFlush(invalidSprint);
        });
    }

    @Test
    @DisplayName("should successfully save sprint when end_date >= start_date")
    void shouldSaveSprintWithValidDates() {
        User creator = createAndSaveUser("Sprint Planner", "planner@example.com");
        Project project = createAndSaveProject("Agile Dev", creator);

        Instant now = Instant.now();
        Sprint validSprint = new Sprint();
        validSprint.setProject(project);
        validSprint.setName("Sprint 1");
        validSprint.setStartDate(now);
        validSprint.setEndDate(now.plus(14, ChronoUnit.DAYS));
        validSprint.setStatus(SprintStatus.PLANNED);

        Sprint savedSprint = sprintRepository.saveAndFlush(validSprint);

        assertThat(savedSprint.getId()).isNotNull();
        assertThat(savedSprint.getCreatedAt()).isNotNull();
        assertThat(savedSprint.getName()).isEqualTo("Sprint 1");

        List<Sprint> projectSprints = sprintRepository.findByProjectId(project.getId());
        assertThat(projectSprints).hasSize(1);
    }
}
