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
 * E2E tests for User V2 async CRUD operations.
 * 
 * These tests run against a running system (after docker-compose up -d).
 * Tests both sync (GET) and async (POST/PUT/DELETE) endpoints.
 * 
 * To run these tests:
 * 1. Start the system: docker-compose up -d
 * 2. Wait for all services to be healthy
 * 3. Run: ./gradlew :user-rest-spring-service:e2eTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserV2E2ESimpleTest {

  private static final String BASE_URL = "http://localhost:8083";
  private static String createdOperationId;
  private static String updatedOperationId;
  private static String deletedOperationId;
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

  // Test sync operations (GET endpoints)
  @Test
  @Order(1)
  @DisplayName("GET /v2/users - Should return list of users (sync)")
  void testV2ListUsers() {
    given()
        .when()
        .get("/v2/users?page=0&size=10")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("message", equalTo("Users retrieved successfully"))
        .body("items", notNullValue())
        .body("page", equalTo(0))
        .body("size", equalTo(10))
        .body("total", greaterThanOrEqualTo(0))
        .body("totalPages", greaterThanOrEqualTo(0))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue());
  }

  // Test async operations (POST/PUT/DELETE endpoints)
  @Test
  @Order(2)
  @DisplayName("POST /v2/users - Should accept create request and return operation ID (async)")
  void testV2CreateUserAsync() {
    CreateUserRequest request = new CreateUserRequest();
    request.setName("V2 E2E Test User");
    request.setPhone("+1234567890");
    request.setAddress("V2 E2E Test Address");
    request.setStatus(User.UserStatus.ACTIVE);
    request.setRole(User.UserRole.USER);

    Response response = given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/v2/users")
        .then()
        .statusCode(202) // Accepted
        .body("operationId", notNullValue())
        .body("status", equalTo("PENDING"))
        .body("message", equalTo("User creation request submitted"))
        .body("trackingUrl", notNullValue())
        .body("trackingUrl", containsString("/v2/operations/"))
        .body("trackingUrl", containsString("/status"))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue())
        .extract()
        .response();

    // Store the operation ID for subsequent tests
    createdOperationId = response.jsonPath().getString("operationId");
    assertThat(createdOperationId).isNotNull();
    System.out.println("Created operation ID: " + createdOperationId);
  }

  @Test
  @Order(3)
  @DisplayName("GET /v2/operations/{operationId}/status - Should return operation status")
  void testV2GetOperationStatus() {
    assertThat(createdOperationId).isNotNull();

    given()
        .pathParam("operationId", createdOperationId)
        .when()
        .get("/v2/operations/{operationId}/status")
        .then()
        .statusCode(200)
        .body("operationId", equalTo(createdOperationId))
        .body("status", notNullValue()) // Could be PENDING, IN_PROGRESS, or COMPLETED
        .body("correlationId", notNullValue());
  }

  @Test
  @Order(4)
  @DisplayName("PUT /v2/users/{id} - Should accept update request and return operation ID (async)")
  void testV2UpdateUserAsync() {
    String testUserId = "test-user-id-for-update"; // Mock ID for testing

    UpdateUserRequest request = new UpdateUserRequest();
    request.setName("Updated V2 E2E Test User");
    request.setPhone("+9876543210");
    request.setAddress("Updated V2 E2E Test Address");
    request.setStatus(User.UserStatus.ACTIVE);
    request.setRole(User.UserRole.USER);
    request.setVersion(1);

    Response response = given()
        .pathParam("id", testUserId)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/v2/users/{id}")
        .then()
        .statusCode(202) // Accepted
        .body("operationId", notNullValue())
        .body("status", equalTo("PENDING"))
        .body("message", equalTo("User update request submitted"))
        .body("trackingUrl", notNullValue())
        .body("trackingUrl", containsString("/v2/operations/"))
        .body("trackingUrl", containsString("/status"))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue())
        .extract()
        .response();

    // Store the operation ID for subsequent tests
    updatedOperationId = response.jsonPath().getString("operationId");
    assertThat(updatedOperationId).isNotNull();
    System.out.println("Updated operation ID: " + updatedOperationId);
  }

  @Test
  @Order(5)
  @DisplayName("DELETE /v2/users/{id} - Should accept delete request and return operation ID (async)")
  void testV2DeleteUserAsync() {
    String testUserId = "test-user-id-for-delete"; // Mock ID for testing

    Response response = given()
        .pathParam("id", testUserId)
        .when()
        .delete("/v2/users/{id}")
        .then()
        .statusCode(202) // Accepted
        .body("operationId", notNullValue())
        .body("status", equalTo("PENDING"))
        .body("message", equalTo("User deletion request submitted"))
        .body("trackingUrl", notNullValue())
        .body("trackingUrl", containsString("/v2/operations/"))
        .body("trackingUrl", containsString("/status"))
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue())
        .extract()
        .response();

    // Store the operation ID for subsequent tests
    deletedOperationId = response.jsonPath().getString("operationId");
    assertThat(deletedOperationId).isNotNull();
    System.out.println("Deleted operation ID: " + deletedOperationId);
  }

  @Test
  @Order(6)
  @DisplayName("GET /v2/operations/{operationId}/status - Should return operation status for all operations")
  void testV2GetAllOperationStatuses() {
    // Test create operation status
    assertThat(createdOperationId).isNotNull();
    given()
        .pathParam("operationId", createdOperationId)
        .when()
        .get("/v2/operations/{operationId}/status")
        .then()
        .statusCode(200)
        .body("operationId", equalTo(createdOperationId));

    // Test update operation status
    assertThat(updatedOperationId).isNotNull();
    given()
        .pathParam("operationId", updatedOperationId)
        .when()
        .get("/v2/operations/{operationId}/status")
        .then()
        .statusCode(200)
        .body("operationId", equalTo(updatedOperationId));

    // Test delete operation status
    assertThat(deletedOperationId).isNotNull();
    given()
        .pathParam("operationId", deletedOperationId)
        .when()
        .get("/v2/operations/{operationId}/status")
        .then()
        .statusCode(200)
        .body("operationId", equalTo(deletedOperationId));
  }

  @Test
  @Order(7)
  @DisplayName("GET /v2/operations/{operationId}/status - Should return 404 for non-existent operation")
  void testV2GetNonExistentOperationStatus() {
    String nonExistentOperationId = "non-existent-operation-id-12345";

    given()
        .pathParam("operationId", nonExistentOperationId)
        .when()
        .get("/v2/operations/{operationId}/status")
        .then()
        .statusCode(404);
  }

  // Test invalid requests
  @Test
  @Order(8)
  @DisplayName("POST /v2/users - Should return validation error for invalid input (async)")
  void testV2CreateUserWithInvalidInputAsync() {
    CreateUserRequest request = new CreateUserRequest();
    request.setName(""); // Invalid: empty name
    request.setPhone("invalid-phone"); // Invalid: not a valid phone format
    request.setAddress("Test Address");

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/v2/users")
        .then()
        .statusCode(400); // Bad Request due to validation errors
  }

  @Test
  @Order(9)
  @DisplayName("PUT /v2/users/{id} - Should return validation error for invalid input (async)")
  void testV2UpdateUserWithInvalidInputAsync() {
    String testUserId = "test-user-id";
    UpdateUserRequest request = new UpdateUserRequest();
    request.setName(""); // Invalid: empty name
    request.setPhone("invalid-phone"); // Invalid: not a valid phone format

    given()
        .pathParam("id", testUserId)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/v2/users/{id}")
        .then()
        .statusCode(400); // Bad Request due to validation errors
  }

  // Test that V1 endpoints still work (backward compatibility)
  @Test
  @Order(10)
  @DisplayName("GET /users/ping - Should still work (V1 compatibility)")
  void testV1CompatibilityPing() {
    given()
        .when()
        .get("/users/ping")
        .then()
        .statusCode(200)
        .body(equalTo("pong"));
  }

  @Test
  @Order(11)
  @DisplayName("GET /users - Should still work (V1 compatibility)")
  void testV1CompatibilityListUsers() {
    given()
        .when()
        .get("/users?page=0&size=10")
        .then()
        .statusCode(200)
        .body("status", equalTo(0))
        .body("message", equalTo("Users retrieved successfully"))
        .body("items", notNullValue());
  }

  // Test sync GET endpoints for V2 with specific ID
  @Test
  @Order(12)
  @DisplayName("GET /v2/users/{id} - Should return 404 for non-existent user (sync)")
  void testV2GetNonExistentUser() {
    String nonExistentId = "non-existent-id-12345";

    given()
        .pathParam("id", nonExistentId)
        .when()
        .get("/v2/users/{id}")
        .then()
        .statusCode(404)
        .body("status", equalTo(2)) // USER_NOT_FOUND
        .body("message", equalTo("User not found"))
        .body("user", nullValue())
        .body("timestamp", notNullValue())
        .body("correlationId", notNullValue());
  }
}
