package com.devflow.integration;

import com.devflow.entity.User;
import com.devflow.entity.enums.UserRole;
import com.devflow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("User Repository Integration Test")
class UserRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User createSampleUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encodedPassword123!");
        user.setRole(UserRole.DEVELOPER);
        return user;
    }

    @Test
    @DisplayName("should save user and populate generated id and audit timestamps")
    void shouldSaveAndRetrieveUser() {
        User user = createSampleUser("Alice Johnson", "alice@example.com");

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Alice Johnson");
        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.DEVELOPER);

        Optional<User> found = userRepository.findById(savedUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("should find user by email and check existence")
    void shouldFindByEmailAndCheckExistence() {
        User user = createSampleUser("Bob Smith", "bob@example.com");
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByEmail("bob@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bob Smith");

        boolean exists = userRepository.existsByEmail("bob@example.com");
        assertThat(exists).isTrue();

        boolean nonExistent = userRepository.existsByEmail("nonexistent@example.com");
        assertThat(nonExistent).isFalse();
    }

    @Test
    @DisplayName("should enforce unique email constraint idx_user_email in PostgreSQL")
    void shouldEnforceUniqueEmailConstraint() {
        User user1 = createSampleUser("Charlie Brown", "duplicate@example.com");
        userRepository.saveAndFlush(user1);

        User user2 = createSampleUser("Charlie Clone", "duplicate@example.com");

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
}
