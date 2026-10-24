package com.devflow.mapper;
import com.devflow.dto.request.ProjectMemberCreateRequest;
import com.devflow.dto.response.ProjectMemberResponse;
import com.devflow.entity.ProjectMember;
import org.springframework.stereotype.Component;

@Component
public class ProjectMemberMapper {
    public ProjectMemberResponse toResponse(ProjectMember entity) {
        if (entity == null) return null;
        ProjectMemberResponse response = new ProjectMemberResponse();
        response.setId(entity.getId());
        if (entity.getProject() != null) {
            response.setProjectId(entity.getProject().getId());
        }
        if (entity.getUser() != null) {
            response.setUserId(entity.getUser().getId());
        }
        response.setJoinedAt(entity.getJoinedAt());
        return response;
    }

    public ProjectMember toEntity(ProjectMemberCreateRequest request) {
        if (request == null) return null;
        ProjectMember entity = new ProjectMember();
        // project and user entities will be set by the service using IDs from request
        return entity;
    }
}
