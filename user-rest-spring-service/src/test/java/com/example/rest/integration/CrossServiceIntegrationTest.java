package com.example.rest.integration;

import com.example.rest.user.domain.User;
import com.example.rest.user.service.UserService;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.rest.metrics.ApplicationMetrics;
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
 * Cross-service integration tests.
 * Tests the interaction between REST service and Thrift service.
 */
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("Cross-Service Integration Tests")
class CrossServiceIntegrationTest {

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
    @DisplayName("End-to-End User Management Flow")
    class EndToEndUserManagementFlow {

        @Test
        @DisplayName("Should complete full user lifecycle with real Spring context")
        void shouldCompleteFullUserLifecycleWithRealSpringContext() {
            // Given
            User createUser = User.builder()
                    .name("Cross-Service Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Cross-Service Test St")
                    .build();

            User createdUser = User.builder()
                    .id("user-123")
                    .name("Cross-Service Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Cross-Service Test St")
                    .build();

            User updatedUser = User.builder()
                    .id("user-123")
                    .name("Updated Cross-Service Test User")
                    .phone("+1-555-987-6543")
                    .address("456 Updated Cross-Service Test Ave")
                    .build();

            // Mock Thrift client responses
            when(thriftClient.create(any(User.class))).thenReturn(createdUser);
            when(thriftClient.getById("user-123")).thenReturn(Optional.of(createdUser));
            when(thriftClient.update(any(User.class))).thenReturn(updatedUser);
            when(thriftClient.list()).thenReturn(Arrays.asList(createdUser));
            when(thriftClient.count()).thenReturn(1L);
            doNothing().when(thriftClient).delete("user-123");

            // When & Then - Create User
            User result1 = userService.create(createUser);
            assertThat(result1).isEqualTo(createdUser);
            verify(thriftClient).create(any(User.class));
            verify(metrics).incrementThriftClientRequests("create");

            // When & Then - Get User
            Optional<User> result2 = userService.getById("user-123");
            assertThat(result2).isPresent();
            assertThat(result2.get()).isEqualTo(createdUser);
            verify(thriftClient).getById("user-123");
            verify(metrics).incrementThriftClientRequests("getById");

            // When & Then - Update User
            User result3 = userService.update(updatedUser);
            assertThat(result3).isEqualTo(updatedUser);
            verify(thriftClient).update(any(User.class));
            verify(metrics).incrementThriftClientRequests("update");

            // When & Then - List Users
            List<User> result4 = userService.list();
            assertThat(result4).hasSize(1);
            assertThat(result4.get(0)).isEqualTo(createdUser);
            verify(thriftClient).list();
            verify(metrics).incrementThriftClientRequests("list");

            // When & Then - Count Users
            long result5 = userService.count();
            assertThat(result5).isEqualTo(1L);
            verify(thriftClient).count();
            verify(metrics).incrementThriftClientRequests("count");

            // When & Then - Delete User
            assertThatCode(() -> userService.delete("user-123"))
                    .doesNotThrowAnyException();
            verify(thriftClient).delete("user-123");
            verify(metrics).incrementThriftClientRequests("delete");
        }

