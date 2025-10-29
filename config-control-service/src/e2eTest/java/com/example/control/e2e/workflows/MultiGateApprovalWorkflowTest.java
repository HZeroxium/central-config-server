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
 * Multi-Gate Approval workflow tests for E2E testing.
 * <p>
 * Tests complete multi-gate approval workflow including:
 * - User with manager creates approval request
 * - Requires both SYS_ADMIN and LINE_MANAGER gates
 * - Both gates must approve for request to be approved
 * - Service ownership transfer only after all gates approve
 * </p>
 */
@Slf4j
@Epic("Multi-Gate Approval")
@Feature("Multi-Gate Approval Workflow")
@DisplayName("Multi-Gate Approval Workflow Tests")
public class MultiGateApprovalWorkflowTest extends BaseE2ETest {

    @Test
    @Story("Complete Multi-Gate Approval Workflow")
    @Description("Verify complete multi-gate approval workflow with SYS_ADMIN and LINE_MANAGER gates")
    @DisplayName("Should complete full multi-gate approval workflow")
    void shouldCompleteFullMultiGateApprovalWorkflow() {
        logTestStep("Multi-Gate Approval Workflow", "Execute complete multi-gate approval workflow");

        // Step 1: Create an orphaned service
        String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
        Map<String, Object> createOrphanedRequest = Map.of(
                "displayName", orphanedServiceName,
                "description", "E2E test orphaned service for multi-gate approval workflow",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(3, "multi-gate-workflow")
        );

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

        // Step 2: User2 (has manager user1) requests ownership for team1
        Map<String, Object> createApprovalRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM1
        );

