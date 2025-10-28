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
 * Service Ownership workflow tests for E2E testing.
 * <p>
 * Tests complete service ownership transfer workflow including:
 * - User requests ownership of orphaned service
 * - Admin approves the request
 * - Service ownership is transferred
 * - User can now manage the service
 * </p>
 */
@Slf4j
@Epic("Service Ownership")
@Feature("Ownership Transfer Workflow")
@DisplayName("Service Ownership Workflow Tests")
public class ServiceOwnershipWorkflowTest extends BaseE2ETest {

        @Test
        @Story("Complete Ownership Transfer")
        @Description("Verify complete service ownership transfer workflow from request to approval to transfer")
        @DisplayName("Should complete full service ownership transfer workflow")
        void shouldCompleteFullServiceOwnershipTransferWorkflow() {
                logTestStep("Service Ownership Workflow", "Execute complete service ownership transfer workflow");

                // Step 1: Create an orphaned service
                String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
                Map<String, Object> createOrphanedRequest = Map.of(
                                "displayName", orphanedServiceName,
                                "description", "E2E test orphaned service for ownership transfer workflow",
                                "lifecycle", "ACTIVE",
                                "tags", TestDataGenerator.generateStringList(3, "ownership-workflow"));

                Response serviceResponse = ApiClient.given(getAdminToken())
                                .body(createOrphanedRequest)
                                .when()
                                .post("/application-services")
                                .then()
                                .statusCode(201)
                                .body("id", notNullValue())
                                .body("ownerTeamId", nullValue())
                                .extract().response();

                String serviceId = serviceResponse.jsonPath().getString("id");
                logTestData("Created Orphaned Service", serviceId);

                // Verify service is orphaned and visible to all users
                ApiClient.given(getUser5Token())
                                .when()
                                .get("/application-services/" + serviceId)
                                .then()
                                .statusCode(200)
                                .body("ownerTeamId", nullValue());

                // Step 2: User5 requests ownership for team2
                Map<String, Object> createApprovalRequest = Map.of(
                                "serviceId", serviceId,
                                "targetTeamId", TestUsers.TEAM2);

                Response approvalResponse = ApiClient.given(getUser5Token())
                                .body(createApprovalRequest)
                                .when()
                                .post("/approval-requests")
                                .then()
                                .statusCode(201)
                                .body("id", notNullValue())
                                .body("serviceId", equalTo(serviceId))
                                .body("targetTeamId", equalTo(TestUsers.TEAM2))
                                .body("status", equalTo("PENDING"))
                                .extract().response();

                String requestId = approvalResponse.jsonPath().getString("id");
                logTestData("Created Approval Request", requestId);

                // Step 3: Verify request is visible to admin
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/approval-requests/" + requestId)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("PENDING"));

                // Step 4: Admin approves the request
                Map<String, Object> approveRequest = Map.of(
                                "decision", "APPROVE",
                                "gate", "SYS_ADMIN",
                                "note", "Approved for E2E ownership transfer workflow test");

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

                // Step 5: Verify request status is updated
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/approval-requests/" + requestId)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("APPROVED"));

                // Step 6: Verify service ownership is transferred
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/application-services/" + serviceId)
                                .then()
                                .statusCode(200)
                                .body("ownerTeamId", equalTo(TestUsers.TEAM2));

                // Step 7: Verify team2 members can now access the service
                ApiClient.given(getUser3Token())
                                .when()
                                .get("/application-services/" + serviceId)
                                .then()
                                .statusCode(200)
                                .body("ownerTeamId", equalTo(TestUsers.TEAM2));

                ApiClient.given(getUser4Token())
                                .when()
                                .get("/application-services/" + serviceId)
                                .then()
                                .statusCode(200)
                                .body("ownerTeamId", equalTo(TestUsers.TEAM2));

                // Step 8: Verify team1 members cannot access the service
                ApiClient.given(getUser1Token())
                                .when()
                                .get("/application-services/" + serviceId)
                                .then()
                                .statusCode(404);

                ApiClient.given(getUser2Token())
                                .when()
                                .get("/application-services/" + serviceId)
                                .then()
                                .statusCode(404);

                // Step 9: Verify team2 can create instances for the service
                String instanceId = TestDataGenerator.generateInstanceId();
                Map<String, Object> createInstanceRequest = Map.of(
                                "serviceName", orphanedServiceName,
                                "instanceId", instanceId,
                                "serviceId", serviceId,
                                "host", TestDataGenerator.generateTestHost(),
                                "port", TestDataGenerator.generateTestPort(),
                                "environment", "dev",
                                "version", TestDataGenerator.generateTestVersion());

                ApiClient.given(getUser3Token())
                                .body(createInstanceRequest)
                                .when()
                                .post("/service-instances")
                                .then()
                                .statusCode(201)
                                .body("instanceId", equalTo(instanceId));

                // Step 10: Verify team2 can manage the service
                Map<String, Object> updateServiceRequest = Map.of(
                                "displayName", orphanedServiceName + " (Updated)",
                                "description", "Updated description by team2",
                                "lifecycle", "ACTIVE");

                ApiClient.given(getUser3Token())
                                .body(updateServiceRequest)
                                .when()
                                .put("/application-services/" + serviceId)
                                .then()
                                .statusCode(200)
                                .body("displayName", equalTo(orphanedServiceName + " (Updated)"));

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

                logTestResult("Service Ownership Workflow",
                                "Complete service ownership transfer workflow executed successfully");
        }

