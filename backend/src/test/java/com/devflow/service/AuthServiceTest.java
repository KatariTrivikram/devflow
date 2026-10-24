package com.devflow.service;

import com.devflow.dto.request.LoginRequest;
import com.devflow.dto.request.UserCreateRequest;
import com.devflow.dto.response.AuthResponse;
import com.devflow.dto.response.UserResponse;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.DuplicateResourceException;
import com.devflow.security.CustomUserDetails;
import com.devflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UserCreateRequest registerRequest;
    private LoginRequest loginRequest;
    private UserResponse userResponse;
    private CustomUserDetails userDetails;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        registerRequest = new UserCreateRequest();
        registerRequest.setName("Jane Doe");
        registerRequest.setEmail("jane.doe@example.com");
        registerRequest.setPassword("securePassword123");
        registerRequest.setRole(UserRole.DEVELOPER);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("jane.doe@example.com");
        loginRequest.setPassword("securePassword123");

        userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setName("Jane Doe");
        userResponse.setEmail("jane.doe@example.com");
        userResponse.setRole(UserRole.DEVELOPER);

        userDetails = new CustomUserDetails(userId, "jane.doe@example.com", "encodedPassword", UserRole.DEVELOPER);
    }

    @Test
    @DisplayName("Register creates user, generates JWT, and returns AuthResponse")
    void testRegister_Success() {
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(userResponse);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("jwt.token.here");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse authResponse = authService.register(registerRequest);

        assertNotNull(authResponse);
        assertEquals("jwt.token.here", authResponse.getAccessToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals(3600000L, authResponse.getExpiresIn());
        assertNotNull(authResponse.getUser());
        assertEquals("jane.doe@example.com", authResponse.getUser().getEmail());

        verify(userService, times(1)).createUser(registerRequest);
        verify(jwtService, times(1)).generateToken(any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("Register throws DuplicateResourceException if email is already taken")
    void testRegister_DuplicateEmail() {
        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("User with email jane.doe@example.com already exists"));

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Login authenticates credentials and returns AuthResponse")
    void testLogin_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userService.getUserByEmail("jane.doe@example.com")).thenReturn(userResponse);
        when(jwtService.generateToken(userDetails)).thenReturn("login.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse authResponse = authService.login(loginRequest);

        assertNotNull(authResponse);
        assertEquals("login.jwt.token", authResponse.getAccessToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals(3600000L, authResponse.getExpiresIn());
        assertEquals(userResponse, authResponse.getUser());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService, times(1)).getUserByEmail("jane.doe@example.com");
    }

    @Test
    @DisplayName("Login with invalid credentials throws BadCredentialsException")
    void testLogin_BadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(jwtService, never()).generateToken(any());
    }
}
