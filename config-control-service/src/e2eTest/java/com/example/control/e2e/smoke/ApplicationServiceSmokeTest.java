package com.example.control.e2e.smoke;

import com.example.control.e2e.base.BaseE2ETest;
import com.example.control.e2e.client.ApiClient;
import com.example.control.e2e.fixtures.TestDataGenerator;
import com.example.control.e2e.fixtures.TestUsers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Application Service smoke tests for E2E testing.
 * <p>
 * Tests critical ApplicationService functionality including:
 * - Service listing with visibility filtering
 * - Service creation by admin
 * - Service visibility for different user roles
 * - Basic CRUD operations
 * </p>
 */
@Slf4j
@Epic("Application Services")
@Feature("Service Management")
@DisplayName("Application Service Smoke Tests")
public class ApplicationServiceSmokeTest extends BaseE2ETest {

    private String createdServiceId;

    @Test
    @Story("Service Listing")
    @Description("Verify that users can list application services with proper visibility filtering")
    @DisplayName("Should list application services with visibility filtering")
    void shouldListApplicationServicesWithVisibilityFiltering() {
        logTestStep("Service Listing", "Verify application service listing with visibility filtering");

        // Test admin can see all services
        Response adminResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("metadata.totalElements", greaterThanOrEqualTo(0))
                .extract().response();

        int adminServiceCount = adminResponse.jsonPath().getInt("metadata.totalElements");
        logTestData("Admin Service Count", String.valueOf(adminServiceCount));

        // Test user1 can see services (filtered by visibility)
        Response user1Response = ApiClient.given(getUser1Token())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("metadata.totalElements", greaterThanOrEqualTo(0))
                .extract().response();

        int user1ServiceCount = user1Response.jsonPath().getInt("metadata.totalElements");
        logTestData("User1 Service Count", String.valueOf(user1ServiceCount));

        // Admin should see more or equal services than regular users
        assertTrue(adminServiceCount >= user1ServiceCount, 
                "Admin should see more or equal services than regular users");

        logTestResult("Service Listing", "Service listing with visibility filtering working correctly");
    }

    @Test
    @Story("Service Creation")
    @Description("Verify that admin can create new application services")
    @DisplayName("Should create application service as admin")
    void shouldCreateApplicationServiceAsAdmin() {
        logTestStep("Service Creation", "Verify admin can create new application services");

        String serviceName = TestDataGenerator.generateServiceName();
        Map<String, Object> createServiceRequest = Map.of(
                "displayName", serviceName,
                "description", "E2E test service created by admin",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "test-tag"),
                "environments", TestDataGenerator.generateStringList(3, "env")
        );

        Response response = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("displayName", equalTo(serviceName))
                .body("description", equalTo("E2E test service created by admin"))
                .body("lifecycle", equalTo("ACTIVE"))
                .extract().response();

        createdServiceId = response.jsonPath().getString("id");
        logTestData("Created Service ID", createdServiceId);
        logTestData("Created Service Name", serviceName);