        Response approvalResponse = ApiClient.given(getUser2Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("serviceId", equalTo(serviceId))
                .body("targetTeamId", equalTo(TestUsers.TEAM1))
                .body("status", equalTo("PENDING"))
                .extract().response();

        String requestId = approvalResponse.jsonPath().getString("id");
        logTestData("Created Multi-Gate Approval Request", requestId);

        // Step 3: Verify request requires multiple gates
        ApiClient.given(getAdminToken())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING"))
                .body("requiredGates", notNullValue());

        // Step 4: Admin approves SYS_ADMIN gate
        Map<String, Object> adminApproveRequest = Map.of(
                "decision", "APPROVE",
                "gate", "SYS_ADMIN",
                "note", "SYS_ADMIN gate approved for E2E multi-gate workflow test"
        );

        Response adminDecisionResponse = ApiClient.given(getAdminToken())
                .body(adminApproveRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("decision", equalTo("APPROVE"))
                .body("gate", equalTo("SYS_ADMIN"))
                .extract().response();

        logTestData("SYS_ADMIN Decision", adminDecisionResponse.jsonPath().getString("id"));

        // Step 5: Verify request is still pending (waiting for LINE_MANAGER)
        ApiClient.given(getAdminToken())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING"));

        // Step 6: User1 (manager) approves LINE_MANAGER gate
        Map<String, Object> managerApproveRequest = Map.of(
                "decision", "APPROVE",
                "gate", "LINE_MANAGER",
                "note", "LINE_MANAGER gate approved for E2E multi-gate workflow test"
        );

        Response managerDecisionResponse = ApiClient.given(getUser1Token())
                .body(managerApproveRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("decision", equalTo("APPROVE"))
                .body("gate", equalTo("LINE_MANAGER"))
                .extract().response();

        logTestData("LINE_MANAGER Decision", managerDecisionResponse.jsonPath().getString("id"));

        // Step 7: Verify request is now approved
        ApiClient.given(getAdminToken())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));

        // Step 8: Verify service ownership is transferred
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("ownerTeamId", equalTo(TestUsers.TEAM1));

        // Step 9: Verify team1 members can now access the service
        ApiClient.given(getUser1Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("ownerTeamId", equalTo(TestUsers.TEAM1));

        ApiClient.given(getUser2Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("ownerTeamId", equalTo(TestUsers.TEAM1));

        // Step 10: Verify team2 members cannot access the service
        ApiClient.given(getUser3Token())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(404);

        // Step 11: Verify both decisions are recorded
        Response decisionsResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", equalTo(2))
                .extract().response();

        // Verify both gates are present
        boolean hasSysAdminDecision = decisionsResponse.jsonPath().getList("items.gate").contains("SYS_ADMIN");
        boolean hasLineManagerDecision = decisionsResponse.jsonPath().getList("items.gate").contains("LINE_MANAGER");

        assertTrue(hasSysAdminDecision, "Should have SYS_ADMIN decision");
        assertTrue(hasLineManagerDecision, "Should have LINE_MANAGER decision");

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Multi-Gate Approval Workflow", "Complete multi-gate approval workflow executed successfully");
    }

    @Test
    @Story("Partial Gate Approval")
    @Description("Verify that partial gate approval keeps request pending")
    @DisplayName("Should keep request pending with partial gate approval")
    void shouldKeepRequestPendingWithPartialGateApproval() {
        logTestStep("Partial Gate Approval", "Verify partial gate approval behavior");

        // Create an orphaned service
        String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
        Map<String, Object> createOrphanedRequest = Map.of(
                "displayName", orphanedServiceName,
                "description", "E2E test orphaned service for partial approval",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "partial-approval-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createOrphanedRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // User2 creates approval request
        Map<String, Object> createApprovalRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM1
        );

        Response approvalResponse = ApiClient.given(getUser2Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(201)
                .extract().response();

        String requestId = approvalResponse.jsonPath().getString("id");

        // Only admin approves SYS_ADMIN gate
        Map<String, Object> adminApproveRequest = Map.of(
                "decision", "APPROVE",
                "gate", "SYS_ADMIN",
                "note", "Only SYS_ADMIN gate approved"
        );

        ApiClient.given(getAdminToken())
                .body(adminApproveRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(201);

        // Verify request is still pending
        ApiClient.given(getAdminToken())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING"));

        // Verify service ownership is not transferred
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("ownerTeamId", nullValue());

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Partial Gate Approval", "Request remains pending with partial gate approval");
    }

    @Test
    @Story("Gate Rejection")
    @Description("Verify that any gate rejection rejects the entire request")
    @DisplayName("Should reject entire request when any gate is rejected")
    void shouldRejectEntireRequestWhenAnyGateIsRejected() {
        logTestStep("Gate Rejection", "Verify gate rejection behavior");

        // Create an orphaned service
        String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
        Map<String, Object> createOrphanedRequest = Map.of(
                "displayName", orphanedServiceName,
                "description", "E2E test orphaned service for gate rejection",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "gate-rejection-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createOrphanedRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // User2 creates approval request
        Map<String, Object> createApprovalRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM1
        );

        Response approvalResponse = ApiClient.given(getUser2Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(201)
                .extract().response();

        String requestId = approvalResponse.jsonPath().getString("id");

        // Admin approves SYS_ADMIN gate
        Map<String, Object> adminApproveRequest = Map.of(
                "decision", "APPROVE",
                "gate", "SYS_ADMIN",
                "note", "SYS_ADMIN gate approved"
        );

        ApiClient.given(getAdminToken())
                .body(adminApproveRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(201);

        // Manager rejects LINE_MANAGER gate
        Map<String, Object> managerRejectRequest = Map.of(
                "decision", "REJECT",
                "gate", "LINE_MANAGER",
                "note", "LINE_MANAGER gate rejected for testing"
        );

        ApiClient.given(getUser1Token())
                .body(managerRejectRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(201);

        // Verify request is rejected
        ApiClient.given(getAdminToken())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("REJECTED"));

        // Verify service ownership is not transferred
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + serviceId)
                .then()
                .statusCode(200)
                .body("ownerTeamId", nullValue());

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Gate Rejection", "Entire request rejected when any gate is rejected");
    }

    @Test
    @Story("Unauthorized Gate Decision")
    @Description("Verify that only authorized users can make gate decisions")
    @DisplayName("Should reject unauthorized gate decisions")
    void shouldRejectUnauthorizedGateDecisions() {
        logTestStep("Unauthorized Gate Decision", "Verify unauthorized gate decision rejection");

        // Create an orphaned service
        String orphanedServiceName = TestDataGenerator.generateOrphanedServiceName();
        Map<String, Object> createOrphanedRequest = Map.of(
                "displayName", orphanedServiceName,
                "description", "E2E test orphaned service for unauthorized gate decision",
                "lifecycle", "ACTIVE",
                "tags", TestDataGenerator.generateStringList(2, "unauthorized-gate-test")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createOrphanedRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String serviceId = serviceResponse.jsonPath().getString("id");

        // User2 creates approval request
        Map<String, Object> createApprovalRequest = Map.of(
                "serviceId", serviceId,
                "targetTeamId", TestUsers.TEAM1
        );

        Response approvalResponse = ApiClient.given(getUser2Token())
                .body(createApprovalRequest)
                .when()
                .post("/approval-requests")
                .then()
                .statusCode(201)
                .extract().response();

        String requestId = approvalResponse.jsonPath().getString("id");

        // User3 (team2) tries to approve SYS_ADMIN gate (should fail)
        Map<String, Object> unauthorizedSysAdminRequest = Map.of(
                "decision", "APPROVE",
                "gate", "SYS_ADMIN",
                "note", "Unauthorized SYS_ADMIN gate decision"
        );

        ApiClient.given(getUser3Token())
                .body(unauthorizedSysAdminRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(403);

        // User3 (team2) tries to approve LINE_MANAGER gate (should fail)
        Map<String, Object> unauthorizedLineManagerRequest = Map.of(
                "decision", "APPROVE",
                "gate", "LINE_MANAGER",
                "note", "Unauthorized LINE_MANAGER gate decision"
        );

        ApiClient.given(getUser3Token())
                .body(unauthorizedLineManagerRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(403);

        // User4 (team2, has manager user3) tries to approve LINE_MANAGER gate (should fail)
        ApiClient.given(getUser4Token())
                .body(unauthorizedLineManagerRequest)
                .when()
                .post("/approval-requests/" + requestId + "/decisions")
                .then()
                .statusCode(403);

        // Verify request is still pending
        ApiClient.given(getAdminToken())
                .when()
                .get("/approval-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING"));

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + serviceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Unauthorized Gate Decision", "Unauthorized gate decisions properly rejected");
    }
}
