package com.example.user.integration;

import com.example.common.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for Thrift service integration tests.
 * Provides common setup and utilities for Thrift service integration tests.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
public abstract class BaseThriftIntegrationTest {

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0")
            .withReuse(true);

    @BeforeEach
    void setUp() {
        // MongoDB container is automatically started and configured
        // The application will use the container's connection string
    }

    /**
     * Helper method to create a test user.
     */
    protected User createTestUser() {
        return User.builder()
                .name("Integration Test User")
                .phone("+1-555-123-4567")
                .address("123 Integration Test St")
                .build();
    }

    /**
     * Helper method to create a test user with custom data.
     */
    protected User createTestUser(String name, String phone, String address) {
        return User.builder()
                .name(name)
                .phone(phone)
                .address(address)
                .build();
    }

    /**
     * Helper method to create a test user with ID.
     */
    protected User createTestUserWithId(String id, String name, String phone, String address) {
        return User.builder()
                .id(id)
                .name(name)
                .phone(phone)
                .address(address)
                .build();
    }

    /**
     * Helper method to create a test user with ID.
     */
    protected User createTestUserWithId(String id) {
        return User.builder()
                .id(id)
                .name("Test User " + id)
                .phone("+1-555-123-4567")
                .address("123 Test St")
                .build();
    }
}
