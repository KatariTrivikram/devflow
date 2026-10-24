package com.devflow.exception;

import com.devflow.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Global Exception Handler Logging & Sanitization Test")
class GlobalExceptionHandlerLoggingTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/sensitive-operation");
    }

    @Test
    @DisplayName("should sanitize unexpected internal server error and prevent leaking sensitive exception message")
    void shouldSanitizeGenericExceptionResponse() {
        String sensitiveInternalMessage = "Fatal database connection drop: host=db.internal; secret_pass=supersecret!";
        Exception unexpectedException = new RuntimeException(sensitiveInternalMessage);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(unexpectedException, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected internal server error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("secret_pass");
        assertThat(response.getBody().getMessage()).doesNotContain(sensitiveInternalMessage);
        assertThat(response.getBody().getPath()).isEqualTo("/api/sensitive-operation");
    }

    @Test
    @DisplayName("should return sanitized 401 error response for BadCredentialsException")
    void shouldSanitizeBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials for user test@example.com with raw password hash");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentialsException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
        assertThat(response.getBody().getMessage()).doesNotContain("raw password hash");
    }

    @Test
    @DisplayName("should return sanitized 403 error response for AccessDeniedException")
    void shouldSanitizeAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied: role DEVELOPER cannot execute admin function");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDeniedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("should return 400 error response for HttpMessageNotReadableException with sanitized message")
    void shouldHandleHttpMessageNotReadableException() {
        org.springframework.http.converter.HttpMessageNotReadableException ex =
                new org.springframework.http.converter.HttpMessageNotReadableException(
                        "JSON parse error: Cannot deserialize value of type `com.devflow.entity.enums.IssueStatus` from String \"INVALID_STATUS\"",
                        (org.springframework.http.HttpInputMessage) null
                );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadableException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Malformed JSON request or invalid field format");
        assertThat(response.getBody().getPath()).isEqualTo("/api/sensitive-operation");
    }

    @Test
    @DisplayName("should return 400 error response for MissingServletRequestParameterException")
    void shouldHandleMissingServletRequestParameterException() {
        org.springframework.web.bind.MissingServletRequestParameterException ex =
                new org.springframework.web.bind.MissingServletRequestParameterException("projectId", "UUID");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingServletRequestParameterException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Required request parameter 'projectId' is missing");
    }

    @Test
    @DisplayName("should return 405 error response for HttpRequestMethodNotSupportedException")
    void shouldHandleHttpRequestMethodNotSupportedException() {
        org.springframework.web.HttpRequestMethodNotSupportedException ex =
                new org.springframework.web.HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpRequestMethodNotSupportedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(405);
        assertThat(response.getBody().getMessage()).isEqualTo("HTTP method 'PATCH' is not supported for this endpoint");
    }

    @Test
    @DisplayName("should return sanitized 409 error response for DataIntegrityViolationException")
    void shouldHandleDataIntegrityViolationException() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException(
                        "ERROR: duplicate key value violates unique constraint \"uk_users_email\" Detail: Key (email)=(test@example.com) already exists."
                );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo("Data integrity constraint violation occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("uk_users_email");
        assertThat(response.getBody().getMessage()).doesNotContain("test@example.com");
    }

    @Test
    @DisplayName("should return 404 error response for NoResourceFoundException")
    void shouldHandleNoResourceFoundException() {
        org.springframework.web.servlet.resource.NoResourceFoundException ex =
                new org.springframework.web.servlet.resource.NoResourceFoundException(
                        org.springframework.http.HttpMethod.GET, "/unknown/endpoint"
                );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found: /unknown/endpoint");
    }

    @Test
    @DisplayName("should return 400 error response with field errors for ConstraintViolationException")
    void shouldHandleConstraintViolationException() {
        jakarta.validation.ConstraintViolation<?> violation = org.mockito.Mockito.mock(jakarta.validation.ConstraintViolation.class);
        jakarta.validation.Path path = org.mockito.Mockito.mock(jakarta.validation.Path.class);
        org.mockito.Mockito.when(path.toString()).thenReturn("page");
        org.mockito.Mockito.when(violation.getPropertyPath()).thenReturn(path);
        org.mockito.Mockito.when(violation.getMessage()).thenReturn("must be greater than or equal to 0");

        jakarta.validation.ConstraintViolationException ex =
                new jakarta.validation.ConstraintViolationException("Constraint violation", java.util.Set.of(violation));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getErrors()).containsEntry("page", "must be greater than or equal to 0");
    }
}