        @Test
        @Story("Ownership Request Validation")
        @Description("Verify that ownership requests are properly validated")
        @DisplayName("Should validate ownership request requirements")
        void shouldValidateOwnershipRequestRequirements() {
                logTestStep("Ownership Request Validation", "Verify ownership request validation");

                // Create an orphaned service
                String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
                Map<String, Object> createOrphanedRequest = Map.of(
                                "displayName", orphanedServiceName,
                                "description", "E2E test orphaned service for request validation",
                                "lifecycle", "ACTIVE",
                                "tags", TestDataGenerator.generateStringList(2, "validation-test"));

                Response serviceResponse = ApiClient.given(getAdminToken())
                                .body(createOrphanedRequest)
                                .when()
                                .post("/application-services")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String serviceId = serviceResponse.jsonPath().getString("id");

                // Test 1: Missing serviceId should fail
                Map<String, Object> invalidRequest1 = Map.of(
                                "targetTeamId", TestUsers.TEAM2);

                ApiClient.given(getUser5Token())
                                .body(invalidRequest1)
                                .when()
                                .post("/approval-requests")
                                .then()
                                .statusCode(400);

                // Test 2: Missing targetTeamId should fail
                Map<String, Object> invalidRequest2 = Map.of(
                                "serviceId", serviceId);

                ApiClient.given(getUser5Token())
                                .body(invalidRequest2)
                                .when()
                                .post("/approval-requests")
                                .then()
                                .statusCode(400);

                // Test 3: Invalid serviceId should fail
                Map<String, Object> invalidRequest3 = Map.of(
                                "serviceId", "invalid-service-id",
                                "targetTeamId", TestUsers.TEAM2);

                ApiClient.given(getUser5Token())
                                .body(invalidRequest3)
                                .when()
                                .post("/approval-requests")
                                .then()
                                .statusCode(404);

                // Test 4: Valid request should succeed
                Map<String, Object> validRequest = Map.of(
                                "serviceId", serviceId,
                                "targetTeamId", TestUsers.TEAM2);

                Response validResponse = ApiClient.given(getUser5Token())
                                .body(validRequest)
                                .when()
                                .post("/approval-requests")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String requestId = validResponse.jsonPath().getString("id");

                // Clean up
                ApiClient.given(getAdminToken())
                                .when()
                                .delete("/approval-requests/" + requestId)
                                .then()
                                .statusCode(anyOf(is(204), is(404)));

                ApiClient.given(getAdminToken())
                                .when()
                                .delete("/application-services/" + serviceId)
                                .then()
                                .statusCode(anyOf(is(204), is(404)));

                logTestResult("Ownership Request Validation", "Ownership request validation working correctly");
        }

        @Test
        @Story("Multiple Ownership Requests")
        @Description("Verify handling of multiple ownership requests for the same service")
        @DisplayName("Should handle multiple ownership requests correctly")
        void shouldHandleMultipleOwnershipRequestsCorrectly() {
                logTestStep("Multiple Ownership Requests", "Verify handling of multiple ownership requests");

                // Create an orphaned service
                String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
                Map<String, Object> createOrphanedRequest = Map.of(
                                "displayName", orphanedServiceName,
                                "description", "E2E test orphaned service for multiple requests",
                                "lifecycle", "ACTIVE",
                                "tags", TestDataGenerator.generateStringList(2, "multiple-requests-test"));

                Response serviceResponse = ApiClient.given(getAdminToken())
                                .body(createOrphanedRequest)
                                .when()
                                .post("/application-services")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String serviceId = serviceResponse.jsonPath().getString("id");

                // User5 requests ownership for team1
                Map<String, Object> request1 = Map.of(
                                "serviceId", serviceId,
                                "targetTeamId", TestUsers.TEAM1);

                Response response1 = ApiClient.given(getUser5Token())
                                .body(request1)
                                .when()
                                .post("/approval-requests")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String requestId1 = response1.jsonPath().getString("id");

                // User5 requests ownership for team2 (should be allowed)
                Map<String, Object> request2 = Map.of(
                                "serviceId", serviceId,
                                "targetTeamId", TestUsers.TEAM2);

                Response response2 = ApiClient.given(getUser5Token())
                                .body(request2)
                                .when()
                                .post("/approval-requests")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String requestId2 = response2.jsonPath().getString("id");

                // Verify both requests exist
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/approval-requests/" + requestId1)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("PENDING"));

                ApiClient.given(getAdminToken())
                                .when()
                                .get("/approval-requests/" + requestId2)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("PENDING"));