        // Verify service can be retrieved
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + createdServiceId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdServiceId))
                .body("displayName", equalTo(serviceName));

        logTestResult("Service Creation", "Service created successfully by admin");
    }

    @Test
    @Story("Service Visibility")
    @Description("Verify that users can only see services they have access to")
    @DisplayName("Should enforce service visibility rules")
    void shouldEnforceServiceVisibilityRules() {
        logTestStep("Service Visibility", "Verify service visibility rules for different users");

        // Create a service as admin
        String serviceName = TestDataGenerator.generateServiceName();
        Map<String, Object> createServiceRequest = Map.of(
                "displayName", serviceName,
                "description", "E2E test service for visibility testing",
                "lifecycle", "ACTIVE",
                "ownerTeamId", TestUsers.TEAM1, // Assign to team1
                "tags", TestDataGenerator.generateStringList(2, "visibility-test")
        );

        Response createResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = createResponse.jsonPath().getString("id");
        logTestData("Created Service for Visibility Test", serviceId);

        // User1 (team1) should be able to see the service
        Response user1Response = ApiClient.given(getUser1Token())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .extract().response();

        boolean user1CanSeeService = user1Response.jsonPath().getList("items.id").contains(serviceId);
        assertTrue(user1CanSeeService, "User1 (team1) should be able to see team1 service");

        // User3 (team2) should not be able to see the service (unless shared)
        Response user3Response = ApiClient.given(getUser3Token())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .extract().response();

        boolean user3CanSeeService = user3Response.jsonPath().getList("items.id").contains(serviceId);
        assertFalse(user3CanSeeService, "User3 (team2) should not be able to see team1 service");

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Service Visibility", "Service visibility rules enforced correctly");
    }

    @Test
    @Story("Orphaned Services")
    @Description("Verify that orphaned services are visible to all authenticated users")
    @DisplayName("Should show orphaned services to all users")
    void shouldShowOrphanedServicesToAllUsers() {
        logTestStep("Orphaned Services", "Verify orphaned services are visible to all authenticated users");

        // Create an orphaned service (no ownerTeamId)
        String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
        Map<String, Object> createOrphanedRequest = Map.of(
                "displayName", orphanedServiceName,
                "description", "E2E test orphaned service",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "orphaned-test")
        );

        Response createResponse = ApiClient.given(getAdminToken())
                .body(createOrphanedRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String orphanedServiceId = createResponse.jsonPath().getString("id");
        logTestData("Created Orphaned Service", orphanedServiceId);

        // All users should be able to see orphaned services
        String[] testUsers = {TestUsers.USER1, TestUsers.USER2, TestUsers.USER3, TestUsers.USER4, TestUsers.USER5};
        String[] tokens = {getUser1Token(), getUser2Token(), getUser3Token(), getUser4Token(), getUser5Token()};

        for (int i = 0; i < testUsers.length; i++) {
            Response userResponse = ApiClient.given(tokens[i])
                    .when()
                    .get("/application-services")
                    .then()
                    .statusCode(200)
                    .extract().response();

            boolean canSeeOrphaned = userResponse.jsonPath().getList("items.id").contains(orphanedServiceId);
            assertTrue(canSeeOrphaned, 
                    String.format("%s should be able to see orphaned service", testUsers[i]));
        }

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + orphanedServiceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Orphaned Services", "Orphaned services visible to all authenticated users");
    }

    @Test
    @Story("Service Retrieval")
    @Description("Verify that users can retrieve specific services they have access to")
    @DisplayName("Should retrieve accessible services by ID")
    void shouldRetrieveAccessibleServicesById() {
        logTestStep("Service Retrieval", "Verify service retrieval by ID for accessible services");

        // Create a service owned by team1
        String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        Map<String, Object> createServiceRequest = Map.of(
                "displayName", serviceName,
                "description", "E2E test service for retrieval testing",
                "lifecycle", "ACTIVE",
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "retrieval-test")
        );

        Response createResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = createResponse.jsonPath().getString("id");
        logTestData("Created Service for Retrieval Test", serviceId);

        // User1 (team1) should be able to retrieve the service
        ApiClient.given(getUser1Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("id", equalTo(serviceId))
                .body("displayName", equalTo(serviceName));

        // User2 (team1) should also be able to retrieve the service
        ApiClient.given(getUser2Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("id", equalTo(serviceId));

        // User3 (team2) should not be able to retrieve the service
        ApiClient.given(getUser3Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(404);

        // Admin should be able to retrieve any service
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("id", equalTo(serviceId));

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Service Retrieval", "Service retrieval by ID working correctly");
    }

    @Test
    @Story("Service Deletion")
    @Description("Verify that only admin can delete application services")
    @DisplayName("Should restrict service deletion to admin only")
    void shouldRestrictServiceDeletionToAdminOnly() {
        logTestStep("Service Deletion", "Verify service deletion is restricted to admin only");

        // Create a service
        String serviceName = TestDataGenerator.generateServiceName();
        Map<String, Object> createServiceRequest = Map.of(
                "displayName", serviceName,
                "description", "E2E test service for deletion testing",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "deletion-test")
        );

        Response createResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = createResponse.jsonPath().getString("id");
        logTestData("Created Service for Deletion Test", serviceId);

        // User1 should not be able to delete the service
        ApiClient.given(getUser1Token())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(403);

        // User3 should not be able to delete the service
        ApiClient.given(getUser3Token())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(403);

        // Admin should be able to delete the service
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(204);

        // Verify service is deleted
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(404);

        logTestResult("Service Deletion", "Service deletion restricted to admin only");
    }
}
