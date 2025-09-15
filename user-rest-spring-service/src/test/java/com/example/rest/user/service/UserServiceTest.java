package com.example.rest.user.service;

import com.example.rest.user.domain.User;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.rest.exception.ThriftServiceException;
import com.example.rest.exception.UserNotFoundException;
import com.example.rest.metrics.ApplicationMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private ApplicationMetrics metrics;

    @InjectMocks
    private UserService userService;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        // Use real metrics for more accurate testing
        when(metrics.startThriftClientTimer()).thenReturn(Timer.start(meterRegistry));
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
            verify(metrics).incrementThriftClientRequests("ping");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("ping"));
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
            verify(metrics).incrementThriftClientRequests("ping");
            verify(metrics).incrementThriftClientErrors("ping", "RuntimeException");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("ping"));
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
            verify(metrics).incrementThriftClientRequests("create");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("create"));
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
            verify(metrics).incrementThriftClientRequests("create");
            verify(metrics).incrementThriftClientErrors("create", "RuntimeException");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("create"));
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
            verify(metrics).incrementThriftClientRequests("getById");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("getById"));
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
            verify(metrics).incrementThriftClientRequests("getById");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("getById"));
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
            verify(metrics).incrementThriftClientRequests("getById");
            verify(metrics).incrementThriftClientErrors("getById", "RuntimeException");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("getById"));
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
            verify(metrics).incrementThriftClientRequests("getById");
            verify(metrics).incrementThriftClientErrors("getById", "RuntimeException");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("getById"));
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
            verify(metrics).incrementThriftClientRequests("update");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("update"));
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
            verify(metrics).incrementThriftClientRequests("update");
            verify(metrics).incrementThriftClientErrors("update", "RuntimeException");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("update"));
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
            verify(metrics).incrementThriftClientRequests("delete");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("delete"));
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
            verify(metrics).incrementThriftClientRequests("delete");
            verify(metrics).incrementThriftClientErrors("delete", "RuntimeException");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("delete"));
        }
    }

    @Nested
    @DisplayName("List Users Tests")
    class ListUsersTests {

        @Test
        @DisplayName("Should list all users successfully")
        void shouldListAllUsersSuccessfully() {
            // Given
            List<User> expectedUsers = Arrays.asList(
                    User.builder().id("user-1").name("John Doe").phone("+1-555-123-4567").build(),
                    User.builder().id("user-2").name("Jane Smith").phone("+1-555-987-6543").build()
            );

            when(thriftClient.list()).thenReturn(expectedUsers);

            // When
            List<User> result = userService.list();

            // Then
            assertThat(result).isEqualTo(expectedUsers);
            verify(thriftClient).list();
            verify(metrics).incrementThriftClientRequests("list");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("list"));
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsersExist() {
            // Given
            when(thriftClient.list()).thenReturn(Arrays.asList());

            // When
            List<User> result = userService.list();

            // Then
            assertThat(result).isEmpty();
            verify(thriftClient).list();
            verify(metrics).incrementThriftClientRequests("list");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("list"));
        }

        @Test
        @DisplayName("Should throw ThriftServiceException when list fails")
        void shouldThrowThriftServiceExceptionWhenListFails() {
            // Given
            RuntimeException cause = new RuntimeException("List failed");
            when(thriftClient.list()).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.list())
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to list users via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).list();
            verify(metrics).incrementThriftClientRequests("list");
            verify(metrics).incrementThriftClientErrors("list", "RuntimeException");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("list"));
        }
    }

    @Nested
    @DisplayName("List Users Paged Tests")
    class ListUsersPagedTests {

        @Test
        @DisplayName("Should list users with pagination successfully")
        void shouldListUsersWithPaginationSuccessfully() {
            // Given
            int page = 0;
            int size = 10;
            List<User> expectedUsers = Arrays.asList(
                    User.builder().id("user-1").name("John Doe").phone("+1-555-123-4567").build(),
                    User.builder().id("user-2").name("Jane Smith").phone("+1-555-987-6543").build()
            );

            when(thriftClient.listPaged(page, size)).thenReturn(expectedUsers);

            // When
            List<User> result = userService.listPaged(page, size);

            // Then
            assertThat(result).isEqualTo(expectedUsers);
            verify(thriftClient).listPaged(page, size);
            verify(metrics).incrementThriftClientRequests("listPaged");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("listPaged"));
        }

        @Test
        @DisplayName("Should handle different page and size values")
        void shouldHandleDifferentPageAndSizeValues() {
            // Given
            int page = 2;
            int size = 5;
            List<User> expectedUsers = Arrays.asList(
                    User.builder().id("user-6").name("User 6").phone("+1-555-111-1111").build()
            );

            when(thriftClient.listPaged(page, size)).thenReturn(expectedUsers);

            // When
            List<User> result = userService.listPaged(page, size);

            // Then
            assertThat(result).isEqualTo(expectedUsers);
            verify(thriftClient).listPaged(page, size);
            verify(metrics).incrementThriftClientRequests("listPaged");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("listPaged"));
        }

        @Test
        @DisplayName("Should throw ThriftServiceException when listPaged fails")
        void shouldThrowThriftServiceExceptionWhenListPagedFails() {
            // Given
            int page = 0;
            int size = 10;
            RuntimeException cause = new RuntimeException("Paged list failed");
            when(thriftClient.listPaged(page, size)).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.listPaged(page, size))
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to list users with pagination via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).listPaged(page, size);
            verify(metrics).incrementThriftClientRequests("listPaged");
            verify(metrics).incrementThriftClientErrors("listPaged", "RuntimeException");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("listPaged"));
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
            when(thriftClient.count()).thenReturn(expectedCount);

            // When
            long result = userService.count();

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(thriftClient).count();
            verify(metrics).incrementThriftClientRequests("count");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("count"));
        }

        @Test
        @DisplayName("Should return zero when no users exist")
        void shouldReturnZeroWhenNoUsersExist() {
            // Given
            when(thriftClient.count()).thenReturn(0L);

            // When
            long result = userService.count();

            // Then
            assertThat(result).isZero();
            verify(thriftClient).count();
            verify(metrics).incrementThriftClientRequests("count");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("count"));
        }

        @Test
        @DisplayName("Should throw ThriftServiceException when count fails")
        void shouldThrowThriftServiceExceptionWhenCountFails() {
            // Given
            RuntimeException cause = new RuntimeException("Count failed");
            when(thriftClient.count()).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.count())
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to count users via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).count();
            verify(metrics).incrementThriftClientRequests("count");
            verify(metrics).incrementThriftClientErrors("count", "RuntimeException");
            verify(metrics).recordThriftClientDuration(any(Timer.Sample.class), eq("count"));
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
            verify(metrics).incrementThriftClientRequests("create");
            verify(metrics).incrementThriftClientErrors("create", "NullPointerException");
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

        @Test
        @DisplayName("Should handle very large page numbers")
        void shouldHandleVeryLargePageNumbers() {
            // Given
            int largePage = Integer.MAX_VALUE;
            int size = 10;
            when(thriftClient.listPaged(largePage, size)).thenReturn(Arrays.asList());

            // When
            List<User> result = userService.listPaged(largePage, size);

            // Then
            assertThat(result).isEmpty();
            verify(thriftClient).listPaged(largePage, size);
        }

        @Test
        @DisplayName("Should handle very large size values")
        void shouldHandleVeryLargeSizeValues() {
            // Given
            int page = 0;
            int largeSize = Integer.MAX_VALUE;
            when(thriftClient.listPaged(page, largeSize)).thenReturn(Arrays.asList());

            // When
            List<User> result = userService.listPaged(page, largeSize);

            // Then
            assertThat(result).isEmpty();
            verify(thriftClient).listPaged(page, largeSize);
        }
    }
}