                // Approve first request
                Map<String, Object> approveRequest1 = Map.of(
                                "decision", "APPROVE",
                                "gate", "SYS_ADMIN",
                                "note", "Approved first request");

                ApiClient.given(getAdminToken())
                                .body(approveRequest1)
                                .when()
                                .post("/approval-requests/" + requestId1 + "/decisions")
                                .then()
                                .statusCode(201);

                // Verify service ownership is transferred to team1
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/application-services/" + serviceId)
                                .then()
                                .statusCode(200)
                                .body("ownerTeamId", equalTo(TestUsers.TEAM1));

                // Verify second request is still pending
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/approval-requests/" + requestId2)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("PENDING"));

                // Clean up
                ApiClient.given(getAdminToken())
                                .when()
                                .delete("/approval-requests/" + requestId1)
                                .then()
                                .statusCode(anyOf(is(204), is(404)));

                ApiClient.given(getAdminToken())
                                .when()
                                .delete("/approval-requests/" + requestId2)
                                .then()
                                .statusCode(anyOf(is(204), is(404)));

                ApiClient.given(getAdminToken())
                                .when()
                                .delete("/application-services/" + serviceId)
                                .then()
                                .statusCode(anyOf(is(204), is(404)));

                logTestResult("Multiple Ownership Requests", "Multiple ownership requests handled correctly");
        }

        @Test
        @Story("Ownership Transfer Audit")
        @Description("Verify that ownership transfers are properly audited")
        @DisplayName("Should audit ownership transfer process")
        void shouldAuditOwnershipTransferProcess() {
                logTestStep("Ownership Transfer Audit", "Verify ownership transfer audit trail");

                // Create an orphaned service
                String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
                Map<String, Object> createOrphanedRequest = Map.of(
                                "displayName", orphanedServiceName,
                                "description", "E2E test orphaned service for audit trail",
                                "lifecycle", "ACTIVE",
                                "tags", TestDataGenerator.generateStringList(2, "audit-test"));

                Response serviceResponse = ApiClient.given(getAdminToken())
                                .body(createOrphanedRequest)
                                .when()
                                .post("/application-services")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String serviceId = serviceResponse.jsonPath().getString("id");

                // User5 requests ownership
                Map<String, Object> createApprovalRequest = Map.of(
                                "serviceId", serviceId,
                                "targetTeamId", TestUsers.TEAM2);

                Response approvalResponse = ApiClient.given(getUser5Token())
                                .body(createApprovalRequest)
                                .when()
                                .post("/approval-requests")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String requestId = approvalResponse.jsonPath().getString("id");

                // Admin approves with note
                String approvalNote = "Approved for audit trail testing";
                Map<String, Object> approveRequest = Map.of(
                                "decision", "APPROVE",
                                "gate", "SYS_ADMIN",
                                "note", approvalNote);

                Response decisionResponse = ApiClient.given(getAdminToken())
                                .body(approveRequest)
                                .when()
                                .post("/approval-requests/" + requestId + "/decisions")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String decisionId = decisionResponse.jsonPath().getString("id");

                // Verify decision details are audited
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/approval-requests/" + requestId + "/decisions/" + decisionId)
                                .then()
                                .statusCode(200)
                                .body("decision", equalTo("APPROVE"))
                                .body("gate", equalTo("SYS_ADMIN"))
                                .body("note", equalTo(approvalNote))
                                .body("createdAt", notNullValue());

                // Verify request history
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/approval-requests/" + requestId)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("APPROVED"))
                                .body("decisions", notNullValue())
                                .body("decisions.size()", greaterThan(0));

                // Clean up
                ApiClient.given(getAdminToken())
                                .when()
                                .delete("/application-services/" + serviceId)
                                .then()
                                .statusCode(anyOf(is(204), is(404)));

                logTestResult("Ownership Transfer Audit", "Ownership transfer audit trail working correctly");
        }
}
