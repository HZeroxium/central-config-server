package com.example.rest.user.service;

import com.example.rest.user.domain.User;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.rest.exception.ThriftServiceException;
import com.example.rest.exception.UserNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;    

/**
 * Comprehensive unit tests for UserService class.
 * Tests all service methods with mocked Thrift client and metrics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private ThriftUserClientPort thriftClient;


    @InjectMocks
    private UserService userService;


    @BeforeEach
    void setUp() {

    }

    @Nested
    @DisplayName("Ping Tests")
    class PingTests {

        @Test
        @DisplayName("Should ping Thrift service successfully")
        void shouldPingThriftServiceSuccessfully() {
            // Given
            String expectedResponse = "pong";
            when(thriftClient.ping()).thenReturn(expectedResponse);

            // When
            String result = userService.ping();

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(thriftClient).ping();

        }

        @Test
        @DisplayName("Should throw ThriftServiceException when ping fails")
        void shouldThrowThriftServiceExceptionWhenPingFails() {
            // Given
            RuntimeException cause = new RuntimeException("Connection failed");
            when(thriftClient.ping()).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.ping())
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to ping Thrift service")
                    .hasCause(cause);

            verify(thriftClient).ping();

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

            User expectedUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(thriftClient.create(inputUser)).thenReturn(expectedUser);

            // When
            User result = userService.create(inputUser);

            // Then
            assertThat(result).isEqualTo(expectedUser);
            verify(thriftClient).create(inputUser);

           
        }

        @Test
        @DisplayName("Should throw ThriftServiceException when create fails")
        void shouldThrowThriftServiceExceptionWhenCreateFails() {
            // Given
            User inputUser = User.builder()
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            RuntimeException cause = new RuntimeException("Database error");
            when(thriftClient.create(inputUser)).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.create(inputUser))
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to create user via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).create(inputUser);

        }
    }

    @Nested
    @DisplayName("Get User by ID Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should get user by ID successfully")
        void shouldGetUserByIdSuccessfully() {
            // Given
            String userId = "user-123";
            User expectedUser = User.builder()
                    .id(userId)
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(thriftClient.getById(userId)).thenReturn(Optional.of(expectedUser));

            // When
            Optional<User> result = userService.getById(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedUser);
            verify(thriftClient).getById(userId);

        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            String userId = "non-existent-user";
            when(thriftClient.getById(userId)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.getById(userId);

            // Then
            assertThat(result).isEmpty();
            verify(thriftClient).getById(userId);

        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found with specific message")
        void shouldThrowUserNotFoundExceptionWhenUserNotFoundWithSpecificMessage() {
            // Given
            String userId = "user-123";
            RuntimeException cause = new RuntimeException("User not found with ID: " + userId);
            when(thriftClient.getById(userId)).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.getById(userId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User with ID 'user-123' not found in context: Thrift service")
                    .hasCause(cause);

            verify(thriftClient).getById(userId);

        }

        @Test
        @DisplayName("Should throw ThriftServiceException when getById fails with other error")
        void shouldThrowThriftServiceExceptionWhenGetByIdFailsWithOtherError() {
            // Given
            String userId = "user-123";
            RuntimeException cause = new RuntimeException("Database connection failed");
            when(thriftClient.getById(userId)).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.getById(userId))
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to retrieve user via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).getById(userId);

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
                    .name("John Doe Updated")
                    .phone("+1-555-123-4567")
                    .address("456 Oak Ave")
                    .build();

            User expectedUser = User.builder()
                    .id("user-123")
                    .name("John Doe Updated")
                    .phone("+1-555-123-4567")
                    .address("456 Oak Ave")
                    .build();

            when(thriftClient.update(inputUser)).thenReturn(expectedUser);

            // When
            User result = userService.update(inputUser);

            // Then
            assertThat(result).isEqualTo(expectedUser);
            verify(thriftClient).update(inputUser);

        }

        @Test
        @DisplayName("Should throw ThriftServiceException when update fails")
        void shouldThrowThriftServiceExceptionWhenUpdateFails() {
            // Given
            User inputUser = User.builder()
                    .id("user-123")
                    .name("John Doe Updated")
                    .phone("+1-555-123-4567")
                    .build();

            RuntimeException cause = new RuntimeException("Update failed");
            when(thriftClient.update(inputUser)).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.update(inputUser))
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to update user via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).update(inputUser);
           
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
            doNothing().when(thriftClient).delete(userId);

            // When
            assertThatCode(() -> userService.delete(userId))
                    .doesNotThrowAnyException();

            // Then
            verify(thriftClient).delete(userId);
           
        }

        @Test
        @DisplayName("Should throw ThriftServiceException when delete fails")
        void shouldThrowThriftServiceExceptionWhenDeleteFails() {
            // Given
            String userId = "user-123";
            RuntimeException cause = new RuntimeException("Delete failed");
            doThrow(cause).when(thriftClient).delete(userId);

            // When & Then
            assertThatThrownBy(() -> userService.delete(userId))
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to delete user via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).delete(userId);
           
        }
    }

    

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle null user input gracefully")
        void shouldHandleNullUserInputGracefully() {
            // Given
            User nullUser = null;
            when(thriftClient.create(nullUser)).thenThrow(new NullPointerException());

            // When & Then
            assertThatThrownBy(() -> userService.create(nullUser))
                    .isInstanceOf(ThriftServiceException.class);

            verify(thriftClient).create(nullUser);
        }

        @Test
        @DisplayName("Should handle empty string user ID gracefully")
        void shouldHandleEmptyStringUserIdGracefully() {
            // Given
            String emptyUserId = "";
            when(thriftClient.getById(emptyUserId)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.getById(emptyUserId);

            // Then
            assertThat(result).isEmpty();
            verify(thriftClient).getById(emptyUserId);
        }
    }
        
}
