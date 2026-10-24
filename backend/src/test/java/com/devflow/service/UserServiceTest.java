package com.devflow.service;

import com.devflow.dto.request.UserCreateRequest;
import com.devflow.dto.request.UserUpdateRequest;
import com.devflow.dto.response.UserResponse;
import com.devflow.entity.User;
import com.devflow.entity.enums.UserRole;
import com.devflow.exception.DuplicateResourceException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.mapper.UserMapper;
import com.devflow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("createUser - success")
    void testCreateUser_Success() {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("John");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.DEVELOPER);

        User user = new User();
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());

        UserResponse response = new UserResponse();
        response.setId(savedUser.getId());
        response.setEmail("john@example.com");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(response.getId(), result.getId());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("createUser - duplicate email throws DuplicateResourceException")
    void testCreateUser_DuplicateEmail() {
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail("john@example.com");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("getUserById - not found throws ResourceNotFoundException")
    void testGetUserById_NotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(id));
    }

    @Test
    @DisplayName("updateUser - success")
    void testUpdateUser_Success() {
        UUID id = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("John Updated");

        User user = new User();
        user.setId(id);

        UserResponse response = new UserResponse();
        response.setId(id);
        response.setName("John Updated");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.updateUser(id, request);

        assertNotNull(result);
        assertEquals("John Updated", result.getName());
        verify(userMapper).updateEntity(request, user);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("existsByEmail - returns repository result")
    void testExistsByEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        assertTrue(userService.existsByEmail("test@example.com"));
    }
}
