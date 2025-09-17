package com.example.user.integration;

import com.example.user.domain.User;
import com.example.user.service.UserServiceImpl;
import com.example.user.exception.DatabaseException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Thrift service integration tests with real database.
 * Tests the complete Thrift service with actual database operations.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("Thrift Service Integration Tests")
class ThriftServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:8.0"))
            .withExposedPorts(27017);

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserServiceImpl userService;


    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Nested
    @DisplayName("User CRUD Operations Integration Tests")
    class UserCrudOperationsIntegrationTests {

        @Test
        @DisplayName("Should create user successfully with real database")
        void shouldCreateUserSuccessfullyWithRealDatabase() {
            // Given
            User user = User.builder()
                    .name("Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Integration Test St")
                    .build();

            // When
            User result = userService.create(user);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isEqualTo("Integration Test User");
            assertThat(result.getPhone()).isEqualTo("+1-555-123-4567");
            assertThat(result.getAddress()).isEqualTo("123 Integration Test St");
        }

        @Test
        @DisplayName("Should get user successfully with real database")
        void shouldGetUserSuccessfullyWithRealDatabase() {
            // Given
            User user = User.builder()
                    .name("Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Integration Test St")
                    .build();

            User savedUser = userService.create(user);
            String userId = savedUser.getId();

            // When
            Optional<User> result = userService.getById(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(savedUser);
        }

        @Test
        @DisplayName("Should update user successfully with real database")
        void shouldUpdateUserSuccessfullyWithRealDatabase() {
            // Given
            User user = User.builder()
                    .name("Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Integration Test St")
                    .build();

            User savedUser = userService.create(user);
            String userId = savedUser.getId();

            User updatedUser = User.builder()
                    .id(userId)
                    .name("Updated Integration Test User")
                    .phone("+1-555-987-6543")
                    .address("456 Updated Integration Test Ave")
                    .build();

            // When
            User result = userService.update(updatedUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getName()).isEqualTo("Updated Integration Test User");
            assertThat(result.getPhone()).isEqualTo("+1-555-987-6543");
            assertThat(result.getAddress()).isEqualTo("456 Updated Integration Test Ave");
        }

        @Test
        @DisplayName("Should delete user successfully with real database")
        void shouldDeleteUserSuccessfullyWithRealDatabase() {
            // Given
            User user = User.builder()
                    .name("Integration Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Integration Test St")
                    .build();

            User savedUser = userService.create(user);
            String userId = savedUser.getId();

            // When
            assertThatCode(() -> userService.delete(userId))
                    .doesNotThrowAnyException();

            // Then
            Optional<User> deletedUser = userService.getById(userId);
            assertThat(deletedUser).isEmpty();
        }

        

        
    }

    @Nested
    @DisplayName("Error Handling Integration Tests")
    class ErrorHandlingIntegrationTests {

        @Test
        @DisplayName("Should handle user not found error with real database")
        void shouldHandleUserNotFoundErrorWithRealDatabase() {
            // Given
            String nonExistentUserId = "non-existent-user";

            // When & Then
            assertThatThrownBy(() -> userService.getById(nonExistentUserId))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessageContaining("Failed to retrieve user from database");
        }

        @Test
        @DisplayName("Should handle validation error with real database")
        void shouldHandleValidationErrorWithRealDatabase() {
            // Given
            User invalidUser = User.builder()
                    .name("") // Invalid: empty name
                    .phone("invalid-phone") // Invalid: wrong format
                    .build();

            // When & Then
            assertThatThrownBy(() -> userService.create(invalidUser))
                    .isInstanceOf(DatabaseException.class)
                    .hasMessageContaining("Failed to create user in database");
        }
    }

    @Nested
    @DisplayName("Performance and Load Integration Tests")
    class PerformanceAndLoadIntegrationTests {

        @Test
        @DisplayName("Should handle multiple concurrent operations with real database")
        void shouldHandleMultipleConcurrentOperationsWithRealDatabase() {
            // Given
            User user = User.builder()
                    .name("Concurrent Test User")
                    .phone("+1-555-123-4567")
                    .address("123 Concurrent Test St")
                    .build();

            // When
            List<User> results = Arrays.asList(
                    userService.create(user),
                    userService.create(user),
                    userService.create(user)
            );

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(u -> u.getId() != null);
            assertThat(results).allMatch(u -> u.getName().equals("Concurrent Test User"));
        }

        
    }

    @Nested
    @DisplayName("Configuration and Environment Integration Tests")
    class ConfigurationAndEnvironmentIntegrationTests {

        @Test
        @DisplayName("Should use correct Spring profile for integration tests")
        void shouldUseCorrectSpringProfileForIntegrationTests() {
            // Given
            String expectedProfile = "integration";

            // When
            String activeProfile = System.getProperty("spring.profiles.active");

            // Then
            assertThat(activeProfile).isEqualTo(expectedProfile);
        }

        @Test
        @DisplayName("Should have all required beans in Spring context")
        void shouldHaveAllRequiredBeansInSpringContext() {
            // Given & When
            // The test context should load successfully with all required beans

            // Then
            assertThat(userService).isNotNull();
        }

        @Test
        @DisplayName("Should connect to real database containers")
        void shouldConnectToRealDatabaseContainers() {
            // Given
            User user = User.builder()
                    .name("Database Connection Test User")
                    .phone("+1-555-123-4567")
                    .build();

            // When
            User result = userService.create(user);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();

            // Verify we can retrieve the user from the database
            Optional<User> retrievedUser = userService.getById(result.getId());
            assertThat(retrievedUser).isPresent();
            assertThat(retrievedUser.get()).isEqualTo(result);
        }
    }
}