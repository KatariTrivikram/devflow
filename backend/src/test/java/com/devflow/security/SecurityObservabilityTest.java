package com.devflow.security;

import com.devflow.controller.UserController;
import com.devflow.dto.request.UserCreateRequest;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.filter.RequestIdFilter;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        JwtService.class,
        GlobalExceptionHandler.class,
        RequestIdFilter.class
})
@DisplayName("Security Observability & Error Sanitization Test")
class SecurityObservabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private CustomUserDetails developerUser;
    private String developerToken;

    @BeforeEach
    void setUp() {
        developerUser = new CustomUserDetails(
                UUID.randomUUID(),
                "dev@example.com",
                "encodedPassword",
                UserRole.DEVELOPER
        );
        developerToken = jwtService.generateToken(developerUser);
        when(userDetailsService.loadUserByUsername("dev@example.com")).thenReturn(developerUser);
    }

    @Test
    @DisplayName("should return 401 without leaking invalid JWT token in error response")
    void shouldNotLeakInvalidJwtInErrorResponse() throws Exception {
        String invalidJwt = "malformed.or.expired.jwt.token.string";

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidJwt))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Authentication required")))
                .andExpect(jsonPath("$.path", is("/api/users")))
                .andExpect(jsonPath("$.message", not(containsString(invalidJwt))))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    @DisplayName("should preserve existing X-Request-ID even on 401 unauthorized response")
    void shouldPreserveRequestIdOnUnauthorizedResponse() throws Exception {
        String customRequestId = "trace-auth-fail-789";

        mockMvc.perform(get("/api/users")
                        .header("X-Request-ID", customRequestId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-ID", customRequestId));
    }

    @Test
    @DisplayName("should return 403 sanitized response when access denied without leaking internal details")
    void shouldReturnSanitized403OnAccessDenied() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("New User");
        request.setEmail("newuser@example.com");
        request.setPassword("SecretPassword123!");
        request.setRole(UserRole.DEVELOPER);

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + developerToken)
                        .header("X-Request-ID", "trace-forbidden-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", is("Access denied")))
                .andExpect(jsonPath("$.path", is("/api/users")))
                // Ensure request body password was not reflected in error response
                .andExpect(jsonPath("$.message", not(containsString("SecretPassword123!"))))
                .andExpect(header().string("X-Request-ID", "trace-forbidden-123"));
    }
}
