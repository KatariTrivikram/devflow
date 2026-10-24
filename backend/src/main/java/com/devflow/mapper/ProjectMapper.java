package com.devflow.mapper;
import com.devflow.dto.request.ProjectCreateRequest;
import com.devflow.dto.request.ProjectUpdateRequest;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {
    public ProjectResponse toResponse(Project entity) {
        if (entity == null) return null;
        ProjectResponse response = new ProjectResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        if (entity.getCreatedBy() != null) {
            response.setCreatedById(entity.getCreatedBy().getId());
        }
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public Project toEntity(ProjectCreateRequest request) {
        if (request == null) return null;
        Project entity = new Project();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        // Note: createdBy should be set in the service layer using request.getCreatedById()
        return entity;
    }

    public void updateEntity(ProjectUpdateRequest request, Project entity) {
        if (request == null || entity == null) return;
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }
}
