package com.devflow.specification;

import com.devflow.entity.Project;
import com.devflow.entity.ProjectMember;
import com.devflow.entity.enums.ProjectStatus;
import com.devflow.entity.enums.UserRole;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ProjectSpecification {

    private ProjectSpecification() {
    }

    public static Specification<Project> hasStatus(ProjectStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Project> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };
    }

    public static Specification<Project> searchGlobal(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Project> isAccessibleByUser(UUID userId, UserRole role) {
        return (root, query, cb) -> {
            if (role == UserRole.ADMIN) {
                return cb.conjunction();
            }

            Subquery<UUID> memberSubquery = query.subquery(UUID.class);
            Root<ProjectMember> pmRoot = memberSubquery.from(ProjectMember.class);
            memberSubquery.select(pmRoot.get("project").get("id"))
                    .where(cb.equal(pmRoot.get("user").get("id"), userId));

            Predicate isMember = root.get("id").in(memberSubquery);

            if (role == UserRole.PROJECT_MANAGER) {
                Predicate isCreator = cb.equal(root.get("createdBy").get("id"), userId);
                return cb.or(isCreator, isMember);
            }

            return isMember;
        };
    }
}
