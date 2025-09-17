package com.example.user.integration;

import com.example.user.domain.User;
import com.example.user.service.UserServiceImpl;
import com.example.user.service.port.UserRepositoryPort;
import com.example.user.exception.DatabaseException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for UserService using @SpringBootTest.
 * Tests the service layer with real Spring context but mocked repository.
 */
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest {

    @Autowired
    private UserServiceImpl userService;

    @MockBean
    private UserRepositoryPort userRepository;


    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(userRepository);
    }

    @Nested
    @DisplayName("Create User Integration Tests")
    class CreateUserIntegrationTests {

        @Test
        @DisplayName("Should create user successfully with real Spring context")
        void shouldCreateUserSuccessfullyWithRealSpringContext() {
            // Given
            User inputUser = User.builder()
                    .name("Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Integration Test St")
                    .build();

            User savedUser = User.builder()
                    .id("user-123")
                    .name("Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Integration Test St")
                    .build();

            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            User result = userService.create(inputUser);

            // Then
            assertThat(result).isEqualTo(savedUser);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle create user validation failure with real Spring context")
        void shouldHandleCreateUserValidationFailureWithRealSpringContext() {
            // Given
            User invalidUser = User.builder()
                    .name("") // Invalid: empty name
                    .phone("invalid-phone") // Invalid: wrong format
                    .build();

            // When & Then
            assertThatThrownBy(() -> userService.create(invalidUser))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessageContaining("Failed to create user in database");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle create user database failure with real Spring context")
        void shouldHandleCreateUserDatabaseFailureWithRealSpringContext() {
            // Given
            User inputUser = User.builder()
                    .name("Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Integration Test St")
                    .build();

            DatabaseException cause = new DatabaseException("Database connection failed", "create", new RuntimeException("Connection failed"));
            when(userRepository.save(any(User.class))).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.create(inputUser))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessage("Database connection failed")
                    .hasCause(cause);

            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Get User Integration Tests")
    class GetUserIntegrationTests {

        @Test
        @DisplayName("Should get user successfully with real Spring context")
        void shouldGetUserSuccessfullyWithRealSpringContext() {
            // Given
            String userId = "user-123";
            User expectedUser = User.builder()
                    .id(userId)
                    .name("Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Integration Test St")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

            // When
            Optional<User> result = userService.getById(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedUser);
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("Should return empty when user not found with real Spring context")
        void shouldReturnEmptyWhenUserNotFoundWithRealSpringContext() {
            // Given
            String userId = "non-existent-user";
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.getById(userId);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("Should handle get user database failure with real Spring context")
        void shouldHandleGetUserDatabaseFailureWithRealSpringContext() {
            // Given
            String userId = "user-123";
            DatabaseException cause = new DatabaseException("Database query failed", "getById", new RuntimeException("Query failed"));
            when(userRepository.findById(userId)).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.getById(userId))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessage("Database query failed")
                    .hasCause(cause);

            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("Update User Integration Tests")
    class UpdateUserIntegrationTests {

        @Test
        @DisplayName("Should update user successfully with real Spring context")
        void shouldUpdateUserSuccessfullyWithRealSpringContext() {
            // Given
            User inputUser = User.builder()
                    .id("user-123")
                    .name("Updated Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("456 Updated Integration Test Ave")
                    .build();

            User updatedUser = User.builder()
                    .id("user-123")
                    .name("Updated Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("456 Updated Integration Test Ave")
                    .build();

            when(userRepository.save(any(User.class))).thenReturn(updatedUser);

            // When
            User result = userService.update(inputUser);

            // Then
            assertThat(result).isEqualTo(updatedUser);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle update user validation failure with real Spring context")
        void shouldHandleUpdateUserValidationFailureWithRealSpringContext() {
            // Given
            User invalidUser = User.builder()
                    .id("user-123")
                    .name("") // Invalid: empty name
                    .phone("invalid-phone") // Invalid: wrong format
                    .build();

            // When & Then
            assertThatThrownBy(() -> userService.update(invalidUser))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessageContaining("Failed to update user in database");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Delete User Integration Tests")
    class DeleteUserIntegrationTests {

        @Test
        @DisplayName("Should delete user successfully with real Spring context")
        void shouldDeleteUserSuccessfullyWithRealSpringContext() {
            // Given
            String userId = "user-123";
            doNothing().when(userRepository).deleteById(userId);

            // When
            assertThatCode(() -> userService.delete(userId))
                    .doesNotThrowAnyException();

            // Then
            verify(userRepository).deleteById(userId);
        }

        @Test
        @DisplayName("Should handle delete user database failure with real Spring context")
        void shouldHandleDeleteUserDatabaseFailureWithRealSpringContext() {
            // Given
            String userId = "user-123";
            DatabaseException cause = new DatabaseException("Database delete failed", "delete", new RuntimeException("Delete failed"));
            doThrow(cause).when(userRepository).deleteById(userId);

            // When & Then
            assertThatThrownBy(() -> userService.delete(userId))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessage("Database delete failed")
                    .hasCause(cause);

            verify(userRepository).deleteById(userId);
        }
    }

    
}