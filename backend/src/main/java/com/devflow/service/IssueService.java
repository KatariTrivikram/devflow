package com.devflow.service;

import com.devflow.dto.request.IssueCreateRequest;
import com.devflow.dto.request.IssueUpdateRequest;
import com.devflow.dto.response.IssueResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.Sprint;
import com.devflow.entity.User;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.IssueMapper;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.AuthorizationService;
import com.devflow.security.SecurityUtils;
import com.devflow.specification.IssueSpecification;
import com.devflow.util.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devflow.entity.enums.NotificationReferenceType;
import com.devflow.entity.enums.NotificationType;
import java.util.UUID;

@Service
public class IssueService {

    private static final Logger log = LoggerFactory.getLogger(IssueService.class);

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final IssueMapper issueMapper;
    private final AuthorizationService authorizationService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    public IssueService(IssueRepository issueRepository,
                        ProjectRepository projectRepository,
                        SprintRepository sprintRepository,
                        UserRepository userRepository,
                        IssueMapper issueMapper,
                        AuthorizationService authorizationService,
                        SecurityUtils securityUtils) {
        this(issueRepository, projectRepository, sprintRepository, userRepository, issueMapper, authorizationService, securityUtils, null);
    }

    @Autowired
    public IssueService(IssueRepository issueRepository,
                        ProjectRepository projectRepository,
                        SprintRepository sprintRepository,
                        UserRepository userRepository,
                        IssueMapper issueMapper,
                        AuthorizationService authorizationService,
                        SecurityUtils securityUtils,
                        NotificationService notificationService) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.userRepository = userRepository;
        this.issueMapper = issueMapper;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
    }

    @Transactional
    public IssueResponse createIssue(IssueCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

        // Only ADMIN or the Project Manager managing this project can create issues
        authorizationService.requireProjectManagerOrAdmin(project);

        Sprint sprint = null;
        if (request.getSprintId() != null) {
            sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + request.getSprintId()));
            if (!sprint.getProject().getId().equals(project.getId())) {
                throw new BusinessRuleException("Sprint does not belong to the project");
            }
        }

        User assignedTo = null;
        if (request.getAssignedToId() != null) {
            assignedTo = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getAssignedToId()));
            // Validate that the assigned user belongs to the project
            authorizationService.validateIssueAssignment(project, request.getAssignedToId());
        }

        Issue issue = issueMapper.toEntity(request);
        issue.setProject(project);
        issue.setSprint(sprint);
        issue.setAssignedTo(assignedTo);

        Issue saved = issueRepository.save(issue);
        log.info("Issue created successfully: id={}, title='{}', projectId={}", saved.getId(), saved.getTitle(), project.getId());

        if (assignedTo != null && notificationService != null) {
            UUID actorId = securityUtils.isAuthenticated() ? securityUtils.getCurrentUserId() : null;
            User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
            notificationService.createNotification(
                    assignedTo,
                    actor,
                    NotificationType.ISSUE_ASSIGNED,
                    "You were assigned an issue",
                    String.format("You were assigned issue \"%s\"", saved.getTitle()),
                    NotificationReferenceType.ISSUE,
                    saved.getId()
            );
        }

        return issueMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssueById(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + id));

        authorizationService.requireProjectAccess(issue.getProject());

        return issueMapper.toResponse(issue);
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssues(
            UUID projectId,
            UUID sprintId,
            UUID assignedToId,
            IssueStatus status,
            IssuePriority priority,
            IssueType type,
            String search,
            int page,
            int size,
            String sort) {
        return getIssues(projectId, sprintId, null, assignedToId, status, priority, type, search, page, size, sort);
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssues(
            UUID projectId,
            UUID sprintId,
            Boolean noSprint,
            UUID assignedToId,
            IssueStatus status,
            IssuePriority priority,
            IssueType type,
            String search,
            int page,
            int size,
            String sort) {

        if (projectId != null) {
            authorizationService.requireProjectAccess(projectId);
        }

        UUID currentUserId = securityUtils.getCurrentUserId();
        UserRole currentUserRole = securityUtils.getCurrentUserRole();

        Specification<Issue> spec = Specification.where(IssueSpecification.isAccessibleByUser(currentUserId, currentUserRole));
        if (projectId != null) {
            spec = spec.and(IssueSpecification.hasProjectId(projectId));
        }
        if (Boolean.TRUE.equals(noSprint)) {
            spec = spec.and(IssueSpecification.hasNoSprint());
        } else if (sprintId != null) {
            spec = spec.and(IssueSpecification.hasSprintId(sprintId));
        }
        if (assignedToId != null) {
            spec = spec.and(IssueSpecification.hasAssignedToId(assignedToId));
        }
        if (status != null) {
            spec = spec.and(IssueSpecification.hasStatus(status));
        }
        if (priority != null) {
            spec = spec.and(IssueSpecification.hasPriority(priority));
        }
        if (type != null) {
            spec = spec.and(IssueSpecification.hasType(type));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(IssueSpecification.search(search));
        }

        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.ISSUE_SORT_FIELDS, "createdAt,desc"
        );

        Page<Issue> issuePage = issueRepository.findAll(spec, pageable);
        return PageResponse.of(issuePage.map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssuesByProject(UUID projectId, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        authorizationService.requireProjectAccess(project);

        return PageResponse.of(issueRepository.findByProjectId(projectId, pageable)
                .map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssuesBySprint(UUID sprintId, Pageable pageable) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + sprintId));

        authorizationService.requireProjectAccess(sprint.getProject());

        return PageResponse.of(issueRepository.findBySprintId(sprintId, pageable)
                .map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssuesByAssignee(UUID assignedToId, Pageable pageable) {
        if (!userRepository.existsById(assignedToId)) {
            throw new ResourceNotFoundException("User not found with id: " + assignedToId);
        }

        if (!authorizationService.isAdmin() && !assignedToId.equals(securityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("Access denied: Cannot query assigned issues for another user");
        }

        UUID currentUserId = securityUtils.getCurrentUserId();
        UserRole currentUserRole = securityUtils.getCurrentUserRole();

        Specification<Issue> spec = Specification.where(IssueSpecification.isAccessibleByUser(currentUserId, currentUserRole))
                .and(IssueSpecification.hasAssignedToId(assignedToId));

        return PageResponse.of(issueRepository.findAll(spec, pageable)
                .map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssuesByStatus(IssueStatus status, Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        UserRole currentUserRole = securityUtils.getCurrentUserRole();

        Specification<Issue> spec = Specification.where(IssueSpecification.isAccessibleByUser(currentUserId, currentUserRole))
                .and(IssueSpecification.hasStatus(status));

        return PageResponse.of(issueRepository.findAll(spec, pageable)
                .map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssuesByPriority(IssuePriority priority, Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        UserRole currentUserRole = securityUtils.getCurrentUserRole();

        Specification<Issue> spec = Specification.where(IssueSpecification.isAccessibleByUser(currentUserId, currentUserRole))
                .and(IssueSpecification.hasPriority(priority));

        return PageResponse.of(issueRepository.findAll(spec, pageable)
                .map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssuesByProjectAndStatus(UUID projectId, IssueStatus status, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        authorizationService.requireProjectAccess(project);

        return PageResponse.of(issueRepository.findByProjectIdAndStatus(projectId, status, pageable)
                .map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssuesByProjectAndAssignee(UUID projectId, UUID assignedToId, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        authorizationService.requireProjectAccess(project);

        if (!userRepository.existsById(assignedToId)) {
            throw new ResourceNotFoundException("User not found with id: " + assignedToId);
        }
        return PageResponse.of(issueRepository.findByProjectIdAndAssignedToId(projectId, assignedToId, pageable)
                .map(issueMapper::toResponse));
    }

    @Transactional
    public IssueResponse updateIssue(UUID id, IssueUpdateRequest request) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + id));

        // ADMIN, managing PM, or developer assigned to this issue
        authorizationService.requireIssueUpdateAccess(issue);

        UUID oldAssigneeId = issue.getAssignedTo() != null ? issue.getAssignedTo().getId() : null;
        IssueStatus oldStatus = issue.getStatus();
        IssuePriority oldPriority = issue.getPriority();

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + request.getSprintId()));
            if (!sprint.getProject().getId().equals(issue.getProject().getId())) {
                throw new BusinessRuleException("Sprint does not belong to the project");
            }
            issue.setSprint(sprint);
        }

        if (request.getAssignedToId() != null) {
            User assignedTo = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getAssignedToId()));
            // Validate that the newly assigned user belongs to the project
            authorizationService.validateIssueAssignment(issue.getProject(), request.getAssignedToId());
            issue.setAssignedTo(assignedTo);
        }

        issueMapper.updateEntity(request, issue);
        Issue updated = issueRepository.save(issue);
        log.info("Issue updated successfully: id={}", id);

        if (notificationService != null) {
            UUID actorId = securityUtils.isAuthenticated() ? securityUtils.getCurrentUserId() : null;
            User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

            // 1. Issue assignment notification
            if (updated.getAssignedTo() != null) {
                UUID newAssigneeId = updated.getAssignedTo().getId();
                if (!newAssigneeId.equals(oldAssigneeId)) {
                    notificationService.createNotification(
                            updated.getAssignedTo(),
                            actor,
                            NotificationType.ISSUE_ASSIGNED,
                            "You were assigned an issue",
                            String.format("You were assigned issue \"%s\"", updated.getTitle()),
                            NotificationReferenceType.ISSUE,
                            updated.getId()
                    );
                }
            }

            // 2. Issue status change notification
            if (request.getStatus() != null && request.getStatus() != oldStatus) {
                if (updated.getAssignedTo() != null) {
                    notificationService.createNotification(
                            updated.getAssignedTo(),
                            actor,
                            NotificationType.ISSUE_STATUS_CHANGED,
                            "Issue status updated",
                            String.format("Issue \"%s\" moved to %s", updated.getTitle(), updated.getStatus()),
                            NotificationReferenceType.ISSUE,
                            updated.getId()
                    );
                }
            }

            // 3. Issue priority change notification
            if (request.getPriority() != null && request.getPriority() != oldPriority) {
                if (updated.getAssignedTo() != null) {
                    notificationService.createNotification(
                            updated.getAssignedTo(),
                            actor,
                            NotificationType.ISSUE_PRIORITY_CHANGED,
                            "Issue priority changed",
                            String.format("Issue \"%s\" priority changed to %s", updated.getTitle(), updated.getPriority()),
                            NotificationReferenceType.ISSUE,
                            updated.getId()
                    );
                }
            }
        }

        return issueMapper.toResponse(updated);
    }

    @Transactional
    public void deleteIssue(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + id));

        // Only ADMIN or the Project Manager managing this project can delete issues
        authorizationService.requireProjectManagerOrAdmin(issue.getProject());

        issueRepository.delete(issue);
        log.info("Issue deleted successfully: id={}", id);
    }
}
