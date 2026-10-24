package com.devflow.controller;

import com.devflow.dto.request.UserCreateRequest;
import com.devflow.dto.request.UserUpdateRequest;
import com.devflow.dto.response.UserResponse;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.DuplicateResourceException;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/users - successful create returns 201")
    void testCreateUser_Success() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.DEVELOPER);

        UserResponse response = new UserResponse();
        UUID id = UUID.randomUUID();
        response.setId(id);
        response.setName("Alice");
        response.setEmail("alice@example.com");
        response.setRole(UserRole.DEVELOPER);

        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("POST /api/users - validation failure returns 400 with errors map")
    void testCreateUser_ValidationFailure() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        // missing name, invalid email, short password, missing role
        request.setName("");
        request.setEmail("not-an-email");
        request.setPassword("short");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/users - duplicate email returns 409")
    void testCreateUser_DuplicateEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.DEVELOPER);

        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("User with email alice@example.com already exists"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("User with email alice@example.com already exists"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - successful get returns 200")
    void testGetUserById_Success() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse();
        response.setId(id);
        response.setName("Alice");

        when(userService.getUserById(id)).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - not found returns 404")
    void testGetUserById_NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUserById(id))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + id));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id: " + id));
    }

    @Test
    @DisplayName("GET /api/users/email - successful get by email returns 200")
    void testGetUserByEmail_Success() throws Exception {
        UserResponse response = new UserResponse();
        response.setEmail("alice@example.com");

        when(userService.getUserByEmail("alice@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/users/email").param("email", "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - successful update returns 200")
    void testUpdateUser_Success() throws Exception {
        UUID id = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Alice New");

        UserResponse response = new UserResponse();
        response.setId(id);
        response.setName("Alice New");

        when(userService.updateUser(eq(id), any(UserUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice New"));
    }
}
