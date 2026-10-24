package com.devflow.config;

import com.devflow.controller.ActivityController;
import com.devflow.controller.AuthController;
import com.devflow.controller.CommentController;
import com.devflow.controller.IssueController;
import com.devflow.controller.ProjectController;
import com.devflow.controller.ProjectMemberController;
import com.devflow.controller.SprintController;
import com.devflow.controller.UserController;
import com.devflow.exception.GlobalExceptionHandler;
import com.devflow.security.CustomAccessDeniedHandler;
import com.devflow.security.CustomAuthenticationEntryPoint;
import com.devflow.security.CustomUserDetailsService;
import com.devflow.security.JwtService;
import com.devflow.security.SecurityConfig;
import com.devflow.service.ActivityService;
import com.devflow.service.AuthService;
import com.devflow.service.CommentService;
import com.devflow.service.IssueService;
import com.devflow.service.ProjectMemberService;
import com.devflow.service.ProjectService;
import com.devflow.service.SprintService;
import com.devflow.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        UserController.class,
        ProjectController.class,
        ProjectMemberController.class,
        SprintController.class,
        IssueController.class,
        CommentController.class,
        ActivityController.class
})
@ImportAutoConfiguration({
        org.springdoc.core.configuration.SpringDocConfiguration.class,
        org.springdoc.core.properties.SpringDocConfigProperties.class,
        org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration.class,
        org.springdoc.core.configuration.SpringDocUIConfiguration.class,
        org.springdoc.webmvc.ui.SwaggerConfig.class,
        org.springdoc.core.properties.SwaggerUiConfigProperties.class,
        org.springdoc.core.properties.SwaggerUiConfigParameters.class,
        org.springdoc.core.properties.SwaggerUiOAuthProperties.class,
        JacksonAutoConfiguration.class
})
@Import({
        OpenApiConfig.class,
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        JwtService.class,
        GlobalExceptionHandler.class
})
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ProjectMemberService projectMemberService;

    @MockBean
    private SprintService sprintService;

    @MockBean
    private IssueService issueService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private ActivityService activityService;

    @Test
    @DisplayName("Should serve OpenAPI JSON specification at /v3/api-docs without authentication")
    void shouldServeOpenApiSpecWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi", containsString("3.")))
                .andExpect(jsonPath("$.info.title", is("DevFlow API")))
                .andExpect(jsonPath("$.info.version", is("1.0.0")))
                .andExpect(jsonPath("$.info.description", containsString("DevFlow software development project")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth", notNullValue()))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type", is("http")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme", is("bearer")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat", is("JWT")));
    }

    @Test
    @DisplayName("OpenAPI JSON specification contains all expected controller paths and schemas")
    void openApiSpecShouldContainExpectedPathsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // Paths
                .andExpect(jsonPath("$.paths['/api/auth/register'].post", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/users'].post", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/projects'].get", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/projects'].post", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/issues'].get", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/issues'].post", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/sprints'].post", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/comments'].post", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/activities'].post", notNullValue()))
                // Schemas
                .andExpect(jsonPath("$.components.schemas.ErrorResponse", notNullValue()))
                .andExpect(jsonPath("$.components.schemas.UserCreateRequest", notNullValue()))
                .andExpect(jsonPath("$.components.schemas.LoginRequest", notNullValue()))
                .andExpect(jsonPath("$.components.schemas.ProjectCreateRequest", notNullValue()))
                .andExpect(jsonPath("$.components.schemas.IssueCreateRequest", notNullValue()))
                .andExpect(jsonPath("$.components.schemas.IssueResponse", notNullValue()));
    }

    @Test
    @DisplayName("Authentication endpoints in OpenAPI spec should not require security")
    void authEndpointsShouldNotRequireSecurityInSpec() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.security", empty()))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security", empty()));
    }

    @Test
    @DisplayName("Should serve Swagger UI HTML page at /swagger-ui/index.html without authentication")
    void shouldServeSwaggerUiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    @DisplayName("Should redirect /swagger-ui.html to Swagger UI index without authentication")
    void shouldRedirectSwaggerUiHtml() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui")));
    }
}
