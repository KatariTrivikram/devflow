package com.devflow.security;

import com.devflow.entity.Comment;
import com.devflow.entity.Issue;
import com.devflow.entity.Project;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.repository.ProjectMemberRepository;
import com.devflow.repository.ProjectRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthorizationService {

    private final SecurityUtils securityUtils;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public AuthorizationService(
            SecurityUtils securityUtils,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.securityUtils = securityUtils;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public boolean isAdmin() {
        return securityUtils.getCurrentUserRole() == UserRole.ADMIN;
    }

    public boolean isProjectManager() {
        return securityUtils.getCurrentUserRole() == UserRole.PROJECT_MANAGER;
    }

    public boolean isDeveloper() {
        return securityUtils.getCurrentUserRole() == UserRole.DEVELOPER;
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Access denied: Admin role required");
        }
    }

    public void requireProjectCreateAccess() {
        if (!isAdmin() && !isProjectManager()) {
            throw new AccessDeniedException("Access denied: Only Admins and Project Managers can create projects");
        }
    }

    public boolean isProjectMember(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public void requireProjectMember(UUID projectId) {
        if (isAdmin()) {
            return;
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!isProjectMember(projectId, currentUserId)) {
            throw new AccessDeniedException("Access denied: Not a member of project");
        }
    }

    public void requireProjectManagerOrAdmin(Project project) {
        if (isAdmin()) {
            return;
        }
        if (!isProjectManager()) {
            throw new AccessDeniedException("Access denied: Project Manager or Admin role required");
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (project.getCreatedBy() == null || !project.getCreatedBy().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied: You are not the manager of this project");
        }
    }

    public void requireProjectManagerOrAdmin(UUID projectId) {
        if (isAdmin()) {
            return;
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        requireProjectManagerOrAdmin(project);
    }

    public void requireProjectAccess(Project project) {
        if (isAdmin()) {
            return;
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        boolean isCreator = project.getCreatedBy() != null && project.getCreatedBy().getId().equals(currentUserId);
        boolean isMember = isProjectMember(project.getId(), currentUserId);
        if (!isCreator && !isMember) {
            throw new AccessDeniedException("Access denied: You do not have access to this project");
        }
    }

    public void requireProjectAccess(UUID projectId) {
        if (isAdmin()) {
            return;
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        requireProjectAccess(project);
    }

    public void requireIssueUpdateAccess(Issue issue) {
        if (isAdmin()) {
            return;
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (isProjectManager()) {
            if (issue.getProject().getCreatedBy() != null
                    && issue.getProject().getCreatedBy().getId().equals(currentUserId)) {
                return;
            }
        }
        if (isDeveloper()) {
            if (issue.getAssignedTo() != null && issue.getAssignedTo().getId().equals(currentUserId)) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied: You are not authorized to update this issue");
    }

    public void requireCommentModifyAccess(Comment comment) {
        if (isAdmin()) {
            return;
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (isProjectManager()) {
            Project project = comment.getIssue().getProject();
            if (project.getCreatedBy() != null && project.getCreatedBy().getId().equals(currentUserId)) {
                return;
            }
        }
        if (comment.getUser() != null && comment.getUser().getId().equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("Access denied: You are not authorized to modify this comment");
    }

    public void validateIssueAssignment(Project project, UUID targetUserId) {
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), targetUserId);
        boolean isCreator = project.getCreatedBy() != null && project.getCreatedBy().getId().equals(targetUserId);
        if (!isMember && !isCreator) {
            throw new BusinessRuleException("Assigned user does not belong to project");
        }
    }
}
