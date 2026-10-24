package com.devflow.repository;

import com.devflow.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findByIssueIdOrderByCreatedAtAsc(UUID issueId, Pageable pageable);
    Page<Comment> findByIssueId(UUID issueId, Pageable pageable);
}
