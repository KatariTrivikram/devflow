package com.devflow.mapper;
import com.devflow.dto.request.UserCreateRequest;
import com.devflow.dto.request.UserUpdateRequest;
import com.devflow.dto.response.UserResponse;
import com.devflow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(User entity) {
        if (entity == null) return null;
        UserResponse response = new UserResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setEmail(entity.getEmail());
        response.setRole(entity.getRole());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public User toEntity(UserCreateRequest request) {
        if (request == null) return null;
        User entity = new User();
        entity.setName(request.getName());
        entity.setEmail(request.getEmail());
        entity.setPassword(request.getPassword());
        entity.setRole(request.getRole());
        return entity;
    }

    public void updateEntity(UserUpdateRequest request, User entity) {
        if (request == null || entity == null) return;
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getRole() != null) {
            entity.setRole(request.getRole());
        }
    }
}
