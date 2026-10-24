package com.devflow.service;

import com.devflow.dto.request.SprintCreateRequest;
import com.devflow.dto.request.SprintUpdateRequest;
import com.devflow.dto.response.SprintResponse;
import com.devflow.entity.Project;
import com.devflow.entity.Sprint;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.SprintMapper;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.security.AuthorizationService;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SprintMapper sprintMapper;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private SprintService sprintService;

    @Test
    @DisplayName("createSprint - success")
    void testCreateSprint_Success() {
        UUID projectId = UUID.randomUUID();
        SprintCreateRequest request = new SprintCreateRequest();
        request.setProjectId(projectId);
        request.setName("Sprint 1");
        Instant start = Instant.now();
        Instant end = start.plusSeconds(86400 * 14);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setStatus(SprintStatus.PLANNED);

        Project project = new Project();
        project.setId(projectId);

        Sprint sprint = new Sprint();
        Sprint savedSprint = new Sprint();
        savedSprint.setId(UUID.randomUUID());

        SprintResponse response = new SprintResponse();
        response.setId(savedSprint.getId());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(sprintMapper.toEntity(request)).thenReturn(sprint);
        when(sprintRepository.save(sprint)).thenReturn(savedSprint);
        when(sprintMapper.toResponse(savedSprint)).thenReturn(response);

        SprintResponse result = sprintService.createSprint(request);

        assertNotNull(result);
        assertEquals(response.getId(), result.getId());
        verify(authorizationService).requireProjectManagerOrAdmin(project);
        verify(sprintRepository).save(sprint);
    }

    @Test
    @DisplayName("createSprint - invalid date range throws BusinessRuleException")
    void testCreateSprint_InvalidDates() {
        UUID projectId = UUID.randomUUID();
        SprintCreateRequest request = new SprintCreateRequest();
        request.setProjectId(projectId);
        Instant start = Instant.now();
        Instant end = start.minusSeconds(86400);
        request.setStartDate(start);
        request.setEndDate(end);

        Project project = new Project();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(BusinessRuleException.class, () -> sprintService.createSprint(request));
        verify(authorizationService).requireProjectManagerOrAdmin(project);
    }

    @Test
    @DisplayName("createSprint - missing project throws ResourceNotFoundException")
    void testCreateSprint_MissingProject() {
        UUID projectId = UUID.randomUUID();
        SprintCreateRequest request = new SprintCreateRequest();
        request.setProjectId(projectId);
        request.setStartDate(Instant.now());
        request.setEndDate(Instant.now().plusSeconds(3600));

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sprintService.createSprint(request));
    }

    @Test
    @DisplayName("updateSprint - missing sprint throws ResourceNotFoundException")
    void testUpdateSprint_NotFound() {
        UUID id = UUID.randomUUID();
        SprintUpdateRequest request = new SprintUpdateRequest();

        when(sprintRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sprintService.updateSprint(id, request));
    }

    @Test
    @DisplayName("updateSprint - invalid date range throws BusinessRuleException")
    void testUpdateSprint_InvalidDates() {
        UUID id = UUID.randomUUID();
        Sprint sprint = new Sprint();
        sprint.setId(id);
        sprint.setProject(new Project());
        sprint.setStartDate(Instant.now());
        sprint.setEndDate(Instant.now().plusSeconds(86400));

        SprintUpdateRequest request = new SprintUpdateRequest();
        request.setStartDate(Instant.now().plusSeconds(86400 * 10)); // after existing end date

        when(sprintRepository.findById(id)).thenReturn(Optional.of(sprint));

        assertThrows(BusinessRuleException.class, () -> sprintService.updateSprint(id, request));
    }

    @Test
    @DisplayName("deleteSprint - success")
    void testDeleteSprint_Success() {
        UUID id = UUID.randomUUID();
        Sprint sprint = new Sprint();
        sprint.setId(id);
        sprint.setProject(new Project());

        when(sprintRepository.findById(id)).thenReturn(Optional.of(sprint));

        sprintService.deleteSprint(id);

        verify(authorizationService).requireProjectManagerOrAdmin(sprint.getProject());
        verify(sprintRepository).delete(sprint);
    }

    @Test
    @DisplayName("deleteSprint - not found throws ResourceNotFoundException")
    void testDeleteSprint_NotFound() {
        UUID id = UUID.randomUUID();
        when(sprintRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sprintService.deleteSprint(id));
    }
}
