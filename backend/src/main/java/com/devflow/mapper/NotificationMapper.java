package com.devflow.mapper;

import com.devflow.dto.response.NotificationResponse;
import com.devflow.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification entity) {
        if (entity == null) {
            return null;
        }

        NotificationResponse response = new NotificationResponse();
        response.setId(entity.getId());
        response.setType(entity.getType());
        response.setTitle(entity.getTitle());
        response.setMessage(entity.getMessage());
        response.setReferenceType(entity.getReferenceType());
        response.setReferenceId(entity.getReferenceId());
        response.setRead(entity.isRead());
        response.setCreatedAt(entity.getCreatedAt());
        response.setReadAt(entity.getReadAt());

        if (entity.getActor() != null) {
            response.setActorId(entity.getActor().getId());
            response.setActorName(entity.getActor().getName());
        }

        return response;
    }
}
