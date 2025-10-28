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
 * Drift Detection workflow tests for E2E testing.
 * <p>
 * Tests complete drift detection workflow including:
 * - Instance registration with config hash
 * - Config mismatch detection
 * - Drift event creation
 * - Drift resolution tracking
 * </p>
 */
@Slf4j
@Epic("Drift Detection")
@Feature("Configuration Drift Workflow")
@DisplayName("Drift Detection Workflow Tests")
public class DriftDetectionWorkflowTest extends BaseE2ETest {

        @Test
        @Story("Complete Drift Detection Workflow")
        @Description("Verify complete drift detection workflow from instance registration to drift resolution")
        @DisplayName("Should complete full drift detection workflow")
        void shouldCompleteFullDriftDetectionWorkflow() {
                logTestStep("Drift Detection Workflow", "Execute complete drift detection workflow");

                // Step 1: Create a service owned by team1
                String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
                Map<String, Object> createServiceRequest = Map.of(
                                "displayName", serviceName,
                                "description", "E2E test service for drift detection workflow",
                                "lifecycle", "ACTIVE",
                                "ownerTeamId", TestUsers.TEAM1,
                                "tags", TestDataGenerator.generateStringList(3, "drift-workflow"));

                Response serviceResponse = ApiClient.given(getAdminToken())
                                .body(createServiceRequest)
                                .when()
                                .post("/application-services")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String serviceId = serviceResponse.jsonPath().getString("id");
                logTestData("Created Service for Drift Test", serviceId);

                // Step 2: Create an instance with initial config hash
                String instanceId = TestDataGenerator.generateInstanceId();
                String initialHash = TestDataGenerator.generateTestHash();
                Map<String, Object> createInstanceRequest = Map.of(
                                "serviceName", serviceName,
                                "instanceId", instanceId,
                                "serviceId", serviceId,
                                "host", TestDataGenerator.generateTestHost(),
                                "port", TestDataGenerator.generateTestPort(),
                                "environment", "dev",
                                "version", TestDataGenerator.generateTestVersion(),
                                "configHash", initialHash,
                                "status", "HEALTHY");

                Response instanceResponse = ApiClient.given(getUser1Token())
                                .body(createInstanceRequest)
                                .when()
                                .post("/service-instances")
                                .then()
                                .statusCode(201)
                                .body("instanceId", equalTo(instanceId))
                                .body("configHash", equalTo(initialHash))
                                .body("status", equalTo("HEALTHY"))
                                .extract().response();

                logTestData("Created Instance", instanceId);

                // Step 3: Update instance with different config hash (simulating drift)
                String driftedHash = TestDataGenerator.generateTestHash();
                Map<String, Object> updateInstanceRequest = Map.of(
                                "configHash", driftedHash,
                                "status", "DRIFT",
                                "hasDrift", true,
                                "expectedHash", initialHash,
                                "lastAppliedHash", driftedHash);

                ApiClient.given(getUser1Token())
                                .body(updateInstanceRequest)
                                .when()
                                .put("/service-instances/" + instanceId)
                                .then()
                                .statusCode(200)
                                .body("configHash", equalTo(driftedHash))
                                .body("status", equalTo("DRIFT"))
                                .body("hasDrift", equalTo(true));

                // Step 4: Verify drift event is created
                Response driftEventsResponse = ApiClient.given(getUser1Token())
                                .when()
                                .get("/drift-events")
                                .then()
                                .statusCode(200)
                                .body("items", notNullValue())
                                .extract().response();

                // Find drift event for this instance
                boolean driftEventExists = driftEventsResponse.jsonPath().getList("items.instanceId")
                                .contains(instanceId);
                assertTrue(driftEventExists, "Drift event should be created for the instance");

                // Step 5: Get drift event details
                String driftEventId = driftEventsResponse.jsonPath().getList("items")
                                .stream()
                                .filter(item -> ((Map<String, Object>) item).get("instanceId").equals(instanceId))
                                .map(item -> ((Map<String, Object>) item).get("id"))
                                .findFirst()
                                .map(Object::toString)
                                .orElse(null);

                assertNotNull(driftEventId, "Drift event ID should not be null");

                ApiClient.given(getUser1Token())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("instanceId", equalTo(instanceId))
                                .body("serviceId", equalTo(serviceId))
                                .body("status", equalTo("DETECTED"));

                // Step 6: Update drift event status to resolved
                Map<String, Object> resolveDriftRequest = Map.of(
                                "status", "RESOLVED",
                                "resolutionNote", "Drift resolved by applying correct configuration");

                ApiClient.given(getUser1Token())
                                .body(resolveDriftRequest)
                                .when()
                                .put("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("RESOLVED"));

                // Step 7: Update instance status back to healthy
                Map<String, Object> fixInstanceRequest = Map.of(
                                "configHash", initialHash,
                                "status", "HEALTHY",
                                "hasDrift", false,
                                "expectedHash", initialHash,
                                "lastAppliedHash", initialHash);

                ApiClient.given(getUser1Token())
                                .body(fixInstanceRequest)
                                .when()
                                .put("/service-instances/" + instanceId)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("HEALTHY"))
                                .body("hasDrift", equalTo(false));

                // Step 8: Verify drift event is updated
                ApiClient.given(getUser1Token())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("RESOLVED"));

