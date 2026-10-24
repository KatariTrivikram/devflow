package com.devflow.repository;

import com.devflow.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    @EntityGraph(attributePaths = {"user", "issue"})
    Page<Activity> findByIssueIdOrderByCreatedAtDesc(UUID issueId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "issue"})
    Page<Activity> findByIssueId(UUID issueId, Pageable pageable);

    @Query("SELECT a FROM Activity a JOIN FETCH a.issue i JOIN FETCH i.project p JOIN FETCH a.user u ORDER BY a.createdAt DESC")
    List<Activity> findRecentActivities(Pageable pageable);

    @Query("SELECT a FROM Activity a JOIN FETCH a.issue i JOIN FETCH i.project p JOIN FETCH a.user u WHERE i.project.id IN :projectIds ORDER BY a.createdAt DESC")
    List<Activity> findRecentActivitiesIn(@Param("projectIds") Collection<UUID> projectIds, Pageable pageable);
}
