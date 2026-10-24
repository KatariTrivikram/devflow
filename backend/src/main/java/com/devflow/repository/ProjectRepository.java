package com.devflow.repository;

import com.devflow.entity.Project;
import com.devflow.entity.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {
    List<Project> findByCreatedById(UUID createdById);
    List<Project> findByStatus(ProjectStatus status);
    List<Project> findByNameContainingIgnoreCase(String name);

    @Query("SELECT p.id FROM Project p WHERE p.createdBy.id = :userId OR p.id IN (SELECT pm.project.id FROM ProjectMember pm WHERE pm.user.id = :userId)")
    List<UUID> findAccessibleProjectIdsForManager(@Param("userId") UUID userId);

    @Query("SELECT pm.project.id FROM ProjectMember pm WHERE pm.user.id = :userId")
    List<UUID> findAccessibleProjectIdsForMember(@Param("userId") UUID userId);

    @Query("SELECT p.status, COUNT(p) FROM Project p GROUP BY p.status")
    List<Object[]> countProjectsByStatus();

    @Query("SELECT p.status, COUNT(p) FROM Project p WHERE p.id IN :projectIds GROUP BY p.status")
    List<Object[]> countProjectsByStatusIn(@Param("projectIds") Collection<UUID> projectIds);
}
