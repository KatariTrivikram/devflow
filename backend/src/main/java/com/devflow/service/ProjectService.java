package com.devflow.service;

import com.devflow.dto.request.ProjectCreateRequest;
import com.devflow.dto.request.ProjectUpdateRequest;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.dto.response.ProjectSummaryResponse;
import com.devflow.dto.response.ProjectSummaryResponse.ActiveSprintSummaryDto;
import com.devflow.dto.response.PageResponse;
import com.devflow.entity.Project;
import com.devflow.entity.Sprint;
import com.devflow.entity.User;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.SprintStatus;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.ProjectMapper;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.SprintRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.AuthorizationService;
import com.devflow.security.SecurityUtils;
import com.devflow.specification.ProjectSpecification;
import com.devflow.util.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final AuthorizationService authorizationService;
    private final SecurityUtils securityUtils;
    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          ProjectMapper projectMapper,
                          AuthorizationService authorizationService,
                          SecurityUtils securityUtils) {
        this(projectRepository, userRepository, projectMapper, authorizationService, securityUtils, null, null, null);
    }

    @Autowired
    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          ProjectMapper projectMapper,
                          AuthorizationService authorizationService,
                          SecurityUtils securityUtils,
                          IssueRepository issueRepository,
                          SprintRepository sprintRepository,
                          ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
        this.issueRepository = issueRepository;
        this.sprintRepository = sprintRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        authorizationService.requireProjectCreateAccess();

        // Acting user ID is obtained securely from SecurityContext, never trusting client-supplied ID
        UUID actorId = securityUtils.getCurrentUserId();
        User creator = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + actorId));

        Project project = projectMapper.toEntity(request);
        project.setCreatedBy(creator);
        Project saved = projectRepository.save(project);
        log.info("Project created successfully: id={}, name='{}', createdBy={}", saved.getId(), saved.getName(), creator.getId());
        return projectMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        authorizationService.requireProjectAccess(project);
        return projectMapper.toResponse(project);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getProjects(
            ProjectStatus status,
            String search,
            int page,
            int size,
            String sort) {

        UUID currentUserId = securityUtils.getCurrentUserId();
        UserRole currentUserRole = securityUtils.getCurrentUserRole();

        Specification<Project> spec = Specification.where(ProjectSpecification.isAccessibleByUser(currentUserId, currentUserRole));
        if (status != null) {
            spec = spec.and(ProjectSpecification.hasStatus(status));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(ProjectSpecification.search(search));
        }

        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.PROJECT_SORT_FIELDS, "createdAt,desc"
        );

        Page<Project> projectPage = projectRepository.findAll(spec, pageable);
        return PageResponse.of(projectPage.map(projectMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByCreator(UUID createdById) {
        if (!authorizationService.isAdmin() && !createdById.equals(securityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("Access denied: Cannot query projects for another user");
        }
        return projectRepository.findByCreatedById(createdById).stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByStatus(ProjectStatus status) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        UserRole currentUserRole = securityUtils.getCurrentUserRole();

        Specification<Project> spec = Specification.where(ProjectSpecification.isAccessibleByUser(currentUserId, currentUserRole));
        if (status != null) {
            spec = spec.and(ProjectSpecification.hasStatus(status));
        }

        return projectRepository.findAll(spec).stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> searchProjectsByName(String name) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        UserRole currentUserRole = securityUtils.getCurrentUserRole();

        Specification<Project> spec = Specification.where(ProjectSpecification.isAccessibleByUser(currentUserId, currentUserRole));
        if (name != null && !name.isBlank()) {
            spec = spec.and(ProjectSpecification.search(name));
        }

        return projectRepository.findAll(spec).stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse updateProject(UUID id, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        authorizationService.requireProjectManagerOrAdmin(project);

        projectMapper.updateEntity(request, project);
        Project updated = projectRepository.save(project);
        log.info("Project updated successfully: id={}", id);
        return projectMapper.toResponse(updated);
    }

    @Transactional
    public void deleteProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        authorizationService.requireProjectManagerOrAdmin(project);

        projectRepository.delete(project);
        log.info("Project deleted successfully: id={}", id);
    }

    @Transactional(readOnly = true)
    public ProjectSummaryResponse getProjectSummary(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        authorizationService.requireProjectAccess(project);

        ProjectSummaryResponse summary = new ProjectSummaryResponse();
        summary.setProjectId(project.getId());
        summary.setProjectName(project.getName());

        if (issueRepository != null) {
            List<Object[]> statusCounts = issueRepository.countIssuesByStatusIn(Collections.singletonList(projectId));
            Map<IssueStatus, Long> counts = new EnumMap<>(IssueStatus.class);
            for (Object[] row : statusCounts) {
                if (row[0] instanceof IssueStatus status && row[1] instanceof Number count) {
                    counts.put(status, count.longValue());
                }
            }

            long todo = counts.getOrDefault(IssueStatus.TODO, 0L);
            long inProgress = counts.getOrDefault(IssueStatus.IN_PROGRESS, 0L);
            long codeReview = counts.getOrDefault(IssueStatus.CODE_REVIEW, 0L);
            long done = counts.getOrDefault(IssueStatus.DONE, 0L);
            long total = todo + inProgress + codeReview + done;
            long open = todo + inProgress + codeReview;

            summary.setTotalIssues(total);
            summary.setTodoIssues(todo);
            summary.setInProgressIssues(inProgress);
            summary.setCodeReviewIssues(codeReview);
            summary.setDoneIssues(done);
            summary.setOpenIssues(open);
        }

        if (projectMemberRepository != null) {
            long memberCount = projectMemberRepository.countByProjectId(projectId);
            summary.setMemberCount(memberCount);
        }

        if (sprintRepository != null) {
            List<Sprint> activeSprints = sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
            if (!activeSprints.isEmpty()) {
                Sprint activeSprint = activeSprints.get(0);
                ActiveSprintSummaryDto sprintDto = new ActiveSprintSummaryDto();
                sprintDto.setId(activeSprint.getId());
                sprintDto.setName(activeSprint.getName());
                sprintDto.setStartDate(activeSprint.getStartDate());
                sprintDto.setEndDate(activeSprint.getEndDate());
                sprintDto.setStatus(activeSprint.getStatus().name());

                if (issueRepository != null) {
                    List<Object[]> sprintStatusCounts = issueRepository.countIssuesByStatusAndSprintId(activeSprint.getId());
                    Map<IssueStatus, Long> sCounts = new EnumMap<>(IssueStatus.class);
                    for (Object[] row : sprintStatusCounts) {
                        if (row[0] instanceof IssueStatus status && row[1] instanceof Number count) {
                            sCounts.put(status, count.longValue());
                        }
                    }

                    long sTodo = sCounts.getOrDefault(IssueStatus.TODO, 0L);
                    long sInProgress = sCounts.getOrDefault(IssueStatus.IN_PROGRESS, 0L) + sCounts.getOrDefault(IssueStatus.CODE_REVIEW, 0L);
                    long sDone = sCounts.getOrDefault(IssueStatus.DONE, 0L);
                    long sTotal = sTodo + sInProgress + sDone;
                    int progress = sTotal > 0 ? (int) Math.round((sDone * 100.0) / sTotal) : 0;

                    sprintDto.setTotalIssues(sTotal);
                    sprintDto.setCompletedIssues(sDone);
                    sprintDto.setInProgressIssues(sInProgress);
                    sprintDto.setTodoIssues(sTodo);
                    sprintDto.setProgressPercentage(progress);
                }

                summary.setActiveSprint(sprintDto);
            }
        }

        return summary;
    }
}
