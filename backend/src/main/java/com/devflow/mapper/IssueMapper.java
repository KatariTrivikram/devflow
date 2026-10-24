package com.devflow.mapper;
import com.devflow.dto.request.IssueCreateRequest;
import com.devflow.dto.request.IssueUpdateRequest;
import com.devflow.dto.response.IssueResponse;
import com.devflow.entity.Issue;
import org.springframework.stereotype.Component;

@Component
public class IssueMapper {
    public IssueResponse toResponse(Issue entity) {
        if (entity == null) return null;
        IssueResponse response = new IssueResponse();
        response.setId(entity.getId());
        if (entity.getProject() != null) {
            response.setProjectId(entity.getProject().getId());
        }
        if (entity.getSprint() != null) {
            response.setSprintId(entity.getSprint().getId());
        }
        if (entity.getAssignedTo() != null) {
            response.setAssignedToId(entity.getAssignedTo().getId());
        }
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setType(entity.getType());
        response.setPriority(entity.getPriority());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public Issue toEntity(IssueCreateRequest request) {
        if (request == null) return null;
        Issue entity = new Issue();
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());
        entity.setPriority(request.getPriority());
        entity.setStatus(request.getStatus());
        // project, sprint, assignedTo set by service
        return entity;
    }

    public void updateEntity(IssueUpdateRequest request, Issue entity) {
        if (request == null || entity == null) return;
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }
}
