package com.devflow.controller;

import com.devflow.dto.request.LoginRequest;
import com.devflow.dto.request.UserCreateRequest;
import com.devflow.dto.response.AuthResponse;
import com.devflow.dto.response.PageResponse;
import com.devflow.dto.response.ProjectResponse;
import com.devflow.dto.response.UserResponse;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.security.CustomAccessDeniedHandler;
import com.devflow.security.CustomAuthenticationEntryPoint;
import com.devflow.security.CustomUserDetails;
import com.devflow.security.CustomUserDetailsService;
import com.devflow.security.JwtService;
import com.devflow.security.SecurityConfig;
import com.devflow.service.AuthService;
import com.devflow.service.ProjectService;
import com.devflow.service.SprintService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        ProjectController.class,
        UserController.class
})
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        JwtService.class,
        GlobalExceptionHandler.class
})
@DisplayName("Backend API Quality & Production Hardening Regression Tests")
class ApiHardeningRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AuthService authService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private SprintService sprintService;

    @MockBean
    private UserService userService;

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
                adminId, "admin@example.com", "hashedPassword", UserRole.ADMIN
        );
        CustomUserDetails devUserDetails = new CustomUserDetails(
                devId, "dev@example.com", "hashedPassword", UserRole.DEVELOPER
        );

        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(adminUserDetails);
        when(userDetailsService.loadUserByUsername("dev@example.com")).thenReturn(devUserDetails);

        adminToken = jwtService.generateToken(adminUserDetails);
        developerToken = jwtService.generateToken(devUserDetails);
    }

    @Test
    @DisplayName("should return 400 Bad Request when request body contains malformed JSON syntax")
    void testMalformedJson_Returns400() throws Exception {
        String invalidJson = "{ \"email\": \"user@example.com\", \"password\": ";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid field format"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when request body contains unparseable enum constant")
    void testInvalidEnumInRequestBody_Returns400() throws Exception {
        String invalidEnumJson = """
                {
                    "name": "Jane Doe",
                    "email": "jane@example.com",
                    "password": "Password123!",
                    "role": "SUPER_ADMIN_INVALID"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEnumJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid field format"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    @DisplayName("should return 405 Method Not Allowed when endpoint is invoked with unsupported HTTP method")
    void testUnsupportedHttpMethod_Returns405() throws Exception {
        mockMvc.perform(patch("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value(containsString("is not supported for this endpoint")))
                .andExpect(jsonPath("$.path").value("/api/projects"));
    }

    @Test
    @DisplayName("should return 400 Bad Request with field errors when required fields are missing")
    void testValidationFailure_Returns400WithErrors() throws Exception {
        LoginRequest emptyRequest = new LoginRequest();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    @DisplayName("should never return password or sensitive hash fields in AuthResponse")
    void testAuthResponse_NeverLeaksPassword() throws Exception {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(devId);
        userResponse.setName("Jane Developer");
        userResponse.setEmail("dev@example.com");
        userResponse.setRole(UserRole.DEVELOPER);
        userResponse.setCreatedAt(Instant.now());
        userResponse.setUpdatedAt(Instant.now());

        AuthResponse authResponse = new AuthResponse("mock.jwt.token", "Bearer", 3600000, userResponse);
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("dev@example.com");
        loginRequest.setPassword("ValidPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value(devId.toString()))
                .andExpect(jsonPath("$.user.email").value("dev@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("should return 400 Bad Request when pagination page is negative")
    void testInvalidPagination_NegativePage_Returns400() throws Exception {
        when(projectService.getProjects(any(), any(), eq(-1), anyInt(), any()))
                .thenThrow(new BusinessRuleException("Page index must not be less than zero"));

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when pagination size exceeds 100")
    void testInvalidPagination_SizeExceedsMax_Returns400() throws Exception {
        when(projectService.getProjects(any(), any(), anyInt(), eq(150), any()))
                .thenThrow(new BusinessRuleException("Page size must be between 1 and 100"));

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken)
                        .param("size", "150"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when sorting by an unallowed property")
    void testInvalidSort_UnallowedProperty_Returns400() throws Exception {
        when(projectService.getProjects(any(), any(), anyInt(), anyInt(), eq("password,asc")))
                .thenThrow(new BusinessRuleException("Invalid sort property: password. Allowed fields: [createdAt, updatedAt, name, status]"));

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken)
                        .param("sort", "password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort property: password")));
    }

    @Test
    @DisplayName("should return 400 Bad Request when sorting with an invalid direction")
    void testInvalidSort_InvalidDirection_Returns400() throws Exception {
        when(projectService.getProjects(any(), any(), anyInt(), anyInt(), eq("name,sideways")))
                .thenThrow(new BusinessRuleException("Invalid sort direction: sideways. Must be 'asc' or 'desc'"));

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken)
                        .param("sort", "name,sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort direction: sideways")));
    }

    @Test
    @DisplayName("should return 401 Unauthorized when accessing protected endpoint without authentication")
    void testUnauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("should return 403 Forbidden when non-admin accesses admin-restricted endpoint")
    void testRbac_DeveloperForbiddenFromAdminEndpoint_Returns403() throws Exception {
        UserCreateRequest adminRequest = new UserCreateRequest();
        adminRequest.setName("New User");
        adminRequest.setEmail("newuser@example.com");
        adminRequest.setPassword("SecurePassword123!");
        adminRequest.setRole(UserRole.DEVELOPER);

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when an IllegalArgumentException is thrown")
    void testIllegalArgumentException_Returns400() throws Exception {
        when(projectService.getProjects(any(), any(), anyInt(), anyInt(), any()))
                .thenThrow(new IllegalArgumentException("Illegal argument provided"));

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Illegal argument provided"));
    }

    @Test
    @DisplayName("should handle CORS preflight requests with configured origin and headers")
    void testCorsPreflight_AllowedOrigin_ReturnsOkWithHeaders() throws Exception {
        mockMvc.perform(options("/api/projects")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }
}
