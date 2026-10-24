package com.devflow.security;

import com.devflow.controller.AuthController;
import com.devflow.controller.UserController;
import com.devflow.dto.request.LoginRequest;
import com.devflow.dto.request.UserCreateRequest;
import com.devflow.dto.request.UserUpdateRequest;
import com.devflow.dto.response.AuthResponse;
import com.devflow.dto.response.UserResponse;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.service.AuthService;
import com.devflow.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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

@WebMvcTest(controllers = {UserController.class, AuthController.class})
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        JwtService.class,
        GlobalExceptionHandler.class
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private String adminToken;
    private String developerToken;
    private UUID adminId;
    private UUID devId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        devId = UUID.randomUUID();

        CustomUserDetails adminUserDetails = new CustomUserDetails(
                adminId, "admin@example.com", "password", UserRole.ADMIN
        );
        CustomUserDetails devUserDetails = new CustomUserDetails(
                devId, "developer@example.com", "password", UserRole.DEVELOPER
        );

        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(adminUserDetails);
        when(userDetailsService.loadUserByUsername("developer@example.com")).thenReturn(devUserDetails);

        adminToken = jwtService.generateToken(adminUserDetails);
        developerToken = jwtService.generateToken(devUserDetails);
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 with ErrorResponse")
    void testUnauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/{id}", devId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.path").value("/api/users/" + devId));
    }

    @Test
    @DisplayName("Invalid Bearer token returns 401 with ErrorResponse")
    void testInvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/{id}", devId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.path").value("/api/users/" + devId));
    }

    @Test
    @DisplayName("Authenticated request with valid token reaches protected GET /api/users/{id}")
    void testAuthenticatedRequest_Success() throws Exception {
        UserResponse response = new UserResponse();
        response.setId(devId);
        response.setName("Dev User");
        response.setEmail("developer@example.com");
        response.setRole(UserRole.DEVELOPER);

        when(userService.getUserById(devId)).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", devId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(devId.toString()))
                .andExpect(jsonPath("$.email").value("developer@example.com"));
    }

    @Test
    @DisplayName("RBAC: DEVELOPER token calling POST /api/users returns 403 Forbidden")
    void testRbac_DeveloperForbiddenFromCreatingUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("New User");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.DEVELOPER);

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    @DisplayName("RBAC: DEVELOPER token calling PUT /api/users/{id} returns 403 Forbidden")
    void testRbac_DeveloperForbiddenFromUpdatingUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Updated Name");

        mockMvc.perform(put("/api/users/{id}", devId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/users/" + devId));
    }

    @Test
    @DisplayName("RBAC: ADMIN token calling POST /api/users succeeds with 201 Created")
    void testRbac_AdminAllowedToCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("New Dev");
        request.setEmail("newdev@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.DEVELOPER);

        UserResponse response = new UserResponse();
        UUID newId = UUID.randomUUID();
        response.setId(newId);
        response.setName("New Dev");
        response.setEmail("newdev@example.com");
        response.setRole(UserRole.DEVELOPER);

        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(newId.toString()))
                .andExpect(jsonPath("$.name").value("New Dev"));
    }

    @Test
    @DisplayName("RBAC: ADMIN token calling PUT /api/users/{id} succeeds with 200 OK")
    void testRbac_AdminAllowedToUpdateUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Updated Alice");

        UserResponse response = new UserResponse();
        response.setId(devId);
        response.setName("Updated Alice");
        response.setEmail("developer@example.com");
        response.setRole(UserRole.DEVELOPER);

        when(userService.updateUser(eq(devId), any(UserUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/{id}", devId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(devId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Alice"));
    }

    @Test
    @DisplayName("Public endpoint POST /api/auth/login is accessible without authentication")
    void testPublicEndpoint_AccessibleWithoutToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("dev@example.com");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse("token", "Bearer", 3600000L, null);
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"));
    }
}
