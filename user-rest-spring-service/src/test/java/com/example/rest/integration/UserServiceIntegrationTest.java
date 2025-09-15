package com.example.rest.integration;

import com.example.rest.user.domain.User;
import com.example.rest.user.service.UserService;
import com.example.rest.metrics.ApplicationMetrics;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.rest.exception.ThriftServiceException;
import com.example.rest.exception.UserNotFoundException;
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
 * Tests the service layer with real Spring context but mocked Thrift client.
 */
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @MockBean
    private ThriftUserClientPort thriftClient;

    @MockBean
    private ApplicationMetrics metrics;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(thriftClient, metrics);
    }

    @Nested
    @DisplayName("Ping Integration Tests")
    class PingIntegrationTests {

        @Test
        @DisplayName("Should ping successfully with real Spring context")
        void shouldPingSuccessfullyWithRealSpringContext() {
            // Given
            String expectedResponse = "pong";
            when(thriftClient.ping()).thenReturn(expectedResponse);

            // When
            String result = userService.ping();

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(thriftClient).ping();
            verify(metrics).incrementThriftClientRequests("ping");
        }

        @Test
        @DisplayName("Should handle ping failure with real Spring context")
        void shouldHandlePingFailureWithRealSpringContext() {
            // Given
            RuntimeException cause = new RuntimeException("Connection failed");
            when(thriftClient.ping()).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.ping())
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to ping Thrift service")
                    .hasCause(cause);

            verify(thriftClient).ping();
            verify(metrics).incrementThriftClientRequests("ping");
            verify(metrics).incrementThriftClientErrors("ping", "RuntimeException");
        }
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

            User expectedUser = User.builder()
                    .id("user-123")
                    .name("Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Integration Test St")
                    .build();

            when(thriftClient.create(inputUser)).thenReturn(expectedUser);

            // When
            User result = userService.create(inputUser);

            // Then
            assertThat(result).isEqualTo(expectedUser);
            verify(thriftClient).create(inputUser);
            verify(metrics).incrementThriftClientRequests("create");
        }

        @Test
        @DisplayName("Should handle create user failure with real Spring context")
        void shouldHandleCreateUserFailureWithRealSpringContext() {
            // Given
            User inputUser = User.builder()
                    .name("Integration Test User")
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
            verify(metrics).incrementThriftClientRequests("create");
            verify(metrics).incrementThriftClientErrors("create", "RuntimeException");
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

            when(thriftClient.getById(userId)).thenReturn(Optional.of(expectedUser));

            // When
            Optional<User> result = userService.getById(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedUser);
            verify(thriftClient).getById(userId);
            verify(metrics).incrementThriftClientRequests("getById");
        }

        @Test
        @DisplayName("Should return empty when user not found with real Spring context")
        void shouldReturnEmptyWhenUserNotFoundWithRealSpringContext() {
            // Given
            String userId = "non-existent-user";
            when(thriftClient.getById(userId)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.getById(userId);

            // Then
            assertThat(result).isEmpty();
            verify(thriftClient).getById(userId);
            verify(metrics).incrementThriftClientRequests("getById");
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found with specific message with real Spring context")
        void shouldThrowUserNotFoundExceptionWhenUserNotFoundWithSpecificMessageWithRealSpringContext() {
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
            verify(metrics).incrementThriftClientRequests("getById");
            verify(metrics).incrementThriftClientErrors("getById", "RuntimeException");
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

            User expectedUser = User.builder()
                    .id("user-123")
                    .name("Updated Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("456 Updated Integration Test Ave")
                    .build();

            when(thriftClient.update(inputUser)).thenReturn(expectedUser);

            // When
            User result = userService.update(inputUser);

            // Then
            assertThat(result).isEqualTo(expectedUser);
            verify(thriftClient).update(inputUser);
            verify(metrics).incrementThriftClientRequests("update");
        }

        @Test
        @DisplayName("Should handle update user failure with real Spring context")
        void shouldHandleUpdateUserFailureWithRealSpringContext() {
            // Given
            User inputUser = User.builder()
                    .id("user-123")
                    .name("Updated Integration Test User")
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
            verify(metrics).incrementThriftClientRequests("update");
            verify(metrics).incrementThriftClientErrors("update", "RuntimeException");
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
            doNothing().when(thriftClient).delete(userId);

            // When
            assertThatCode(() -> userService.delete(userId))
                    .doesNotThrowAnyException();

            // Then
            verify(thriftClient).delete(userId);
            verify(metrics).incrementThriftClientRequests("delete");
        }

        @Test
        @DisplayName("Should handle delete user failure with real Spring context")
        void shouldHandleDeleteUserFailureWithRealSpringContext() {
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
            verify(metrics).incrementThriftClientRequests("delete");
            verify(metrics).incrementThriftClientErrors("delete", "RuntimeException");
        }
    }

    @Nested
    @DisplayName("List Users Integration Tests")
    class ListUsersIntegrationTests {

        @Test
        @DisplayName("Should list all users successfully with real Spring context")
        void shouldListAllUsersSuccessfullyWithRealSpringContext() {
            // Given
            List<User> expectedUsers = Arrays.asList(
                    User.builder().id("user-1").name("User 1").phone("+1-555-111-1111").build(),
                    User.builder().id("user-2").name("User 2").phone("+1-555-222-2222").build()
            );

            when(thriftClient.list()).thenReturn(expectedUsers);

            // When
            List<User> result = userService.list();

            // Then
            assertThat(result).isEqualTo(expectedUsers);
            verify(thriftClient).list();
            verify(metrics).incrementThriftClientRequests("list");
        }

        @Test
        @DisplayName("Should list users with pagination successfully with real Spring context")
        void shouldListUsersWithPaginationSuccessfullyWithRealSpringContext() {
            // Given
            int page = 0;
            int size = 10;
            List<User> expectedUsers = Arrays.asList(
                    User.builder().id("user-1").name("User 1").phone("+1-555-111-1111").build(),
                    User.builder().id("user-2").name("User 2").phone("+1-555-222-2222").build()
            );

            when(thriftClient.listPaged(page, size)).thenReturn(expectedUsers);

            // When
            List<User> result = userService.listPaged(page, size);

            // Then
            assertThat(result).isEqualTo(expectedUsers);
            verify(thriftClient).listPaged(page, size);
            verify(metrics).incrementThriftClientRequests("listPaged");
        }

        @Test
        @DisplayName("Should count users successfully with real Spring context")
        void shouldCountUsersSuccessfullyWithRealSpringContext() {
            // Given
            long expectedCount = 42L;
            when(thriftClient.count()).thenReturn(expectedCount);

            // When
            long result = userService.count();

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(thriftClient).count();
            verify(metrics).incrementThriftClientRequests("count");
        }
    }
}
