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
 * Approval Request smoke tests for E2E testing.
 * <p>
 * Tests critical ApprovalRequest functionality including:
 * - Request creation for orphaned services
 * - Admin approval workflow
 * - Service ownership transfer
 * - Request status tracking
 * </p>
 */
@Slf4j
@Epic("Approval Requests")
@Feature("Ownership Workflow")
@DisplayName("Approval Request Smoke Tests")
public class ApprovalRequestSmokeTest extends BaseE2ETest {

    private String createdRequestId;
    private String orphanedServiceId;

    @Test
    @Story("Request Creation")
    @Description("Verify that users can create approval requests for orphaned services")
    @DisplayName("Should create approval request for orphaned service")
    void shouldCreateApprovalRequestForOrphanedService() {
        logTestStep("Request Creation", "Verify approval request creation for orphaned services");

        // First create an orphaned service (no ownerTeamId)
        String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
        Map<String, Object> createOrphanedRequest = Map.of(
                "displayName", orphanedServiceName,
                "description", "E2E test orphaned service for approval request",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "approval-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createOrphanedRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        orphanedServiceId = serviceResponse.jsonPath().getString("id");
        logTestData("Created Orphaned Service", orphanedServiceId);

        // User5 (no team) should be able to create approval request
        Map<String, Object> createApprovalRequest = Map.of(
                "serviceId", orphanedServiceId,
                "targetTeamId", TestUsers.TEAM2
        );

        Response approvalResponse = ApiClient.given(getUser5Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("serviceId", equalTo(orphanedServiceId))
                .body("targetTeamId", equalTo(TestUsers.TEAM2))
                .body("status", equalTo("PENDING"))
                .extract().response();

        createdRequestId = approvalResponse.jsonPath().getString("id");
        logTestData("Created Approval Request", createdRequestId);

        // Verify request can be retrieved
        ApiClient.given(getUser5Token())
                .when()
                .get("/approval-requests/" + createdRequestId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdRequestId))
                .body("serviceId", equalTo(orphanedServiceId));

        logTestResult("Request Creation", "Approval request created successfully");
    }

    @Test
    @Story("Request Listing")
    @Description("Verify that users can list approval requests with proper filtering")
    @DisplayName("Should list approval requests with user filtering")
    void shouldListApprovalRequestsWithUserFiltering() {
        logTestStep("Request Listing", "Verify approval request listing with user filtering");

        // Test admin can see all requests
        Response adminResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/approval-requests")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("metadata.totalElements", greaterThanOrEqualTo(0))
                .extract().response();

        int adminRequestCount = adminResponse.jsonPath().getInt("metadata.totalElements");
        logTestData("Admin Request Count", String.valueOf(adminRequestCount));

        // Test user5 can see their own requests
        Response user5Response = ApiClient.given(getUser5Token())
                .when()
                .get("/approval-requests")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("metadata.totalElements", greaterThanOrEqualTo(0))
                .extract().response();

        int user5RequestCount = user5Response.jsonPath().getInt("metadata.totalElements");
        logTestData("User5 Request Count", String.valueOf(user5RequestCount));

        // Admin should see more or equal requests than regular users
        assertTrue(adminRequestCount >= user5RequestCount, 
                "Admin should see more or equal requests than regular users");

        logTestResult("Request Listing", "Request listing with user filtering working correctly");
    }

    @Test
    @Story("Admin Approval")
    @Description("Verify that admin can approve approval requests")
    @DisplayName("Should approve approval request as admin")
    void shouldApproveApprovalRequestAsAdmin() {
        logTestStep("Admin Approval", "Verify admin can approve approval requests");

        // Create an orphaned service
        String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
        Map<String, Object> createOrphanedRequest = Map.of(
                "displayName", orphanedServiceName,
                "description", "E2E test orphaned service for admin approval",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "admin-approval-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createOrphanedRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // User5 creates approval request
        Map<String, Object> createApprovalRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM2
        );

        Response approvalResponse = ApiClient.given(getUser5Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(201)
                .extract().response();

        String requestId = approvalResponse.jsonPath().getString("id");

        // Admin approves the request
        Map<String, Object> approveRequest = Map.of(
                "decision", "APPROVE",
                "gate", "SYS_ADMIN",
                "note", "Approved for E2E testing"
        );

        Response decisionResponse = ApiClient.given(getAdminToken())
                .body(approveRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("decision", equalTo("APPROVE"))
                .body("gate", equalTo("SYS_ADMIN"))
                .extract().response();

        logTestData("Approval Decision", decisionResponse.jsonPath().getString("id"));

        // Verify request status is updated
        ApiClient.given(getAdminToken())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));

        // Verify service ownership is transferred
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("ownerTeamId", equalTo(TestUsers.TEAM2));

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Admin Approval", "Approval request approved successfully by admin");
    }

    @Test
    @Story("Request Status Tracking")
    @Description("Verify that approval request status is tracked correctly")
    @DisplayName("Should track approval request status correctly")
    void shouldTrackApprovalRequestStatusCorrectly() {
        logTestStep("Status Tracking", "Verify approval request status tracking");

        // Create an orphaned service
        String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
        Map<String, Object> createOrphanedRequest = Map.of(
                "displayName", orphanedServiceName,
                "description", "E2E test orphaned service for status tracking",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "status-tracking-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createOrphanedRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // User5 creates approval request
        Map<String, Object> createApprovalRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM1
        );

        Response approvalResponse = ApiClient.given(getUser5Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(201)
                .extract().response();

        String requestId = approvalResponse.jsonPath().getString("id");

        // Verify initial status is PENDING
        ApiClient.given(getUser5Token())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING"));

        // Admin approves the request
        Map<String, Object> approveRequest = Map.of(
                "decision", "APPROVE",
                "gate", "SYS_ADMIN",
                "note", "Approved for status tracking test"
        );

        ApiClient.given(getAdminToken())
                .body(approveRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(201);

        // Verify status is updated to APPROVED
        ApiClient.given(getUser5Token())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Status Tracking", "Approval request status tracked correctly");
    }

    @Test
    @Story("Request Cancellation")
    @Description("Verify that requesters can cancel their own requests")
    @DisplayName("Should cancel approval request as requester")
    void shouldCancelApprovalRequestAsRequester() {
        logTestStep("Request Cancellation", "Verify approval request cancellation by requester");

        // Create an orphaned service
        String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
        Map<String, Object> createOrphanedRequest = Map.of(
                "displayName", orphanedServiceName,
                "description", "E2E test orphaned service for cancellation",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "cancellation-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createOrphanedRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // User5 creates approval request
        Map<String, Object> createApprovalRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM1
        );

        Response approvalResponse = ApiClient.given(getUser5Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(201)
                .extract().response();

        String requestId = approvalResponse.jsonPath().getString("id");

        // User5 cancels the request
        ApiClient.given(getUser5Token())
                .when()
                .delete("/approval-requests/" + requestId)
                .then()
                .statusCode(204);

        // Verify request is cancelled
        ApiClient.given(getUser5Token())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"));

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Request Cancellation", "Approval request cancelled successfully by requester");
    }

    @Test
    @Story("Unauthorized Request Creation")
    @Description("Verify that users cannot create requests for services they don't have access to")
    @DisplayName("Should reject unauthorized request creation")
    void shouldRejectUnauthorizedRequestCreation() {
        logTestStep("Unauthorized Request Creation", "Verify rejection of unauthorized request creation");

        // Create a service owned by team1
        String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        Map<String, Object> createServiceRequest = Map.of(
                "displayName", serviceName,
                "description", "E2E test service for unauthorized request test",
                "lifecycle", "ACTIVE",
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "unauthorized-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // User5 (no team) should not be able to create request for team1 service
        Map<String, Object> createApprovalRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM2
        );

        ApiClient.given(getUser5Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(403);

        // User3 (team2) should not be able to create request for team1 service
        ApiClient.given(getUser3Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(403);

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Unauthorized Request Creation", "Unauthorized request creation properly rejected");
    }
}
