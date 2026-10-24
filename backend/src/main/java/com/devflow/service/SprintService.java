package com.devflow.service;

import com.devflow.dto.request.SprintCreateRequest;
import com.devflow.dto.request.SprintUpdateRequest;
import com.devflow.dto.response.PageResponse;
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
import com.devflow.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devflow.entity.ProjectMember;
import com.devflow.entity.User;
import com.devflow.entity.enums.NotificationReferenceType;
import com.devflow.entity.enums.NotificationType;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.SecurityUtils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final SprintMapper sprintMapper;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public SprintService(SprintRepository sprintRepository,
                         ProjectRepository projectRepository,
                         SprintMapper sprintMapper,
                         AuthorizationService authorizationService) {
        this(sprintRepository, projectRepository, sprintMapper, authorizationService, null, null, null, null);
    }

    @Autowired
    public SprintService(SprintRepository sprintRepository,
                         ProjectRepository projectRepository,
                         SprintMapper sprintMapper,
                         AuthorizationService authorizationService,
                         NotificationService notificationService,
                         ProjectMemberRepository projectMemberRepository,
                         UserRepository userRepository,
                         SecurityUtils securityUtils) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.sprintMapper = sprintMapper;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public SprintResponse createSprint(SprintCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

        // Only ADMIN or the Project Manager managing this project can create sprints
        authorizationService.requireProjectManagerOrAdmin(project);

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessRuleException("Sprint end date cannot be before start date");
        }

        Sprint sprint = sprintMapper.toEntity(request);
        sprint.setProject(project);
        Sprint saved = sprintRepository.save(sprint);
        return sprintMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprintById(UUID id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + id));

        authorizationService.requireProjectAccess(sprint.getProject());

        return sprintMapper.toResponse(sprint);
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsByProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        authorizationService.requireProjectAccess(project);

        return sprintRepository.findByProjectId(projectId).stream()
                .map(sprintMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsByProjectAndStatus(UUID projectId, SprintStatus status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        authorizationService.requireProjectAccess(project);

        return sprintRepository.findByProjectIdAndStatus(projectId, status).stream()
                .map(sprintMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SprintResponse> getSprintsByProjectPaginated(
            UUID projectId,
            SprintStatus status,
            int page,
            int size,
            String sort) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        authorizationService.requireProjectAccess(project);

        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.SPRINT_SORT_FIELDS, "startDate,asc"
        );

        Page<Sprint> sprintPage;
        if (status != null) {
            sprintPage = sprintRepository.findByProjectIdAndStatus(projectId, status, pageable);
        } else {
            sprintPage = sprintRepository.findByProjectId(projectId, pageable);
        }

        return PageResponse.of(sprintPage.map(sprintMapper::toResponse));
    }

    @Transactional
    public SprintResponse updateSprint(UUID id, SprintUpdateRequest request) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + id));

        // Derive project from Sprint entity, requiring PM/Admin
        authorizationService.requireProjectManagerOrAdmin(sprint.getProject());

        SprintStatus oldStatus = sprint.getStatus();

        Instant newStart = request.getStartDate() != null ? request.getStartDate() : sprint.getStartDate();
        Instant newEnd = request.getEndDate() != null ? request.getEndDate() : sprint.getEndDate();

        if (newStart != null && newEnd != null && newStart.isAfter(newEnd)) {
            throw new BusinessRuleException("Sprint end date cannot be before start date");
        }

        sprintMapper.updateEntity(request, sprint);
        Sprint updated = sprintRepository.save(sprint);

        // Notify project participants if sprint status changed
        if (request.getStatus() != null && request.getStatus() != oldStatus && notificationService != null && projectMemberRepository != null) {
            UUID actorId = securityUtils != null && securityUtils.isAuthenticated() ? securityUtils.getCurrentUserId() : null;
            User actor = actorId != null && userRepository != null ? userRepository.findById(actorId).orElse(null) : null;

            List<ProjectMember> members = projectMemberRepository.findByProjectId(updated.getProject().getId());
            List<User> participants = new ArrayList<>();
            for (ProjectMember pm : members) {
                if (pm.getUser() != null) {
                    participants.add(pm.getUser());
                }
            }
            if (updated.getProject().getCreatedBy() != null) {
                participants.add(updated.getProject().getCreatedBy());
            }

            notificationService.createNotifications(
                    participants,
                    actor,
                    NotificationType.SPRINT_STATUS_CHANGED,
                    "Sprint status updated",
                    String.format("Sprint \"%s\" is now %s", updated.getName(), updated.getStatus()),
                    NotificationReferenceType.SPRINT,
                    updated.getId()
            );
        }

        return sprintMapper.toResponse(updated);
    }

    @Transactional
    public void deleteSprint(UUID id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + id));

        // Derive project from Sprint entity, requiring PM/Admin
        authorizationService.requireProjectManagerOrAdmin(sprint.getProject());

        sprintRepository.delete(sprint);
    }
}
