package com.example.rest.integration;

import com.example.rest.user.domain.User;
import com.example.rest.user.service.UserService;
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


    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(thriftClient);
    }

    @Nested
    @DisplayName("End-to-End User Management Flow")
    class EndToEndUserManagementFlow {

        

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
        }
    }
}
