package com.devflow.repository;

import com.devflow.entity.Sprint;
import com.devflow.entity.enums.SprintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID>, JpaSpecificationExecutor<Sprint> {
    List<Sprint> findByProjectId(UUID projectId);
    List<Sprint> findByProjectIdAndStatus(UUID projectId, SprintStatus status);
    Page<Sprint> findByProjectId(UUID projectId, Pageable pageable);
    Page<Sprint> findByProjectIdAndStatus(UUID projectId, SprintStatus status, Pageable pageable);

    @Query("SELECT s.status, COUNT(s) FROM Sprint s GROUP BY s.status")
    List<Object[]> countSprintsByStatus();

    @Query("SELECT s.status, COUNT(s) FROM Sprint s WHERE s.project.id IN :projectIds GROUP BY s.status")
    List<Object[]> countSprintsByStatusIn(@Param("projectIds") Collection<UUID> projectIds);
}
