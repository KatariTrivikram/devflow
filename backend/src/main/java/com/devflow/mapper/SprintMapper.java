package com.devflow.mapper;
import com.devflow.dto.request.SprintCreateRequest;
import com.devflow.dto.request.SprintUpdateRequest;
import com.devflow.dto.response.SprintResponse;
import com.devflow.entity.Sprint;
import org.springframework.stereotype.Component;

@Component
public class SprintMapper {
    public SprintResponse toResponse(Sprint entity) {
        if (entity == null) return null;
        SprintResponse response = new SprintResponse();
        response.setId(entity.getId());
        if (entity.getProject() != null) {
            response.setProjectId(entity.getProject().getId());
        }
        response.setName(entity.getName());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public Sprint toEntity(SprintCreateRequest request) {
        if (request == null) return null;
        Sprint entity = new Sprint();
        entity.setName(request.getName());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setStatus(request.getStatus());
        // project will be set by service layer
        return entity;
    }

    public void updateEntity(SprintUpdateRequest request, Sprint entity) {
        if (request == null || entity == null) return;
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getStartDate() != null) {
            entity.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            entity.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }
}
