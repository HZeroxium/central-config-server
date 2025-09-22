package com.example.rest.user.e2e;

import com.example.common.domain.User;
import com.example.rest.user.dto.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Simple End-to-End tests for User CRUD operations.
 * 
 * These tests run against a running system (after docker-compose up -d).
 * They are pure RestAssured tests without Spring Boot test context.
 * 
 * To run these tests:
 * 1. Start the system: docker-compose up -d
 * 2. Wait for all services to be healthy
 * 3. Run: ./gradlew :user-rest-spring-service:e2eTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserE2ESimpleTest {

  private static final String BASE_URL = "http://localhost:8083";
  private static String createdUserId;
  private static final int MAX_RETRIES = 5;
  private static final int RETRY_DELAY_MS = 2000;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = BASE_URL;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    waitForServiceToBeReady();
  }

  private static void waitForServiceToBeReady() {
    int retries = 0;
    while (retries < MAX_RETRIES) {
      try {
        given()
            .when()
            .get("/users/ping")
            .then()
            .statusCode(200);
        System.out.println("Service is ready after " + retries + " retries");
        return;
      } catch (Exception e) {
        retries++;
        System.out.println("Service not ready, retry " + retries + "/" + MAX_RETRIES + " in " + RETRY_DELAY_MS + "ms");
        if (retries >= MAX_RETRIES) {
          throw new RuntimeException("Service not ready after " + MAX_RETRIES + " retries", e);
        }
        try {
          Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Interrupted while waiting for service", ie);
        }
      }
    }
  }

  @Test
  @Order(1)
  @DisplayName("GET /users/ping - Should return 200 OK")
  void testPing() {
    given()
        .when()
        .get("/users/ping")
        .then()
        .statusCode(200)
        .body(equalTo("pong"));
  }

  @Test
  @Order(2)
  @DisplayName("POST /users - Should create a user successfully")
  void testCreateUser() {
    CreateUserRequest request = new CreateUserRequest();
    request.setName("E2E Test User");
    request.setPhone("+1234567890");
    request.setAddress("E2E Test Address");
    request.setStatus(User.UserStatus.ACTIVE);
    request.setRole(User.UserRole.USER);

    Response response = given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/users")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("message", equalTo("User created successfully"))
        .body("user", notNullValue())
        .body("user.name", equalTo("E2E Test User"))
        .body("user.phone", equalTo("+1234567890"))
        .body("user.address", equalTo("E2E Test Address"))
        .body("user.status", equalTo("ACTIVE"))
        .body("user.role", equalTo("USER"))
        .body("user.createdBy", equalTo("admin"))
        .body("user.updatedBy", equalTo("admin"))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue())
        .extract()
        .response();

    // Store the created user ID for subsequent tests
    createdUserId = response.jsonPath().getString("user.id");
    assertThat(createdUserId).isNotNull();
  }

  @Test
  @Order(3)
  @DisplayName("GET /users - Should return the list of users")
  void testListUsers() {
    given()
        .when()
        .get("/users?page=0&size=10")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("message", equalTo("Users retrieved successfully"))
        .body("items", notNullValue())
        .body("page", equalTo(0))
        .body("size", equalTo(10))
        .body("total", greaterThanOrEqualTo(1))
        .body("totalPages", greaterThanOrEqualTo(1))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue())
        .body("items[0].id", notNullValue())
        .body("items[0].name", notNullValue());
  }

  @Test
  @Order(4)
  @DisplayName("GET /users/{id} - Should return a specific user")
  void testGetUserById() {
    given()
        .pathParam("id", createdUserId)
        .when()
        .get("/users/{id}")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("message", equalTo("User retrieved successfully"))
        .body("user", notNullValue())
        .body("user.id", equalTo(createdUserId))
        .body("user.name", equalTo("E2E Test User"))
        .body("user.phone", equalTo("+1234567890"))
        .body("user.address", equalTo("E2E Test Address"))
        .body("user.status", equalTo("ACTIVE"))
        .body("user.role", equalTo("USER"))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue());
  }

  @Test
  @Order(5)
  @DisplayName("PUT /users/{id} - Should update a user")
  void testUpdateUser() {
    UpdateUserRequest request = new UpdateUserRequest();
    request.setName("Updated E2E Test User");
    request.setPhone("+9876543210");
    request.setAddress("Updated E2E Test Address");
    request.setStatus(User.UserStatus.ACTIVE);
    request.setRole(User.UserRole.USER);
    request.setVersion(1);

    given()
        .pathParam("id", createdUserId)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/users/{id}")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("message", equalTo("User updated successfully"))
        .body("user", notNullValue())
        .body("user.id", equalTo(createdUserId))
        .body("user.name", equalTo("Updated E2E Test User"))
        .body("user.phone", equalTo("+9876543210"))
        .body("user.address", equalTo("Updated E2E Test Address"))
        .body("user.status", equalTo("ACTIVE"))
        .body("user.role", equalTo("USER"))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue());
  }

  @Test
  @Order(6)
  @DisplayName("DELETE /users/{id} - Should delete a user")
  void testDeleteUser() {
    given()
        .pathParam("id", createdUserId)
        .when()
        .delete("/users/{id}")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("message", equalTo("User deleted successfully"))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue());
  }

  // Invalid cases tests
  @Test
  @Order(7)
  @DisplayName("POST /users - Should return validation error for invalid input")
  void testCreateUserWithInvalidInput() {
    CreateUserRequest request = new CreateUserRequest();
    request.setName(""); // Invalid: empty name
    request.setPhone("invalid-phone"); // Invalid: not a valid phone format
    request.setAddress("Test Address");

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/users")
        .then()
        .statusCode(400); // Bad Request due to validation errors
  }

  @Test
  @Order(8)
  @DisplayName("GET /users/{id} - Should return 404 for non-existent user")
  void testGetNonExistentUser() {
    String nonExistentId = "non-existent-id-12345";

    given()
        .pathParam("id", nonExistentId)
        .when()
        .get("/users/{id}")
        .then()
        .statusCode(404)
        .body("status", equalTo(2)) // USER_NOT_FOUND
        .body("message", equalTo("User not found"))
        .body("user", nullValue())
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue());
  }

  @Test
  @Order(9)
  @DisplayName("PUT /users/{id} - Should return 404 for non-existent user")
  void testUpdateNonExistentUser() {
    String nonExistentId = "non-existent-id-12345";
    UpdateUserRequest request = new UpdateUserRequest();
    request.setName("Updated User");
    request.setPhone("+1234567890");
    request.setAddress("Updated Address");

    given()
        .pathParam("id", nonExistentId)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/users/{id}")
        .then()
        .statusCode(404)
        .body("status", equalTo(2)) // USER_NOT_FOUND
        .body("message", equalTo("User not found"))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue());
  }

  @Test
  @Order(10)
  @DisplayName("DELETE /users/{id} - Should return 404 for non-existent user")
  void testDeleteNonExistentUser() {
    String nonExistentId = "non-existent-id-12345";

    given()
        .pathParam("id", nonExistentId)
        .when()
        .delete("/users/{id}")
        .then()
        .statusCode(404)
        .body("status", equalTo(2)) // USER_NOT_FOUND
        .body("message", equalTo("User not found"))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue());
  }

  @Test
  @Order(11)
  @DisplayName("GET /users - Should handle pagination correctly")
  void testListUsersWithPagination() {
    // Test with different page sizes
    given()
        .when()
        .get("/users?page=0&size=5")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("page", equalTo(0))
        .body("size", equalTo(5));

    // Test with page 1 (should be empty or have fewer items)
    given()
        .when()
        .get("/users?page=1&size=5")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("page", equalTo(1))
        .body("size", equalTo(5));
  }

  @Test
  @Order(12)
  @DisplayName("POST /users - Should handle missing required fields")
  void testCreateUserWithMissingFields() {
    // Test with missing name (required field)
    CreateUserRequest request = new CreateUserRequest();
    request.setPhone("+1234567890");
    request.setAddress("Test Address");

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/users")
        .then()
        .statusCode(400); // Bad Request due to missing required field
  }

  @Test
  @Order(13)
  @DisplayName("GET /users - Should handle search and filtering")
  void testListUsersWithSearch() {
    // Test with search parameter
    given()
        .when()
        .get("/users?page=0&size=10&search=E2E")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("message", equalTo("Users retrieved successfully"))
        .body("items", notNullValue())
        .body("page", equalTo(0))
        .body("size", equalTo(10));
  }
}
