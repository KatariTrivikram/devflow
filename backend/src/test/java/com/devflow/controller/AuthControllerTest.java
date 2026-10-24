package com.devflow.controller;

import com.devflow.dto.request.LoginRequest;
import com.devflow.dto.request.UserCreateRequest;
import com.devflow.dto.response.AuthResponse;
import com.devflow.dto.response.UserResponse;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.DuplicateResourceException;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.service.AuthService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register - successful registration returns 201")
    void testRegister_Success() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Bob");
        request.setEmail("bob@example.com");
        request.setPassword("securePassword123");
        request.setRole(UserRole.DEVELOPER);

        UserResponse userResponse = new UserResponse();
        UUID userId = UUID.randomUUID();
        userResponse.setId(userId);
        userResponse.setName("Bob");
        userResponse.setEmail("bob@example.com");
        userResponse.setRole(UserRole.DEVELOPER);

        AuthResponse authResponse = new AuthResponse("token123", "Bearer", 3600000L, userResponse);

        when(authService.register(any(UserCreateRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600000))
                .andExpect(jsonPath("$.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.user.email").value("bob@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register - validation failure returns 400")
    void testRegister_ValidationFailure() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("");
        request.setEmail("invalid-email");
        request.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - duplicate email returns 409")
    void testRegister_DuplicateEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Bob");
        request.setEmail("bob@example.com");
        request.setPassword("securePassword123");
        request.setRole(UserRole.DEVELOPER);

        when(authService.register(any(UserCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("User with email bob@example.com already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("User with email bob@example.com already exists"));
    }

    @Test
    @DisplayName("POST /api/auth/login - successful login returns 200")
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("bob@example.com");
        request.setPassword("securePassword123");

        UserResponse userResponse = new UserResponse();
        UUID userId = UUID.randomUUID();
        userResponse.setId(userId);
        userResponse.setName("Bob");
        userResponse.setEmail("bob@example.com");
        userResponse.setRole(UserRole.DEVELOPER);

        AuthResponse authResponse = new AuthResponse("token123", "Bearer", 3600000L, userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600000))
                .andExpect(jsonPath("$.user.email").value("bob@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login - validation failure returns 400")
    void testLogin_ValidationFailure() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - bad credentials returns 401")
    void testLogin_BadCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("bob@example.com");
        request.setPassword("wrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
