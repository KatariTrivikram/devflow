package com.devflow.mapper;

import com.devflow.dto.request.ActivityCreateRequest;
import com.devflow.dto.response.ActivityResponse;
import com.devflow.entity.Activity;
import org.springframework.stereotype.Component;

@Component
public class ActivityMapper {
    public ActivityResponse toResponse(Activity entity) {
        if (entity == null) return null;
        ActivityResponse response = new ActivityResponse();
        response.setId(entity.getId());
        if (entity.getIssue() != null) {
            response.setIssueId(entity.getIssue().getId());
            response.setIssueTitle(entity.getIssue().getTitle());
        }
        if (entity.getUser() != null) {
            response.setUserId(entity.getUser().getId());
            response.setUserName(entity.getUser().getName());
            response.setUserEmail(entity.getUser().getEmail());
        }
        response.setAction(entity.getAction());
        response.setOldValue(entity.getOldValue());
        response.setNewValue(entity.getNewValue());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    public Activity toEntity(ActivityCreateRequest request) {
        if (request == null) return null;
        Activity entity = new Activity();
        entity.setAction(request.getAction());
        entity.setOldValue(request.getOldValue());
        entity.setNewValue(request.getNewValue());
        return entity;
    }
}
