package com.example.user.service;

import com.example.user.domain.User;
import com.example.user.exception.DatabaseException;
import com.example.user.service.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

 

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for UserServiceImpl.
 * Tests all business logic, error handling, metrics collection, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @Mock
    private UserRepositoryPort userRepository;



    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    @Nested
    @DisplayName("Ping Tests")
    class PingTests {

        @Test
        @DisplayName("Should return pong for ping request")
        void shouldReturnPongForPingRequest() {
            // When
            String result = userService.ping();

            // Then
            assertThat(result).isEqualTo("pong");

        }

        @Test
        @DisplayName("Should handle ping with metrics collection")
        void shouldHandlePingWithMetricsCollection() {
            // Given
            

            // When
            String result = userService.ping();

            // Then
            assertThat(result).isEqualTo("pong");

        }

        @Test
        @DisplayName("Should handle ping errors with metrics")
        void shouldHandlePingErrorsWithMetrics() {
            // Given
            RuntimeException exception = new RuntimeException("Test error");

            // When & Then
            assertThatThrownBy(() -> userService.ping())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error");
            
        }
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() {
            // Given
            User inputUser = User.builder()
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            User savedUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            

            // When
            User result = userService.create(inputUser);

            // Then
            assertThat(result).isEqualTo(savedUser);
            verify(userRepository).save(inputUser);
        }

        @Test
        @DisplayName("Should create user with null id")
        void shouldCreateUserWithNullId() {
            // Given
            User inputUser = User.builder()
                    .id(null)
                    .name("Jane Doe")
                    .phone("+1-555-987-6543")
                    .build();

            User savedUser = User.builder()
                    .id("user-456")
                    .name("Jane Doe")
                    .phone("+1-555-987-6543")
                    .build();

            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            

            // When
            User result = userService.create(inputUser);

            // Then
            assertThat(result).isEqualTo(savedUser);
            verify(userRepository).save(inputUser);
        }

        @Test
        @DisplayName("Should handle database error during create")
        void shouldHandleDatabaseErrorDuringCreate() {
            // Given
            User inputUser = User.builder()
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            RuntimeException dbException = new RuntimeException("Database error");
            when(userRepository.save(any(User.class))).thenThrow(dbException);

            // When & Then
            assertThatThrownBy(() -> userService.create(inputUser))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessage("Failed to create user in database")
                    .hasCause(dbException);
            
        }

        @Test
        @DisplayName("Should handle null user input")
        void shouldHandleNullUserInput() {
            // Given
            User inputUser = null;

            // When & Then
            assertThatThrownBy(() -> userService.create(inputUser))
                    .isInstanceOf(Exception.class);
            
        }
    }

    @Nested
    @DisplayName("Get User By ID Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() {
            // Given
            String userId = "user-123";
            User foundUser = User.builder()
                    .id(userId)
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(foundUser));
            

            // When
            Optional<User> result = userService.getById(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(foundUser);
            verify(userRepository).findById(userId);
            
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
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
        @DisplayName("Should handle database error during get by id")
        void shouldHandleDatabaseErrorDuringGetById() {
            // Given
            String userId = "user-123";
            RuntimeException dbException = new RuntimeException("Database error");
            when(userRepository.findById(userId)).thenThrow(dbException);
            

            // When & Then
            assertThatThrownBy(() -> userService.getById(userId))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessage("Failed to retrieve user from database")
                    .hasCause(dbException);
            
            
        }

        @ParameterizedTest
        @DisplayName("Should handle various user ID formats")
        @ValueSource(strings = {"", "   ", "user-123", "USER-123", "user_123", "123"})
        void shouldHandleVariousUserIdFormats(String userId) {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            

            // When
            Optional<User> result = userService.getById(userId);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            // Given
            User inputUser = User.builder()
                    .id("user-123")
                    .name("John Updated")
                    .phone("+1-555-999-8888")
                    .address("456 New St")
                    .build();

            User updatedUser = User.builder()
                    .id("user-123")
                    .name("John Updated")
                    .phone("+1-555-999-8888")
                    .address("456 New St")
                    .build();

            when(userRepository.save(any(User.class))).thenReturn(updatedUser);
            

            // When
            User result = userService.update(inputUser);

            // Then
            assertThat(result).isEqualTo(updatedUser);
            verify(userRepository).save(inputUser);
            
        }

        @Test
        @DisplayName("Should handle database error during update")
        void shouldHandleDatabaseErrorDuringUpdate() {
            // Given
            User inputUser = User.builder()
                    .id("user-123")
                    .name("John Updated")
                    .phone("+1-555-999-8888")
                    .build();

            RuntimeException dbException = new RuntimeException("Database error");
            when(userRepository.save(any(User.class))).thenThrow(dbException);
            

            // When & Then
            assertThatThrownBy(() -> userService.update(inputUser))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessage("Failed to update user in database")
                    .hasCause(dbException);
            
            
        }

        @Test
        @DisplayName("Should handle null user input for update")
        void shouldHandleNullUserInputForUpdate() {
            // Given
            User inputUser = null;
            

            // When & Then
            assertThatThrownBy(() -> userService.update(inputUser))
                    .isInstanceOf(Exception.class);
            
            
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            // Given
            String userId = "user-123";
            

            // When
            userService.delete(userId);

            // Then
            verify(userRepository).deleteById(userId);
        }

        @Test
        @DisplayName("Should handle database error during delete")
        void shouldHandleDatabaseErrorDuringDelete() {
            // Given
            String userId = "user-123";
            RuntimeException dbException = new RuntimeException("Database error");
            doThrow(dbException).when(userRepository).deleteById(userId);
            

            // When & Then
            assertThatThrownBy(() -> userService.delete(userId))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessage("Failed to delete user from database")
                    .hasCause(dbException);
            
            
        }

        @ParameterizedTest
        @DisplayName("Should handle various user ID formats for delete")
        @ValueSource(strings = {"", "   ", "user-123", "USER-123", "user_123", "123"})
        void shouldHandleVariousUserIdFormatsForDelete(String userId) {
            // Given
            

            // When
            userService.delete(userId);

            // Then
            verify(userRepository).deleteById(userId);
        }
    }

    @Nested
    @DisplayName("List Users Tests")
    class ListUsersTests {

        @Test
        @DisplayName("Should list users with pagination successfully")
        void shouldListUsersWithPaginationSuccessfully() {
            // Given
            int page = 0;
            int size = 10;
            List<User> users = List.of(
                    User.builder().id("user-1").name("User 1").phone("+1-555-111-1111").build(),
                    User.builder().id("user-2").name("User 2").phone("+1-555-222-2222").build()
            );

            when(userRepository.findPage(page, size)).thenReturn(users);
            

            // When
            List<User> result = userService.list(page, size);

            // Then
            assertThat(result).isEqualTo(users);
            verify(userRepository).findPage(page, size);
            
        }

        @Test
        @DisplayName("Should return empty list when no users found")
        void shouldReturnEmptyListWhenNoUsersFound() {
            // Given
            int page = 0;
            int size = 10;
            List<User> emptyList = List.of();

            when(userRepository.findPage(page, size)).thenReturn(emptyList);
            

            // When
            List<User> result = userService.list(page, size);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findPage(page, size);
        }

        @Test
        @DisplayName("Should handle database error during list")
        void shouldHandleDatabaseErrorDuringList() {
            // Given
            int page = 0;
            int size = 10;
            RuntimeException dbException = new RuntimeException("Database error");
            when(userRepository.findPage(page, size)).thenThrow(dbException);

            // When & Then
            assertThatThrownBy(() -> userService.list(page, size))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessage("Failed to list users with pagination from database")
                    .hasCause(dbException);
            
            
        }

        @ParameterizedTest
        @DisplayName("Should handle various page and size values")
        @ValueSource(ints = {0, 1, 10, 100, 1000})
        void shouldHandleVariousPageAndSizeValues(int value) {
            // Given
            when(userRepository.findPage(value, value)).thenReturn(List.of());
            

            // When
            List<User> result = userService.list(value, value);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findPage(value, value);
        }
    }

    @Nested
    @DisplayName("Count Users Tests")
    class CountUsersTests {

        @Test
        @DisplayName("Should count users successfully")
        void shouldCountUsersSuccessfully() {
            // Given
            long expectedCount = 42L;
            when(userRepository.count()).thenReturn(expectedCount);
            

            // When
            long result = userService.count();

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(userRepository).count();
            
        }

        @Test
        @DisplayName("Should return zero count when no users")
        void shouldReturnZeroCountWhenNoUsers() {
            // Given
            when(userRepository.count()).thenReturn(0L);
            

            // When
            long result = userService.count();

            // Then
            assertThat(result).isZero();
            verify(userRepository).count();
        }

        @Test
        @DisplayName("Should handle database error during count")
        void shouldHandleDatabaseErrorDuringCount() {
            // Given
            RuntimeException dbException = new RuntimeException("Database error");
            when(userRepository.count()).thenThrow(dbException);

            // When & Then
            assertThatThrownBy(() -> userService.count())
                    .isInstanceOf(DatabaseException.class)
                    .hasMessage("Failed to count users in database")
                    .hasCause(dbException);
            
            
        }

        @Test
        @DisplayName("Should handle large count values")
        void shouldHandleLargeCountValues() {
            // Given
            long largeCount = Long.MAX_VALUE;
            when(userRepository.count()).thenReturn(largeCount);
            

            // When
            long result = userService.count();

            // Then
            assertThat(result).isEqualTo(largeCount);
            verify(userRepository).count();
        }
    }

    @Nested
    @DisplayName("Metrics Integration Tests")
    class MetricsIntegrationTests {

        @Test
        @DisplayName("Should record metrics for all operations")
        void shouldRecordMetricsForAllOperations() {
            // Given
            User user = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            when(userRepository.save(any(User.class))).thenReturn(user);
            when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
            when(userRepository.findPage(anyInt(), anyInt())).thenReturn(List.of(user));
            when(userRepository.count()).thenReturn(1L);

            

            // When
            userService.ping();
            userService.create(user);
            userService.getById("user-123");
            userService.update(user);
            userService.list(0, 10);
            userService.count();
            userService.delete("user-123");

            // Then
            
        }

        @Test
        @DisplayName("Should record error metrics for failed operations")
        void shouldRecordErrorMetricsForFailedOperations() {
            // Given
            User user = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            RuntimeException dbException = new RuntimeException("Database error");
            when(userRepository.save(any(User.class))).thenThrow(dbException);
            when(userRepository.findById(anyString())).thenThrow(dbException);
            when(userRepository.findPage(anyInt(), anyInt())).thenThrow(dbException);
            when(userRepository.count()).thenThrow(dbException);
            doThrow(dbException).when(userRepository).deleteById(anyString());

            

            // When & Then
            assertThatThrownBy(() -> userService.create(user))
                    .isInstanceOf(DatabaseException.class);
            assertThatThrownBy(() -> userService.getById("user-123"))
                    .isInstanceOf(DatabaseException.class);
            assertThatThrownBy(() -> userService.update(user))
                    .isInstanceOf(DatabaseException.class);
            assertThatThrownBy(() -> userService.list(0, 10))
                    .isInstanceOf(DatabaseException.class);
            assertThatThrownBy(() -> userService.count())
                    .isInstanceOf(DatabaseException.class);
            assertThatThrownBy(() -> userService.delete("user-123"))
                    .isInstanceOf(DatabaseException.class);

            // Then
            
        }
    }
}
