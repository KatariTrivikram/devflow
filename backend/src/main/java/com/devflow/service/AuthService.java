package com.devflow.service;

import com.devflow.dto.request.LoginRequest;
import com.devflow.dto.request.UserCreateRequest;
import com.devflow.dto.response.AuthResponse;
import com.devflow.dto.response.UserResponse;
import com.devflow.security.CustomUserDetails;
import com.devflow.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(UserCreateRequest request) {
        UserResponse userResponse = userService.createUser(request);

        CustomUserDetails userDetails = new CustomUserDetails(
                userResponse.getId(),
                userResponse.getEmail(),
                "",
                userResponse.getRole()
        );

        String token = jwtService.generateToken(userDetails);
        log.info("User registered successfully: userId={}, email={}", userResponse.getId(), userResponse.getEmail());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), userResponse);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserResponse userResponse = userService.getUserByEmail(userDetails.getUsername());
        String token = jwtService.generateToken(userDetails);

        log.info("User logged in successfully: userId={}, email={}", userDetails.getId(), userDetails.getUsername());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), userResponse);
    }
}
