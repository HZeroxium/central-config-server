package com.example.rest.integration;

import com.example.rest.user.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

/**
 * Base class for integration tests.
 * Provides common setup and utilities for REST API integration tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/users";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Helper method to create a test user request.
     */
    protected CreateUserRequest createTestUserRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Integration Test User");
        request.setPhone("+1-555-123-4567");
        request.setAddress("123 Integration Test St");
        return request;
    }

    /**
     * Helper method to create a test user request with custom data.
     */
    protected CreateUserRequest createTestUserRequest(String name, String phone, String address) {
        CreateUserRequest request = new CreateUserRequest();
        request.setName(name);
        request.setPhone(phone);
        request.setAddress(address);
        return request;
    }

    /**
     * Helper method to create a test user update request.
     */
    protected UpdateUserRequest createTestUpdateRequest() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated Integration Test User");
        request.setPhone("+1-555-987-6543");
        request.setAddress("456 Updated Integration Test Ave");
        return request;
    }

    /**
     * Helper method to create a test user update request with custom data.
     */
    protected UpdateUserRequest createTestUpdateRequest(String name, String phone, String address) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName(name);
        request.setPhone(phone);
        request.setAddress(address);
        return request;
    }

    /**
     * Helper method to perform a GET request to ping endpoint.
     */
    protected String ping() {
        return given()
                .when()
                .get("/ping")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }

    /**
     * Helper method to create a user via POST request.
     */
    protected CreateUserResponse createUser(CreateUserRequest request) {
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(200)
                .extract()
                .as(CreateUserResponse.class);
    }

    /**
     * Helper method to get a user by ID via GET request.
     */
    protected GetUserResponse getUser(String userId) {
        return given()
                .when()
                .get("/{id}", userId)
                .then()
                .extract()
                .as(GetUserResponse.class);
    }

    /**
     * Helper method to update a user via PUT request.
     */
    protected UpdateUserResponse updateUser(String userId, UpdateUserRequest request) {
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/{id}", userId)
                .then()
                .extract()
                .as(UpdateUserResponse.class);
    }

    /**
     * Helper method to delete a user via DELETE request.
     */
    protected DeleteUserResponse deleteUser(String userId) {
        return given()
                .when()
                .delete("/{id}", userId)
                .then()
                .extract()
                .as(DeleteUserResponse.class);
    }

    /**
     * Helper method to list users via GET request.
     */
    protected ListUsersResponse listUsers() {
        return given()
                .when()
                .get()
                .then()
                .extract()
                .as(ListUsersResponse.class);
    }

    /**
     * Helper method to list users with pagination via GET request.
     */
    protected ListUsersResponse listUsers(int page, int size) {
        return given()
                .queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get()
                .then()
                .extract()
                .as(ListUsersResponse.class);
    }
}
