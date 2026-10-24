package com.devflow.service;

import com.devflow.dto.request.CommentCreateRequest;
import com.devflow.dto.request.CommentUpdateRequest;
import com.devflow.dto.response.CommentResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.entity.Comment;
import com.devflow.entity.Issue;
import com.devflow.entity.User;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.CommentMapper;
import com.devflow.repository.CommentRepository;
import com.devflow.repository.IssueRepository;
import com.devflow.repository.UserRepository;
import com.devflow.security.AuthorizationService;
import com.devflow.security.SecurityUtils;
import com.devflow.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import com.devflow.entity.enums.NotificationReferenceType;
import com.devflow.entity.enums.NotificationType;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final AuthorizationService authorizationService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          IssueRepository issueRepository,
                          UserRepository userRepository,
                          CommentMapper commentMapper,
                          AuthorizationService authorizationService,
                          SecurityUtils securityUtils) {
        this(commentRepository, issueRepository, userRepository, commentMapper, authorizationService, securityUtils, null);
    }

    @Autowired
    public CommentService(CommentRepository commentRepository,
                          IssueRepository issueRepository,
                          UserRepository userRepository,
                          CommentMapper commentMapper,
                          AuthorizationService authorizationService,
                          SecurityUtils securityUtils,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.commentMapper = commentMapper;
        this.authorizationService = authorizationService;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
    }

    @Transactional
    public CommentResponse createComment(CommentCreateRequest request) {
        Issue issue = issueRepository.findById(request.getIssueId())
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + request.getIssueId()));

        // Check that authenticated user has access to this issue's project
        authorizationService.requireProjectAccess(issue.getProject());

        // Acting user ID comes securely from SecurityContext, never trusting request.getUserId()
        UUID actorId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + actorId));

        Comment comment = commentMapper.toEntity(request);
        comment.setIssue(issue);
        comment.setUser(user);
        Comment saved = commentRepository.save(comment);

        // Notify issue assignee (if assigned and assignee is not the comment author)
        if (issue.getAssignedTo() != null && notificationService != null) {
            notificationService.createNotification(
                    issue.getAssignedTo(),
                    user,
                    NotificationType.COMMENT_ADDED,
                    "New comment on issue",
                    String.format("%s commented on \"%s\"", user.getName(), issue.getTitle()),
                    NotificationReferenceType.ISSUE,
                    issue.getId()
            );
        }

        return commentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getCommentsByIssue(UUID issueId, int page, int size, String sort) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + issueId));

        authorizationService.requireProjectAccess(issue.getProject());

        Pageable pageable = PaginationUtils.createPageable(
                page, size, sort, PaginationUtils.COMMENT_SORT_FIELDS, "createdAt,asc"
        );

        return PageResponse.of(commentRepository.findByIssueId(issueId, pageable)
                .map(commentMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getCommentsByIssue(UUID issueId, Pageable pageable) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with id: " + issueId));

        authorizationService.requireProjectAccess(issue.getProject());

        return PageResponse.of(commentRepository.findByIssueId(issueId, pageable)
                .map(commentMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CommentResponse getCommentById(UUID id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));

        authorizationService.requireProjectAccess(comment.getIssue().getProject());

        return commentMapper.toResponse(comment);
    }

    @Transactional
    public CommentResponse updateComment(UUID id, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));

        // Author, managing PM, or Admin
        authorizationService.requireCommentModifyAccess(comment);

        commentMapper.updateEntity(request, comment);
        Comment updated = commentRepository.save(comment);
        return commentMapper.toResponse(updated);
    }

    @Transactional
    public void deleteComment(UUID id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));

        // Author, managing PM, or Admin
        authorizationService.requireCommentModifyAccess(comment);

        commentRepository.delete(comment);
    }
}
