package com.devflow.service;

import com.devflow.dto.request.ProjectMemberCreateRequest;
import com.devflow.dto.response.ProjectMemberResponse;
import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.User;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.DuplicateResourceException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.ProjectMemberMapper;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.AuthorizationService;
import com.devflow.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import com.devflow.entity.enums.NotificationReferenceType;
import com.devflow.entity.enums.NotificationType;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final AuthorizationService authorizationService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final IssueRepository issueRepository;

    public ProjectMemberService(ProjectMemberRepository projectMemberRepository,
                                ProjectRepository projectRepository,
                                UserRepository userRepository,
                                ProjectMemberMapper projectMemberMapper,
                                AuthorizationService authorizationService,
                                SecurityUtils securityUtils) {
        this(projectMemberRepository, projectRepository, userRepository, projectMemberMapper, authorizationService, securityUtils, null, null);
    }

    public ProjectMemberService(ProjectMemberRepository projectMemberRepository,
                                ProjectRepository projectRepository,
                                UserRepository userRepository,
                                ProjectMemberMapper projectMemberMapper,
                                AuthorizationService authorizationService,
                                SecurityUtils securityUtils,
                                NotificationService notificationService) {
        this(projectMemberRepository, projectRepository, userRepository, projectMemberMapper, authorizationService, securityUtils, notificationService, null);
    }

    @Autowired
    public ProjectMemberService(ProjectMemberRepository projectMemberRepository,
                                ProjectRepository projectRepository,
                                UserRepository userRepository,
                                ProjectMemberMapper projectMemberMapper,
                                AuthorizationService authorizationService,
                                SecurityUtils securityUtils,
                                NotificationService notificationService,
                                IssueRepository issueRepository) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberMapper = projectMemberMapper;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
        this.issueRepository = issueRepository;
    }

    @Transactional
    public ProjectMemberResponse addMember(ProjectMemberCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

        // Only ADMIN or the Project Manager managing this project can add members
        authorizationService.requireProjectManagerOrAdmin(project);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        if (projectMemberRepository.existsByProjectIdAndUserId(request.getProjectId(), request.getUserId())) {
            throw new DuplicateResourceException("User is already a member of this project");
        }

        ProjectMember member = projectMemberMapper.toEntity(request);
        member.setProject(project);
        member.setUser(user);
        ProjectMember saved = projectMemberRepository.save(member);

        // Notify newly added member
        if (notificationService != null) {
            UUID actorId = securityUtils.isAuthenticated() ? securityUtils.getCurrentUserId() : null;
            User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
            notificationService.createNotification(
                    user,
                    actor,
                    NotificationType.PROJECT_MEMBER_ADDED,
                    "Added to project",
                    String.format("You were added to project \"%s\"", project.getName()),
                    NotificationReferenceType.PROJECT,
                    project.getId()
            );
        }

        return projectMemberMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        authorizationService.requireProjectAccess(project);

        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(projectMemberMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getUserMemberships(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        if (!authorizationService.isAdmin() && !userId.equals(securityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("Access denied: Cannot query memberships for another user");
        }

        return projectMemberRepository.findByUserId(userId).stream()
                .map(projectMemberMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectMemberResponse getMembership(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        authorizationService.requireProjectAccess(project);

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project membership not found for user: " + userId + " in project: " + projectId));
        return projectMemberMapper.toResponse(member);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Only ADMIN or the Project Manager managing this project can remove members
        authorizationService.requireProjectManagerOrAdmin(project);

        if (issueRepository != null) {
            long assignedIssues = issueRepository.countByProjectIdAndAssignedToId(projectId, userId);
            if (assignedIssues > 0) {
                throw new BusinessRuleException(
                        "Cannot remove member who is assigned to " + assignedIssues + " issue(s) in this project. Please reassign or unassign their issues first.");
            }
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project membership not found for user: " + userId + " in project: " + projectId));
        projectMemberRepository.delete(member);
    }
}
