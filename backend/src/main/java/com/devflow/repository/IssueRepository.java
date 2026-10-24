package com.devflow.repository;

import com.devflow.entity.Issue;
import com.devflow.entity.enums.IssuePriority;
import com.devflow.entity.enums.IssueStatus;
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
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {
    Page<Issue> findByProjectId(UUID projectId, Pageable pageable);
    Page<Issue> findBySprintId(UUID sprintId, Pageable pageable);
    Page<Issue> findByAssignedToId(UUID assignedToId, Pageable pageable);
    Page<Issue> findByStatus(IssueStatus status, Pageable pageable);
    Page<Issue> findByPriority(IssuePriority priority, Pageable pageable);
    Page<Issue> findByProjectIdAndStatus(UUID projectId, IssueStatus status, Pageable pageable);
    Page<Issue> findByProjectIdAndAssignedToId(UUID projectId, UUID assignedToId, Pageable pageable);
    long countByProjectIdAndAssignedToId(UUID projectId, UUID assignedToId);

    @Query("SELECT i.status, COUNT(i) FROM Issue i WHERE i.sprint.id = :sprintId GROUP BY i.status")
    List<Object[]> countIssuesByStatusAndSprintId(@Param("sprintId") UUID sprintId);

    @Query("SELECT i.status, COUNT(i) FROM Issue i GROUP BY i.status")
    List<Object[]> countIssuesByStatus();

    @Query("SELECT i.status, COUNT(i) FROM Issue i WHERE i.project.id IN :projectIds GROUP BY i.status")
    List<Object[]> countIssuesByStatusIn(@Param("projectIds") Collection<UUID> projectIds);

    @Query("SELECT i.priority, COUNT(i) FROM Issue i GROUP BY i.priority")
    List<Object[]> countIssuesByPriority();

    @Query("SELECT i.priority, COUNT(i) FROM Issue i WHERE i.project.id IN :projectIds GROUP BY i.priority")
    List<Object[]> countIssuesByPriorityIn(@Param("projectIds") Collection<UUID> projectIds);

    @Query("SELECT i.status, COUNT(i) FROM Issue i WHERE i.assignedTo.id = :userId GROUP BY i.status")
    List<Object[]> countAssignedIssuesByStatus(@Param("userId") UUID userId);

    @Query("SELECT i.status, COUNT(i) FROM Issue i WHERE i.assignedTo.id = :userId AND i.project.id IN :projectIds GROUP BY i.status")
    List<Object[]> countAssignedIssuesByStatusIn(@Param("userId") UUID userId, @Param("projectIds") Collection<UUID> projectIds);
}
