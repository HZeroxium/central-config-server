package com.example.control.e2e.workflows;

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
 * Service Sharing workflow tests for E2E testing.
 * <p>
 * Tests complete service sharing workflow including:
 * - Team1 shares service with team2
 * - Team2 can view shared service
 * - Team2 can create instances for shared service
 * - Team1 can revoke sharing
 * </p>
 */
@Slf4j
@Epic("Service Sharing")
@Feature("Cross-Team Sharing Workflow")
@DisplayName("Service Sharing Workflow Tests")
public class ServiceSharingWorkflowTest extends BaseE2ETest {

    @Test
    @Story("Complete Sharing Workflow")
    @Description("Verify complete service sharing workflow from grant to revoke")
    @DisplayName("Should complete full service sharing workflow")
    void shouldCompleteFullServiceSharingWorkflow() {
        logTestStep("Service Sharing Workflow", "Execute complete service sharing workflow");

        // Step 1: Create a service owned by team1
        String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        Map<String, Object> createServiceRequest = Map.of(
                "displayName", serviceName,
                "description", "E2E test service for sharing workflow",
                "lifecycle", "ACTIVE",
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(3, "sharing-workflow")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("ownerTeamId", equalTo(TestUsers.TEAM1))
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");
        logTestData("Created Team1 Service", serviceId);

        // Step 2: Verify team2 cannot access the service initially
        ApiClient.given(getUser3Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(404);

        // Step 3: Team1 shares the service with team2
        Map<String, Object> shareRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM2,
                "permissions", TestDataGenerator.generateStringList(2, "VIEW")
        );

        Response shareResponse = ApiClient.given(getUser1Token())
                .body(shareRequest)
                .when()
                .post("/service-shares")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("serviceId", equalTo(serviceId))
                .body("targetTeamId", equalTo(TestUsers.TEAM2))
                .extract().response();

        String shareId = shareResponse.jsonPath().getString("id");
        logTestData("Created Service Share", shareId);

        // Step 4: Verify team2 can now access the shared service
        ApiClient.given(getUser3Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("id", equalTo(serviceId))
                .body("ownerTeamId", equalTo(TestUsers.TEAM1));

        ApiClient.given(getUser4Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("id", equalTo(serviceId));

        // Step 5: Verify team2 can see the service in their service list
        Response team2ServicesResponse = ApiClient.given(getUser3Token())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .extract().response();

        boolean team2CanSeeService = team2ServicesResponse.jsonPath().getList("items.id").contains(serviceId);
        assertTrue(team2CanSeeService, "Team2 should be able to see shared service in their list");

        // Step 6: Verify team2 can create instances for the shared service
        String instanceId = TestDataGenerator.generateInstanceId();
        Map<String, Object> createInstanceRequest = Map.of(
                "instanceId", instanceId,
                "serviceId", serviceId,
                "host", TestDataGenerator.generateTestHost(),
                "port", TestDataGenerator.generateTestPort(),
                "environment", "dev",
                "version", TestDataGenerator.generateTestVersion()
        );

        ApiClient.given(getUser3Token())
                .body(createInstanceRequest)
                .when()
                .post("/service-instances")
                .then()
                .statusCode(201)
                .body("instanceId", equalTo(instanceId));

        // Step 7: Verify team2 can view instances for the shared service
        ApiClient.given(getUser3Token())
                .when()
                .get("/service-instances/" + instanceId)
                .then()
                .statusCode(200)
                .body("instanceId", equalTo(instanceId))
                .body("serviceId", equalTo(serviceId));

        // Step 8: Verify team1 can still manage the service
        Map<String, Object> updateServiceRequest = Map.of(
                "displayName", serviceName + " (Updated by Team1)",
                "description", "Updated description by team1",
                "lifecycle", "ACTIVE"
        );

        ApiClient.given(getUser1Token())
                .body(updateServiceRequest)
                .when()
                .put("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("displayName", equalTo(serviceName + " (Updated by Team1)"));

        // Step 9: Team1 revokes the sharing
        ApiClient.given(getUser1Token())
                .when()
                .delete("/service-shares/" + shareId)
                .then()
                .statusCode(204);

        // Step 10: Verify team2 can no longer access the service
        ApiClient.given(getUser3Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(404);

        // Step 11: Verify team2 can no longer see the service in their list
        Response team2ServicesAfterRevokeResponse = ApiClient.given(getUser3Token())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .extract().response();

        boolean team2CanSeeServiceAfterRevoke = team2ServicesAfterRevokeResponse.jsonPath().getList("items.id").contains(serviceId);
        assertFalse(team2CanSeeServiceAfterRevoke, "Team2 should not be able to see service after sharing is revoked");

        // Step 12: Verify team2 can no longer access the instance
        ApiClient.given(getUser3Token())
                .when()
                .get("/service-instances/" + instanceId)
                .then()
                .statusCode(404);

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/service-instances/" + instanceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Service Sharing Workflow", "Complete service sharing workflow executed successfully");
    }

    @Test
    @Story("Sharing Permissions")
    @Description("Verify that different sharing permissions work correctly")
    @DisplayName("Should handle different sharing permissions")
    void shouldHandleDifferentSharingPermissions() {
        logTestStep("Sharing Permissions", "Verify different sharing permissions");

        // Create a service owned by team1
        String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        Map<String, Object> createServiceRequest = Map.of(
                "displayName", serviceName,
                "description", "E2E test service for permission testing",
                "lifecycle", "ACTIVE",
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "permission-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // Test VIEW permission
        Map<String, Object> viewShareRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM2,
                "permissions", TestDataGenerator.generateStringList(1, "VIEW")
        );

        Response viewShareResponse = ApiClient.given(getUser1Token())
                .body(viewShareRequest)
                .when()
                .post("/service-shares")
                .then()
                .statusCode(201)
                .extract().response();

        String viewShareId = viewShareResponse.jsonPath().getString("id");

        // Verify team2 can view the service
        ApiClient.given(getUser3Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200);

        // Verify team2 cannot modify the service
        Map<String, Object> updateRequest = Map.of(
                "displayName", serviceName + " (Modified by Team2)",
                "description", "Modified by team2"
        );

        ApiClient.given(getUser3Token())
                .body(updateRequest)
                .when()
                .put("/application-services/" + serviceId)
                .then()
                .statusCode(403);

        // Test EDIT permission
        Map<String, Object> editShareRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM2,
                "permissions", TestDataGenerator.generateStringList(2, "EDIT")
        );

        Response editShareResponse = ApiClient.given(getUser1Token())
                .body(editShareRequest)
                .when()
                .post("/service-shares")
                .then()
                .statusCode(201)
                .extract().response();

        String editShareId = editShareResponse.jsonPath().getString("id");

        // Verify team2 can now modify the service
        ApiClient.given(getUser3Token())
                .body(updateRequest)
                .when()
                .put("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("displayName", equalTo(serviceName + " (Modified by Team2)"));

        // Clean up
        ApiClient.given(getUser1Token())
                .when()
                .delete("/service-shares/" + viewShareId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        ApiClient.given(getUser1Token())
                .when()
                .delete("/service-shares/" + editShareId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Sharing Permissions", "Different sharing permissions handled correctly");
    }

    @Test
    @Story("Sharing Validation")
    @Description("Verify that sharing requests are properly validated")
    @DisplayName("Should validate sharing request requirements")
    void shouldValidateSharingRequestRequirements() {
        logTestStep("Sharing Validation", "Verify sharing request validation");

        // Create a service owned by team1
        String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        Map<String, Object> createServiceRequest = Map.of(
                "displayName", serviceName,
                "description", "E2E test service for sharing validation",
                "lifecycle", "ACTIVE",
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "validation-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // Test 1: Missing serviceId should fail
        Map<String, Object> invalidRequest1 = Map.of(
                "targetTeamId", TestUsers.TEAM2,
                "permissions", TestDataGenerator.generateStringList(1, "VIEW")
        );

        ApiClient.given(getUser1Token())
                .body(invalidRequest1)
                .when()
                .post("/service-shares")
                .then()
                .statusCode(400);

        // Test 2: Missing targetTeamId should fail
        Map<String, Object> invalidRequest2 = Map.of(
                "serviceId", serviceId,
                "permissions", TestDataGenerator.generateStringList(1, "VIEW")
        );

        ApiClient.given(getUser1Token())
                .body(invalidRequest2)
                .when()
                .post("/service-shares")
                .then()
                .statusCode(400);

        // Test 3: Invalid serviceId should fail
        Map<String, Object> invalidRequest3 = Map.of(
                "serviceId", "invalid-service-id",
                "targetTeamId", TestUsers.TEAM2,
                "permissions", TestDataGenerator.generateStringList(1, "VIEW")
        );

        ApiClient.given(getUser1Token())
                .body(invalidRequest3)
                .when()
                .post("/service-shares")
                .then()
                .statusCode(404);

        // Test 4: User from different team should not be able to share
        Map<String, Object> unauthorizedRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM2,
                "permissions", TestDataGenerator.generateStringList(1, "VIEW")
        );

        ApiClient.given(getUser3Token())
                .body(unauthorizedRequest)
                .when()
                .post("/service-shares")
                .then()
                .statusCode(403);

        // Test 5: Valid request should succeed
        Map<String, Object> validRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM2,
                "permissions", TestDataGenerator.generateStringList(1, "VIEW")
        );

        Response validResponse = ApiClient.given(getUser1Token())
                .body(validRequest)
                .when()
                .post("/service-shares")
                .then()
                .statusCode(201)
                .extract().response();

        String shareId = validResponse.jsonPath().getString("id");

        // Clean up
        ApiClient.given(getUser1Token())
                .when()
                .delete("/service-shares/" + shareId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Sharing Validation", "Sharing request validation working correctly");
    }

    @Test
    @Story("Sharing List Management")
    @Description("Verify that sharing lists are properly managed")
    @DisplayName("Should manage sharing lists correctly")
    void shouldManageSharingListsCorrectly() {
        logTestStep("Sharing List Management", "Verify sharing list management");

        // Create a service owned by team1
        String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        Map<String, Object> createServiceRequest = Map.of(
                "displayName", serviceName,
                "description", "E2E test service for sharing list management",
                "lifecycle", "ACTIVE",
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "list-management-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // Create multiple shares
        Map<String, Object> shareToTeam2 = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM2,
                "permissions", TestDataGenerator.generateStringList(1, "VIEW")
        );

        Response shareToTeam2Response = ApiClient.given(getUser1Token())
                .body(shareToTeam2)
                .when()
                .post("/service-shares")
                .then()
                .statusCode(201)
                .extract().response();

        String shareToTeam2Id = shareToTeam2Response.jsonPath().getString("id");

        // List shares for the service
        Response sharesResponse = ApiClient.given(getUser1Token())
                .when()
                .get("/service-shares")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .extract().response();

        boolean shareExists = sharesResponse.jsonPath().getList("items.id").contains(shareToTeam2Id);
        assertTrue(shareExists, "Share should exist in the shares list");

        // List shares by service
        Response serviceSharesResponse = ApiClient.given(getUser1Token())
                .queryParam("serviceId", serviceId)
                .when()
                .get("/service-shares")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .extract().response();

        boolean serviceShareExists = serviceSharesResponse.jsonPath().getList("items.id").contains(shareToTeam2Id);
        assertTrue(serviceShareExists, "Share should exist in service-specific shares list");

        // Clean up
        ApiClient.given(getUser1Token())
                .when()
                .delete("/service-shares/" + shareToTeam2Id)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Sharing List Management", "Sharing list management working correctly");
    }
}
