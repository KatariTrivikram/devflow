package com.devflow.security;

import com.devflow.entity.enums.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityUtils {

    public Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    public boolean isAuthenticated() {
        return getCurrentUserDetails().isPresent();
    }

    public CustomUserDetails getAuthenticatedUser() {
        return getCurrentUserDetails()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));
    }

    public UUID getCurrentUserId() {
        return getAuthenticatedUser().getId();
    }

    public String getCurrentUserEmail() {
        return getAuthenticatedUser().getUsername();
    }

    public UserRole getCurrentUserRole() {
        return getAuthenticatedUser().getRole();
    }
}
