package com.devflow.specification;

import com.devflow.entity.Issue;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
import com.devflow.entity.enums.IssueType;
import com.devflow.entity.enums.UserRole;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class IssueSpecification {

    private IssueSpecification() {
    }

    public static Specification<Issue> hasProjectId(UUID projectId) {
        return (root, query, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Issue> hasSprintId(UUID sprintId) {
        return (root, query, cb) -> sprintId == null ? null : cb.equal(root.get("sprint").get("id"), sprintId);
    }

    public static Specification<Issue> hasNoSprint() {
        return (root, query, cb) -> cb.isNull(root.get("sprint"));
    }

    public static Specification<Issue> hasAssignedToId(UUID assignedToId) {
        return (root, query, cb) -> assignedToId == null ? null : cb.equal(root.get("assignedTo").get("id"), assignedToId);
    }

    public static Specification<Issue> hasStatus(IssueStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Issue> hasPriority(IssuePriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Issue> hasType(IssueType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Issue> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Issue> isAccessibleByUser(UUID userId, UserRole role) {
        return (root, query, cb) -> {
            if (role == UserRole.ADMIN) {
                return cb.conjunction();
            }

            Subquery<UUID> memberSubquery = query.subquery(UUID.class);
            Root<ProjectMember> pmRoot = memberSubquery.from(ProjectMember.class);
            memberSubquery.select(pmRoot.get("project").get("id"))
                    .where(cb.equal(pmRoot.get("user").get("id"), userId));

            Predicate isMember = root.get("project").get("id").in(memberSubquery);

            if (role == UserRole.PROJECT_MANAGER) {
                Predicate isCreator = cb.equal(root.get("project").get("createdBy").get("id"), userId);
                return cb.or(isCreator, isMember);
            }

            return isMember;
        };
    }
}
