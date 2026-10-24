package com.devflow.security;

import com.devflow.entity.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    private SecurityUtils securityUtils;
    private UUID userId;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        securityUtils = new SecurityUtils();
        userId = UUID.randomUUID();
        userDetails = new CustomUserDetails(userId, "alex@example.com", "password", UserRole.PROJECT_MANAGER);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Extract authenticated user details")
    void testGetCurrentUserDetails_Success() {
        Optional<CustomUserDetails> detailsOpt = securityUtils.getCurrentUserDetails();

        assertTrue(detailsOpt.isPresent());
        assertEquals(userId, detailsOpt.get().getId());
        assertEquals("alex@example.com", detailsOpt.get().getUsername());
        assertEquals(UserRole.PROJECT_MANAGER, detailsOpt.get().getRole());
    }

    @Test
    @DisplayName("Extract current user ID, email, and role")
    void testGetCurrentUserProperties() {
        assertEquals(userId, securityUtils.getCurrentUserId());
        assertEquals("alex@example.com", securityUtils.getCurrentUserEmail());
        assertEquals(UserRole.PROJECT_MANAGER, securityUtils.getCurrentUserRole());
        assertTrue(securityUtils.isAuthenticated());
    }

    @Test
    @DisplayName("isAuthenticated returns false when security context is empty")
    void testIsAuthenticated_EmptyContext() {
        SecurityContextHolder.clearContext();

        assertFalse(securityUtils.isAuthenticated());
        assertTrue(securityUtils.getCurrentUserDetails().isEmpty());
    }

    @Test
    @DisplayName("getAuthenticatedUser throws AccessDeniedException when unauthenticated")
    void testGetAuthenticatedUser_Unauthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class, () -> securityUtils.getAuthenticatedUser());
        assertThrows(AccessDeniedException.class, () -> securityUtils.getCurrentUserId());
    }
}
