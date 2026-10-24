package com.devflow.mapper;
import com.devflow.dto.request.CommentCreateRequest;
import com.devflow.dto.request.CommentUpdateRequest;
import com.devflow.dto.response.CommentResponse;
import com.devflow.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {
    public CommentResponse toResponse(Comment entity) {
        if (entity == null) return null;
        CommentResponse response = new CommentResponse();
        response.setId(entity.getId());
        if (entity.getIssue() != null) {
            response.setIssueId(entity.getIssue().getId());
        }
        if (entity.getUser() != null) {
            response.setUserId(entity.getUser().getId());
        }
        response.setContent(entity.getContent());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public Comment toEntity(CommentCreateRequest request) {
        if (request == null) return null;
        Comment entity = new Comment();
        entity.setContent(request.getContent());
        // issue and user set by service
        return entity;
    }

    public void updateEntity(CommentUpdateRequest request, Comment entity) {
        if (request == null || entity == null) return;
        if (request.getContent() != null) {
            entity.setContent(request.getContent());
        }
    }
}
