package com.devflow.security;

import com.devflow.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET_KEY = "0123456789012345678901234567890123456789012345678901234567890123";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;
    private CustomUserDetails userDetails;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_KEY, EXPIRATION_MS);
        userId = UUID.randomUUID();
        userDetails = new CustomUserDetails(userId, "john.doe@example.com", "password", UserRole.DEVELOPER);
    }

    @Test
    @DisplayName("Generate token and extract username")
    void testGenerateTokenAndExtractUsername() {
        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("john.doe@example.com", jwtService.extractUsername(token));
    }

    @Test
    @DisplayName("Extract custom claims (userId and role)")
    void testExtractCustomClaims() {
        String token = jwtService.generateToken(userDetails);

        String extractedUserId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
        String extractedRole = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

        assertEquals(userId.toString(), extractedUserId);
        assertEquals(UserRole.DEVELOPER.name(), extractedRole);
    }

    @Test
    @DisplayName("Validate token with matching user details")
    void testIsTokenValid_Success() {
        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Validate token with different user details fails")
    void testIsTokenValid_MismatchedUser() {
        String token = jwtService.generateToken(userDetails);
        CustomUserDetails differentUser = new CustomUserDetails(
                UUID.randomUUID(), "other@example.com", "password", UserRole.ADMIN
        );

        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    @DisplayName("Token validation returns false for tampered token")
    void testIsTokenValid_TamperedToken() {
        String token = jwtService.generateToken(userDetails);
        String tamperedToken = token + "corrupted";

        assertFalse(jwtService.isTokenValid(tamperedToken, userDetails));
    }

    @Test
    @DisplayName("Expired token is recognized as expired and invalid")
    void testExpiredToken() {
        JwtService expiredJwtService = new JwtService(SECRET_KEY, -1000); // Expired 1 second ago
        String expiredToken = expiredJwtService.generateToken(userDetails);

        assertTrue(expiredJwtService.isTokenExpired(expiredToken));
        assertFalse(expiredJwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    @DisplayName("Secret key with fewer than 32 characters throws IllegalArgumentException")
    void testWeakSecretThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("short-secret-key", EXPIRATION_MS));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(null, EXPIRATION_MS));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("   ", EXPIRATION_MS));
    }
}