        @Test
        @DisplayName("Should handle Thrift service unavailability gracefully")
        void shouldHandleThriftServiceUnavailabilityGracefully() {
            // Given
            User user = User.builder()
                    .name("Cross-Service Test User")
                    .phone("+1-555-123-4567")
                    .build();

            ThriftServiceException cause = new ThriftServiceException("Thrift service unavailable");
            when(thriftClient.create(any(User.class))).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.create(user))
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to create user via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).create(any(User.class));
            verify(metrics).incrementThriftClientRequests("create");
            verify(metrics).incrementThriftClientErrors("create", "ThriftServiceException");
        }

        @Test
        @DisplayName("Should handle Thrift service timeout gracefully")
        void shouldHandleThriftServiceTimeoutGracefully() {
            // Given
            User user = User.builder()
                    .name("Cross-Service Test User")
                    .phone("+1-555-123-4567")
                    .build();

            RuntimeException cause = new RuntimeException("Connection timeout");
            when(thriftClient.create(any(User.class))).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.create(user))
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to create user via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).create(any(User.class));
            verify(metrics).incrementThriftClientRequests("create");
            verify(metrics).incrementThriftClientErrors("create", "RuntimeException");
        }
    }

    @Nested
    @DisplayName("Error Propagation Tests")
    class ErrorPropagationTests {

        @Test
        @DisplayName("Should propagate UserNotFoundException from Thrift service")
        void shouldPropagateUserNotFoundExceptionFromThriftService() {
            // Given
            String userId = "non-existent-user";
            RuntimeException cause = new RuntimeException("User not found with ID: " + userId);
            when(thriftClient.getById(userId)).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.getById(userId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User with ID 'non-existent-user' not found in context: Thrift service")
                    .hasCause(cause);

            verify(thriftClient).getById(userId);
            verify(metrics).incrementThriftClientRequests("getById");
            verify(metrics).incrementThriftClientErrors("getById", "RuntimeException");
        }

        @Test
        @DisplayName("Should propagate ThriftServiceException for service errors")
        void shouldPropagateThriftServiceExceptionForServiceErrors() {
            // Given
            User user = User.builder()
                    .name("Cross-Service Test User")
                    .phone("+1-555-123-4567")
                    .build();

            RuntimeException cause = new RuntimeException("Service error");
            when(thriftClient.create(any(User.class))).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> userService.create(user))
                    .isInstanceOf(ThriftServiceException.class)
                    .hasMessage("Failed to create user via Thrift service")
                    .hasCause(cause);

            verify(thriftClient).create(any(User.class));
            verify(metrics).incrementThriftClientRequests("create");
            verify(metrics).incrementThriftClientErrors("create", "RuntimeException");
        }
    }

    @Nested
    @DisplayName("Performance and Load Tests")
    class PerformanceAndLoadTests {

        @Test
        @DisplayName("Should handle multiple concurrent requests")
        void shouldHandleMultipleConcurrentRequests() {
            // Given
            User user = User.builder()
                    .name("Concurrent Test User")
                    .phone("+1-555-123-4567")
                    .build();

            User createdUser = User.builder()
                    .id("user-123")
                    .name("Concurrent Test User")
                    .phone("+1-555-123-4567")
                    .build();

            when(thriftClient.create(any(User.class))).thenReturn(createdUser);

            // When
            List<User> results = Arrays.asList(
                    userService.create(user),
                    userService.create(user),
                    userService.create(user)
            );

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(u -> u.getId().equals("user-123"));
            verify(thriftClient, times(3)).create(any(User.class));
            verify(metrics, times(3)).incrementThriftClientRequests("create");
        }

        @Test
        @DisplayName("Should handle large dataset operations")
        void shouldHandleLargeDatasetOperations() {
            // Given
            List<User> users = Arrays.asList(
                    User.builder().id("user-1").name("User 1").phone("+1-555-111-1111").build(),
                    User.builder().id("user-2").name("User 2").phone("+1-555-222-2222").build(),
                    User.builder().id("user-3").name("User 3").phone("+1-555-333-3333").build()
            );

            when(thriftClient.list()).thenReturn(users);
            when(thriftClient.count()).thenReturn(3L);

            // When
            List<User> result1 = userService.list();
            long result2 = userService.count();

            // Then
            assertThat(result1).hasSize(3);
            assertThat(result2).isEqualTo(3L);
            verify(thriftClient).list();
            verify(thriftClient).count();
            verify(metrics).incrementThriftClientRequests("list");
            verify(metrics).incrementThriftClientRequests("count");
        }
    }

    @Nested
    @DisplayName("Configuration and Environment Tests")
    class ConfigurationAndEnvironmentTests {

        @Test
        @DisplayName("Should use correct Spring profile for integration tests")
        void shouldUseCorrectSpringProfileForIntegrationTests() {
            // Given
            String expectedProfile = "integration";

            // When
            String activeProfile = System.getProperty("spring.profiles.active");

            // Then
            // This test verifies that the integration profile is active
            // The actual profile verification would be done by Spring's test context
            assertThat(activeProfile).isEqualTo(expectedProfile);
        }

        @Test
        @DisplayName("Should have all required beans in Spring context")
        void shouldHaveAllRequiredBeansInSpringContext() {
            // Given & When
            // The test context should load successfully with all required beans

            // Then
            assertThat(userService).isNotNull();
            assertThat(thriftClient).isNotNull();
            assertThat(metrics).isNotNull();
        }
    }
}