                // Step 9: Verify team2 cannot see drift events for team1 service
                ApiClient.given(getUser3Token())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(404);

                // Step 10: Verify admin can see all drift events
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("instanceId", equalTo(instanceId));

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

                logTestResult("Drift Detection Workflow", "Complete drift detection workflow executed successfully");
        }

        @Test
        @Story("Drift Event Creation")
        @Description("Verify that drift events are properly created when config drift is detected")
        @DisplayName("Should create drift events when config drift is detected")
        void shouldCreateDriftEventsWhenConfigDriftIsDetected() {
                logTestStep("Drift Event Creation", "Verify drift event creation for config drift");

                // Create a service owned by team1
                String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
                Map<String, Object> createServiceRequest = Map.of(
                                "displayName", serviceName,
                                "description", "E2E test service for drift event creation",
                                "lifecycle", "ACTIVE",
                                "ownerTeamId", TestUsers.TEAM1,
                                "tags", TestDataGenerator.generateStringList(2, "drift-event-test"));

                Response serviceResponse = ApiClient.given(getAdminToken())
                                .body(createServiceRequest)
                                .when()
                                .post("/application-services")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String serviceId = serviceResponse.jsonPath().getString("id");

                // Create an instance
                String instanceId = TestDataGenerator.generateInstanceId();
                String initialHash = TestDataGenerator.generateTestHash();
                Map<String, Object> createInstanceRequest = Map.of(
                                "serviceName", serviceName,
                                "instanceId", instanceId,
                                "serviceId", serviceId,
                                "host", TestDataGenerator.generateTestHost(),
                                "port", TestDataGenerator.generateTestPort(),
                                "environment", "dev",
                                "version", TestDataGenerator.generateTestVersion(),
                                "configHash", initialHash,
                                "status", "HEALTHY");

                ApiClient.given(getUser1Token())
                                .body(createInstanceRequest)
                                .when()
                                .post("/service-instances")
                                .then()
                                .statusCode(201);

                // Create drift event manually
                String driftEventId = TestDataGenerator.generateDriftEventId();
                Map<String, Object> createDriftEventRequest = Map.of(
                                "instanceId", instanceId,
                                "serviceId", serviceId,
                                "serviceName", serviceName,
                                "expectedHash", initialHash,
                                "actualHash", TestDataGenerator.generateTestHash(),
                                "status", "DETECTED",
                                "detectedAt", TestDataGenerator.generateTimestamp(),
                                "description", "Config drift detected in E2E test");

                Response driftEventResponse = ApiClient.given(getUser1Token())
                                .body(createDriftEventRequest)
                                .when()
                                .post("/drift-events")
                                .then()
                                .statusCode(201)
                                .body("id", notNullValue())
                                .body("instanceId", equalTo(instanceId))
                                .body("serviceId", equalTo(serviceId))
                                .body("status", equalTo("DETECTED"))
                                .extract().response();

                String createdDriftEventId = driftEventResponse.jsonPath().getString("id");

                // Verify drift event can be retrieved
                ApiClient.given(getUser1Token())
                                .when()
                                .get("/drift-events/" + createdDriftEventId)
                                .then()
                                .statusCode(200)
                                .body("instanceId", equalTo(instanceId))
                                .body("status", equalTo("DETECTED"));

                // Verify drift event appears in list
                Response driftEventsResponse = ApiClient.given(getUser1Token())
                                .when()
                                .get("/drift-events")
                                .then()
                                .statusCode(200)
                                .extract().response();

                boolean driftEventInList = driftEventsResponse.jsonPath().getList("items.id")
                                .contains(createdDriftEventId);
                assertTrue(driftEventInList, "Drift event should appear in the drift events list");

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

                logTestResult("Drift Event Creation",
                                "Drift events created successfully when config drift is detected");
        }

        @Test
        @Story("Drift Event Status Updates")
        @Description("Verify that drift event status can be updated correctly")
        @DisplayName("Should update drift event status correctly")
        void shouldUpdateDriftEventStatusCorrectly() {
                logTestStep("Drift Event Status Updates", "Verify drift event status updates");

                // Create a service owned by team1
                String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
                Map<String, Object> createServiceRequest = Map.of(
                                "displayName", serviceName,
                                "description", "E2E test service for drift status updates",
                                "lifecycle", "ACTIVE",
                                "ownerTeamId", TestUsers.TEAM1,
                                "tags", TestDataGenerator.generateStringList(2, "status-update-test"));

                Response serviceResponse = ApiClient.given(getAdminToken())
                                .body(createServiceRequest)
                                .when()
                                .post("/application-services")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String serviceId = serviceResponse.jsonPath().getString("id");

                // Create an instance
                String instanceId = TestDataGenerator.generateInstanceId();
                Map<String, Object> createInstanceRequest = Map.of(
                                "serviceName", serviceName,
                                "instanceId", instanceId,
                                "serviceId", serviceId,
                                "host", TestDataGenerator.generateTestHost(),
                                "port", TestDataGenerator.generateTestPort(),
                                "environment", "dev",
                                "version", TestDataGenerator.generateTestVersion());

                ApiClient.given(getUser1Token())
                                .body(createInstanceRequest)
                                .when()
                                .post("/service-instances")
                                .then()
                                .statusCode(201);

                // Create drift event
                Map<String, Object> createDriftEventRequest = Map.of(
                                "instanceId", instanceId,
                                "serviceId", serviceId,
                                "serviceName", serviceName,
                                "expectedHash", TestDataGenerator.generateTestHash(),
                                "actualHash", TestDataGenerator.generateTestHash(),
                                "status", "DETECTED",
                                "detectedAt", TestDataGenerator.generateTimestamp(),
                                "description", "Drift event for status update testing");

                Response driftEventResponse = ApiClient.given(getUser1Token())
                                .body(createDriftEventRequest)
                                .when()
                                .post("/drift-events")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String driftEventId = driftEventResponse.jsonPath().getString("id");

                // Update status to IN_PROGRESS
                Map<String, Object> updateToInProgress = Map.of(
                                "status", "IN_PROGRESS",
                                "resolutionNote", "Working on resolving the drift");

                ApiClient.given(getUser1Token())
                                .body(updateToInProgress)
                                .when()
                                .put("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("IN_PROGRESS"));

                // Update status to RESOLVED
                Map<String, Object> updateToResolved = Map.of(
                                "status", "RESOLVED",
                                "resolutionNote", "Drift has been resolved");

                ApiClient.given(getUser1Token())
                                .body(updateToResolved)
                                .when()
                                .put("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("RESOLVED"));

                // Verify final status
                ApiClient.given(getUser1Token())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("RESOLVED"))
                                .body("resolutionNote", equalTo("Drift has been resolved"));

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

                logTestResult("Drift Event Status Updates", "Drift event status updated correctly");
        }

        @Test
        @Story("Drift Event Visibility")
        @Description("Verify that drift events are properly filtered by team visibility")
        @DisplayName("Should enforce drift event visibility rules")
        void shouldEnforceDriftEventVisibilityRules() {
                logTestStep("Drift Event Visibility", "Verify drift event visibility rules");

                // Create a service owned by team1
                String serviceName = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
                Map<String, Object> createServiceRequest = Map.of(
                                "displayName", serviceName,
                                "description", "E2E test service for drift visibility",
                                "lifecycle", "ACTIVE",
                                "ownerTeamId", TestUsers.TEAM1,
                                "tags", TestDataGenerator.generateStringList(2, "visibility-test"));

                Response serviceResponse = ApiClient.given(getAdminToken())
                                .body(createServiceRequest)
                                .when()
                                .post("/application-services")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String serviceId = serviceResponse.jsonPath().getString("id");

                // Create an instance
                String instanceId = TestDataGenerator.generateInstanceId();
                Map<String, Object> createInstanceRequest = Map.of(
                                "serviceName", serviceName,
                                "instanceId", instanceId,
                                "serviceId", serviceId,
                                "host", TestDataGenerator.generateTestHost(),
                                "port", TestDataGenerator.generateTestPort(),
                                "environment", "dev",
                                "version", TestDataGenerator.generateTestVersion());

                ApiClient.given(getUser1Token())
                                .body(createInstanceRequest)
                                .when()
                                .post("/service-instances")
                                .then()
                                .statusCode(201);

                // Create drift event
                Map<String, Object> createDriftEventRequest = Map.of(
                                "instanceId", instanceId,
                                "serviceId", serviceId,
                                "serviceName", serviceName,
                                "expectedHash", TestDataGenerator.generateTestHash(),
                                "actualHash", TestDataGenerator.generateTestHash(),
                                "status", "DETECTED",
                                "detectedAt", TestDataGenerator.generateTimestamp(),
                                "description", "Drift event for visibility testing");

                Response driftEventResponse = ApiClient.given(getUser1Token())
                                .body(createDriftEventRequest)
                                .when()
                                .post("/drift-events")
                                .then()
                                .statusCode(201)
                                .extract().response();

                String driftEventId = driftEventResponse.jsonPath().getString("id");

                // Team1 members should be able to see the drift event
                ApiClient.given(getUser1Token())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("instanceId", equalTo(instanceId));

                ApiClient.given(getUser2Token())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("instanceId", equalTo(instanceId));

                // Team2 members should not be able to see the drift event
                ApiClient.given(getUser3Token())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(404);

                ApiClient.given(getUser4Token())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(404);

                // Admin should be able to see all drift events
                ApiClient.given(getAdminToken())
                                .when()
                                .get("/drift-events/" + driftEventId)
                                .then()
                                .statusCode(200)
                                .body("instanceId", equalTo(instanceId));

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

                logTestResult("Drift Event Visibility", "Drift event visibility rules enforced correctly");
        }
}
