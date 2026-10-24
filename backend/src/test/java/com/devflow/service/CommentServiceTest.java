package com.devflow.service;

import com.devflow.dto.request.CommentCreateRequest;
import com.devflow.dto.request.CommentUpdateRequest;
import com.devflow.dto.response.CommentResponse;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("createComment - success")
    void testCreateComment_Success() {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CommentCreateRequest request = new CommentCreateRequest();
        request.setIssueId(issueId);
        request.setUserId(userId);
        request.setContent("Test comment");

        Issue issue = new Issue();
        User user = new User();
        Comment comment = new Comment();
        Comment savedComment = new Comment();
        savedComment.setId(UUID.randomUUID());

        CommentResponse response = new CommentResponse();
        response.setId(savedComment.getId());
        response.setContent("Test comment");

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentMapper.toEntity(request)).thenReturn(comment);
        when(commentRepository.save(comment)).thenReturn(savedComment);
        when(commentMapper.toResponse(savedComment)).thenReturn(response);

        CommentResponse result = commentService.createComment(request);

        assertNotNull(result);
        assertEquals(response.getId(), result.getId());
        verify(authorizationService).requireProjectAccess(issue.getProject());
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("createComment - missing issue throws ResourceNotFoundException")
    void testCreateComment_MissingIssue() {
        UUID issueId = UUID.randomUUID();
        CommentCreateRequest request = new CommentCreateRequest();
        request.setIssueId(issueId);

        when(issueRepository.findById(issueId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.createComment(request));
    }

    @Test
    @DisplayName("createComment - missing user throws ResourceNotFoundException")
    void testCreateComment_MissingUser() {
        UUID issueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CommentCreateRequest request = new CommentCreateRequest();
        request.setIssueId(issueId);
        request.setUserId(userId);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(new Issue()));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.createComment(request));
    }

    @Test
    @DisplayName("getCommentById - not found throws ResourceNotFoundException")
    void testGetCommentById_NotFound() {
        UUID id = UUID.randomUUID();
        when(commentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.getCommentById(id));
    }

    @Test
    @DisplayName("updateComment - not found throws ResourceNotFoundException")
    void testUpdateComment_NotFound() {
        UUID id = UUID.randomUUID();
        CommentUpdateRequest request = new CommentUpdateRequest();
        when(commentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.updateComment(id, request));
    }

    @Test
    @DisplayName("deleteComment - success")
    void testDeleteComment_Success() {
        UUID id = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(id);

        when(commentRepository.findById(id)).thenReturn(Optional.of(comment));

        commentService.deleteComment(id);

        verify(authorizationService).requireCommentModifyAccess(comment);
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("deleteComment - not found throws ResourceNotFoundException")
    void testDeleteComment_NotFound() {
        UUID id = UUID.randomUUID();
        when(commentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.deleteComment(id));
    }
}
