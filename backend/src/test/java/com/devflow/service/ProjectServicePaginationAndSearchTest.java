package com.devflow.service;

import com.devflow.dto.response.PageResponse;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.dto.response.SprintResponse;
import com.devflow.entity.Project;
import com.devflow.entity.Sprint;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import com.devflow.mapper.ProjectMapper;
import com.devflow.mapper.SprintMapper;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.security.AuthorizationService;
import com.devflow.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServicePaginationAndSearchTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private SprintMapper sprintMapper;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ProjectService projectService;

    @Test
    @DisplayName("getProjects - status and search with pagination returns PageResponse")
    void testGetProjects_WithFilters() {
        UUID currentUserId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(securityUtils.getCurrentUserRole()).thenReturn(UserRole.PROJECT_MANAGER);

        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("DevFlow Cloud");

        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName("DevFlow Cloud");

        Page<Project> page = new PageImpl<>(List.of(project));
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(projectMapper.toResponse(project)).thenReturn(response);

        PageResponse<ProjectResponse> result = projectService.getProjects(
                ProjectStatus.ACTIVE, "Cloud", 0, 10, "name,asc"
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("DevFlow Cloud", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("getSprintsByProjectPaginated - unauthorized project throws AccessDeniedException")
    void testGetSprintsByProjectPaginated_Unauthorized() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);

        SprintService sprintService = new SprintService(sprintRepository, projectRepository, sprintMapper, authorizationService);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        doThrow(new AccessDeniedException("Access denied: You do not have access to this project"))
                .when(authorizationService).requireProjectAccess(project);

        assertThrows(AccessDeniedException.class, () ->
                sprintService.getSprintsByProjectPaginated(projectId, null, 0, 20, "startDate,asc")
        );
    }

    @Test
    @DisplayName("getSprintsByProjectPaginated - authorized project returns PageResponse")
    void testGetSprintsByProjectPaginated_Authorized() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);

        Sprint sprint = new Sprint();
        sprint.setId(UUID.randomUUID());
        sprint.setName("Sprint 1");

        SprintResponse response = new SprintResponse();
        response.setId(sprint.getId());
        response.setName("Sprint 1");

        SprintService sprintService = new SprintService(sprintRepository, projectRepository, sprintMapper, authorizationService);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        Page<Sprint> page = new PageImpl<>(List.of(sprint));
        when(sprintRepository.findByProjectIdAndStatus(eq(projectId), eq(SprintStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);
        when(sprintMapper.toResponse(sprint)).thenReturn(response);

        PageResponse<SprintResponse> result = sprintService.getSprintsByProjectPaginated(
                projectId, SprintStatus.ACTIVE, 0, 10, "startDate,asc"
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Sprint 1", result.getContent().get(0).getName());
        verify(authorizationService).requireProjectAccess(project);
    }
}
