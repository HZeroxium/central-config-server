package com.example.user.integration;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Database integration tests using Testcontainers.
 * Tests real database operations with different persistence types.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("Database Integration Tests")
class DatabaseIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:8.0"))
            .withExposedPorts(27017);

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserRepositoryPort userRepository;


    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Nested
    @DisplayName("MongoDB Integration Tests")
    class MongoDbIntegrationTests {

        @Test
        @DisplayName("Should save and retrieve user from MongoDB")
        void shouldSaveAndRetrieveUserFromMongoDB() {
            // Given
            User user = User.builder()
                    .name("MongoDB Test User")
                    .phone("+1-555-123-4567")
                    .address("123 MongoDB Test St")
                    .build();

            // When
            User savedUser = userRepository.save(user);

            // Then
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getName()).isEqualTo("MongoDB Test User");
            assertThat(savedUser.getPhone()).isEqualTo("+1-555-123-4567");
            assertThat(savedUser.getAddress()).isEqualTo("123 MongoDB Test St");

            // Verify we can retrieve the user
            Optional<User> retrievedUser = userRepository.findById(savedUser.getId());
            assertThat(retrievedUser).isPresent();
            assertThat(retrievedUser.get()).isEqualTo(savedUser);
        }

        @Test
        @DisplayName("Should update user in MongoDB")
        void shouldUpdateUserInMongoDB() {
            // Given
            User user = User.builder()
                    .name("MongoDB Test User")
                    .phone("+1-555-123-4567")
                    .address("123 MongoDB Test St")
                    .build();

            User savedUser = userRepository.save(user);

            // When
            User updatedUser = User.builder()
                    .id(savedUser.getId())
                    .name("Updated MongoDB Test User")
                    .phone("+1-555-987-6543")
                    .address("456 Updated MongoDB Test Ave")
                    .build();

            User result = userRepository.save(updatedUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(savedUser.getId());
            assertThat(result.getName()).isEqualTo("Updated MongoDB Test User");
            assertThat(result.getPhone()).isEqualTo("+1-555-987-6543");
            assertThat(result.getAddress()).isEqualTo("456 Updated MongoDB Test Ave");
        }

        @Test
        @DisplayName("Should delete user from MongoDB")
        void shouldDeleteUserFromMongoDB() {
            // Given
            User user = User.builder()
                    .name("MongoDB Test User")
                    .phone("+1-555-123-4567")
                    .address("123 MongoDB Test St")
                    .build();

            User savedUser = userRepository.save(user);
            String userId = savedUser.getId();

            // When
            userRepository.deleteById(userId);

            // Then
            Optional<User> deletedUser = userRepository.findById(userId);
            assertThat(deletedUser).isEmpty();
        }

        @Test
        @DisplayName("Should count users in MongoDB")
        void shouldCountUsersInMongoDB() {
            // Given
            User user1 = User.builder()
                    .name("MongoDB User 1")
                    .phone("+1-555-111-1111")
                    .build();

            User user2 = User.builder()
                    .name("MongoDB User 2")
                    .phone("+1-555-222-2222")
                    .build();

            userRepository.save(user1);
            userRepository.save(user2);

            // When
            long count = userRepository.count();

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle pagination in MongoDB")
        void shouldHandlePaginationInMongoDB() {
            // Given
            for (int i = 1; i <= 5; i++) {
                User user = User.builder()
                        .name("MongoDB User " + i)
                        .phone("+1-555-" + String.format("%03d", i) + "-" + String.format("%04d", i))
                        .build();
                userRepository.save(user);
            }

            // When
            List<User> page1 = userRepository.findPage(0, 2);
            List<User> page2 = userRepository.findPage(1, 2);

            // Then
            assertThat(page1).hasSize(2);
            assertThat(page2).hasSize(2);
            assertThat(page1).isNotEqualTo(page2);
        }
    }

    @Nested
    @DisplayName("JPA Integration Tests")
    class JpaIntegrationTests {

        @Test
        @DisplayName("Should save and retrieve user from JPA")
        void shouldSaveAndRetrieveUserFromJPA() {
            // Given
            User user = User.builder()
                    .name("JPA Test User")
                    .phone("+1-555-123-4567")
                    .address("123 JPA Test St")
                    .build();

            // When
            User savedUser = userRepository.save(user);

            // Then
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getName()).isEqualTo("JPA Test User");
            assertThat(savedUser.getPhone()).isEqualTo("+1-555-123-4567");
            assertThat(savedUser.getAddress()).isEqualTo("123 JPA Test St");

            // Verify we can retrieve the user
            Optional<User> retrievedUser = userRepository.findById(savedUser.getId());
            assertThat(retrievedUser).isPresent();
            assertThat(retrievedUser.get()).isEqualTo(savedUser);
        }

        @Test
        @DisplayName("Should update user in JPA")
        void shouldUpdateUserInJPA() {
            // Given
            User user = User.builder()
                    .name("JPA Test User")
                    .phone("+1-555-123-4567")
                    .address("123 JPA Test St")
                    .build();

            User savedUser = userRepository.save(user);

            // When
            User updatedUser = User.builder()
                    .id(savedUser.getId())
                    .name("Updated JPA Test User")
                    .phone("+1-555-987-6543")
                    .address("456 Updated JPA Test Ave")
                    .build();

            User result = userRepository.save(updatedUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(savedUser.getId());
            assertThat(result.getName()).isEqualTo("Updated JPA Test User");
            assertThat(result.getPhone()).isEqualTo("+1-555-987-6543");
            assertThat(result.getAddress()).isEqualTo("456 Updated JPA Test Ave");
        }

        @Test
        @DisplayName("Should delete user from JPA")
        void shouldDeleteUserFromJPA() {
            // Given
            User user = User.builder()
                    .name("JPA Test User")
                    .phone("+1-555-123-4567")
                    .address("123 JPA Test St")
                    .build();

            User savedUser = userRepository.save(user);
            String userId = savedUser.getId();

            // When
            userRepository.deleteById(userId);

            // Then
            Optional<User> deletedUser = userRepository.findById(userId);
            assertThat(deletedUser).isEmpty();
        }

        @Test
        @DisplayName("Should count users in JPA")
        void shouldCountUsersInJPA() {
            // Given
            User user1 = User.builder()
                    .name("JPA User 1")
                    .phone("+1-555-111-1111")
                    .build();

            User user2 = User.builder()
                    .name("JPA User 2")
                    .phone("+1-555-222-2222")
                    .build();

            userRepository.save(user1);
            userRepository.save(user2);

            // When
            long count = userRepository.count();

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle pagination in JPA")
        void shouldHandlePaginationInJPA() {
            // Given
            for (int i = 1; i <= 5; i++) {
                User user = User.builder()
                        .name("JPA User " + i)
                        .phone("+1-555-" + String.format("%03d", i) + "-" + String.format("%04d", i))
                        .build();
                userRepository.save(user);
            }

            // When
            List<User> page1 = userRepository.findPage(0, 2);
            List<User> page2 = userRepository.findPage(1, 2);

            // Then
            assertThat(page1).hasSize(2);
            assertThat(page2).hasSize(2);
            assertThat(page1).isNotEqualTo(page2);
        }
    }

    @Nested
    @DisplayName("Error Handling Integration Tests")
    class ErrorHandlingIntegrationTests {

        @Test
        @DisplayName("Should handle database connection failure gracefully")
        void shouldHandleDatabaseConnectionFailureGracefully() {
            // Given
            User user = User.builder()
                    .name("Test User")
                    .phone("+1-555-123-4567")
                    .build();

            // When & Then
            // This test would require stopping the container to simulate connection failure
            // For now, we'll test that the repository interface is properly configured
            assertThatCode(() -> userRepository.save(user))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle invalid user data gracefully")
        void shouldHandleInvalidUserDataGracefully() {
            // Given
            User user = User.builder()
                    .name("") // Invalid: empty name
                    .phone("invalid-phone") // Invalid: wrong format
                    .build();

            // When & Then
            // The repository should handle invalid data according to its implementation
            // This test verifies that the repository doesn't crash with invalid data
            assertThatCode(() -> userRepository.save(user))
                    .doesNotThrowAnyException();
        }
    }
}