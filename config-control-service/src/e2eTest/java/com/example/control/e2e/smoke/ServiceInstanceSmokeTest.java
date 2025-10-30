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

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Service Instance smoke tests for E2E testing.
 * <p>
 * Tests critical ServiceInstance functionality including:
 * - Instance creation and listing
 * - Instance visibility filtering
 * - Instance status updates
 * - Basic CRUD operations
 * </p>
 */
@Slf4j
@Epic("Service Instances")
@Feature("Instance Management")
@DisplayName("Service Instance Smoke Tests")
public class ServiceInstanceSmokeTest extends BaseE2ETest {

    private String createdInstanceId;

    @Test
    @Story("Instance Listing")
    @Description("Verify that users can list service instances with proper visibility filtering")
    @DisplayName("Should list service instances with visibility filtering")
    void shouldListServiceInstancesWithVisibilityFiltering() {
        logTestStep("Instance Listing", "Verify service instance listing with visibility filtering");

        // Test admin can see all instances
        Response adminResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/service-instances")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("metadata.totalElements", greaterThanOrEqualTo(0))
                .extract().response();

        int adminInstanceCount = adminResponse.jsonPath().getInt("metadata.totalElements");
        logTestData("Admin Instance Count", String.valueOf(adminInstanceCount));

        // Test user1 can see instances (filtered by visibility)
        Response user1Response = ApiClient.given(getUser1Token())
                .when()
                .get("/service-instances")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("metadata.totalElements", greaterThanOrEqualTo(0))
                .extract().response();

        int user1InstanceCount = user1Response.jsonPath().getInt("metadata.totalElements");
        logTestData("User1 Instance Count", String.valueOf(user1InstanceCount));

        // Admin should see more or equal instances than regular users
        assertTrue(adminInstanceCount >= user1InstanceCount,
                "Admin should see more or equal instances than regular users");

        logTestResult("Instance Listing", "Instance listing with visibility filtering working correctly");
    }

    @Test
    @Story("Instance Creation")
    @Description("Verify that users can create service instances for services they have access to")
    @DisplayName("Should create service instance for accessible service")
    void shouldCreateServiceInstanceForAccessibleService() {
        logTestStep("Instance Creation", "Verify service instance creation for accessible services");

        // First create a service owned by team1
        String serviceId = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        String serviceName = serviceId;
        Map<String, Object> createServiceRequest = Map.of(
                "id", serviceId,
                "displayName", serviceName,
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "instance-test"),
                "environments", List.of("dev", "staging", "prod")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String instanceTestServiceId = serviceResponse.jsonPath().getString("id");
        logTestData("Created Service for Instance Test", instanceTestServiceId);

        // Now create an instance for this service
        String instanceId = TestDataGenerator.generateInstanceId();
        Map<String, Object> createInstanceRequest = Map.of(
                "instanceId", instanceId,
                "serviceId", instanceTestServiceId,
                "host", TestDataGenerator.generateTestHost(),
                "port", TestDataGenerator.generateTestPort(),
                "environment", "dev",
                "version", TestDataGenerator.generateTestVersion()
        );

        Response instanceResponse = ApiClient.given(getUser1Token())
                .body(createInstanceRequest)
                .when()
                .post("/service-instances")
                .then()
                .statusCode(201)
                .body("instanceId", equalTo(instanceId))
                .body("serviceName", equalTo(serviceName))
                // .body("serviceId", equalTo(instanceTestServiceId))
                .extract().response();

        createdInstanceId = instanceResponse.jsonPath().getString("instanceId");
        logTestData("Created Instance ID", createdInstanceId);

        // Verify instance can be retrieved
        ApiClient.given(getUser1Token())
                .when()
                .get("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(200)
                .body("instanceId", equalTo(createdInstanceId))
                .body("serviceName", equalTo(serviceName));

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + instanceTestServiceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Instance Creation", "Service instance created successfully");
    }

    @Test
    @Story("Instance Visibility")
    @Description("Verify that users can only see instances for services they have access to")
    @DisplayName("Should enforce instance visibility rules")
    void shouldEnforceInstanceVisibilityRules() {
        logTestStep("Instance Visibility", "Verify instance visibility rules for different users");

        // Create a service owned by team1
        String serviceIdInput = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        String serviceName = serviceIdInput;
        Map<String, Object> createServiceRequest = Map.of(
                "id", serviceIdInput,
                "displayName", serviceName,
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "visibility-test"),
                "environments", List.of("dev", "staging", "prod")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String visibilityTestServiceId = serviceResponse.jsonPath().getString("id");

        // Create an instance for this service
        String instanceId = TestDataGenerator.generateInstanceId();
        Map<String, Object> createInstanceRequest = Map.of(
                "instanceId", instanceId,
                "serviceId", visibilityTestServiceId,
                "host", TestDataGenerator.generateTestHost(),
                "port", TestDataGenerator.generateTestPort(),
                "environment", "dev",
                "version", TestDataGenerator.generateTestVersion()
        );

        Response instanceResponse = ApiClient.given(getUser1Token())
                .body(createInstanceRequest)
                .when()
                .post("/service-instances")
                .then()
                .statusCode(201)
                .extract().response();

        String createdInstanceId = instanceResponse.jsonPath().getString("instanceId");
        logTestData("Created Instance for Visibility Test", createdInstanceId);

        // User1 (team1) should be able to see the instance
        Response user1Response = ApiClient.given(getUser1Token())
                .when()
                .get("/service-instances")
                .then()
                .statusCode(200)
                .extract().response();

        boolean user1CanSeeInstance = user1Response.jsonPath().getList("items.instanceId").contains(createdInstanceId);
        assertTrue(user1CanSeeInstance, "User1 (team1) should be able to see team1 instance");

        // User2 (team1) should also be able to see the instance
        Response user2Response = ApiClient.given(getUser2Token())
                .when()
                .get("/service-instances")
                .then()
                .statusCode(200)
                .extract().response();

        boolean user2CanSeeInstance = user2Response.jsonPath().getList("items.instanceId").contains(createdInstanceId);
        assertTrue(user2CanSeeInstance, "User2 (team1) should be able to see team1 instance");

        // User3 (team2) should not be able to see the instance
        Response user3Response = ApiClient.given(getUser3Token())
                .when()
                .get("/service-instances")
                .then()
                .statusCode(200)
                .extract().response();

        boolean user3CanSeeInstance = user3Response.jsonPath().getList("items.instanceId").contains(createdInstanceId);
        assertFalse(user3CanSeeInstance, "User3 (team2) should not be able to see team1 instance");

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + visibilityTestServiceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Instance Visibility", "Instance visibility rules enforced correctly");
    }

    @Test
    @Story("Instance Retrieval")
    @Description("Verify that users can retrieve specific instances they have access to")
    @DisplayName("Should retrieve accessible instances by ID")
    void shouldRetrieveAccessibleInstancesById() {
        logTestStep("Instance Retrieval", "Verify instance retrieval by ID for accessible instances");

        // Create a service owned by team1
        String serviceIdInput = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        String serviceName = serviceIdInput;
        Map<String, Object> createServiceRequest = Map.of(
                "id", serviceIdInput,
                "displayName", serviceName,
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "retrieval-test"),
                "environments", List.of("dev", "staging", "prod")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String retrievalTestServiceId = serviceResponse.jsonPath().getString("id");

        // Create an instance for this service
        String instanceId = TestDataGenerator.generateInstanceId();
        Map<String, Object> createInstanceRequest = Map.of(
                "instanceId", instanceId,
                "serviceId", retrievalTestServiceId,
                "host", TestDataGenerator.generateTestHost(),
                "port", TestDataGenerator.generateTestPort(),
                "environment", "dev",
                "version", TestDataGenerator.generateTestVersion()
        );

        Response instanceResponse = ApiClient.given(getUser1Token())
                .body(createInstanceRequest)
                .when()
                .post("/service-instances")
                .then()
                .statusCode(201)
                .extract().response();

        String createdInstanceId = instanceResponse.jsonPath().getString("instanceId");

        // User1 (team1) should be able to retrieve the instance
        ApiClient.given(getUser1Token())
                .when()
                .get("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(200)
                .body("instanceId", equalTo(createdInstanceId))
                .body("serviceName", equalTo(serviceName));

        // User2 (team1) should also be able to retrieve the instance
        ApiClient.given(getUser2Token())
                .when()
                .get("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(200)
                .body("instanceId", equalTo(createdInstanceId));

        // User3 (team2) should not be able to retrieve the instance
        ApiClient.given(getUser3Token())
                .when()
                .get("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(404);

        // Admin should be able to retrieve any instance
        ApiClient.given(getAdminToken())
                .when()
                .get("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(200)
                .body("instanceId", equalTo(createdInstanceId));

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + retrievalTestServiceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Instance Retrieval", "Instance retrieval by ID working correctly");
    }

    @Test
    @Story("Instance Status Update")
    @Description("Verify that users can update instance status and drift information")
    @DisplayName("Should update instance status and drift information")
    void shouldUpdateInstanceStatusAndDriftInformation() {
        logTestStep("Instance Status Update", "Verify instance status and drift information updates");

        // Create a service owned by team1
        String serviceIdInput = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        String serviceName = serviceIdInput;
        Map<String, Object> createServiceRequest = Map.of(
                "id", serviceIdInput,
                "displayName", serviceName,
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "status-test"),
                "environments", List.of("dev", "staging", "prod")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String statusTestServiceId = serviceResponse.jsonPath().getString("id");

        // Create an instance for this service
        String instanceId = TestDataGenerator.generateInstanceId();
        Map<String, Object> createInstanceRequest = Map.of(
                "instanceId", instanceId,
                "serviceId", statusTestServiceId,
                "host", TestDataGenerator.generateTestHost(),
                "port", TestDataGenerator.generateTestPort(),
                "environment", "dev",
                "version", TestDataGenerator.generateTestVersion()
        );
        
        Response instanceResponse = ApiClient.given(getUser1Token())
                .body(createInstanceRequest)
                .when()
                .post("/service-instances")
                .then()
                .statusCode(201)
                .extract().response();

        String createdInstanceId = instanceResponse.jsonPath().getString("instanceId");
        Map<String, Object> updateRequest = Map.of(
                "status", "DRIFT",
                "hasDrift", true,
                "expectedHash", TestDataGenerator.generateTestHash(),
                "lastAppliedHash", TestDataGenerator.generateTestHash()
        );

        ApiClient.given(getUser1Token())
                .body(updateRequest)
                .when()
                .put("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(200)
                .body("instanceId", equalTo(createdInstanceId))
                .body("status", equalTo("DRIFT"))
                .body("hasDrift", equalTo(true));

        // Verify the update
        ApiClient.given(getUser1Token())
                .when()
                .get("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(200)
                .body("status", equalTo("DRIFT"))
                .body("hasDrift", equalTo(true));

        // Clean up
        ApiClient.given(getAdminToken())
                .when()
                .delete("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + statusTestServiceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Instance Status Update", "Instance status and drift information updated successfully");
    }

    @Test
    @Story("Instance Deletion")
    @Description("Verify that users can delete instances they have access to")
    @DisplayName("Should delete accessible instances")
    void shouldDeleteAccessibleInstances() {
        logTestStep("Instance Deletion", "Verify instance deletion for accessible instances");

        // Create a service owned by team1
        String serviceIdInput = TestDataGenerator.generateServiceNameForTeam(TestUsers.TEAM1);
        String serviceName = serviceIdInput;
        Map<String, Object> createServiceRequest = Map.of(
                "id", serviceIdInput,
                "displayName", serviceName,
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "deletion-test"),
                "environments", List.of("dev", "staging", "prod")
        );

        Response serviceResponse = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        String deletionTestServiceId = serviceResponse.jsonPath().getString("id");

        // Create an instance for this service
        String instanceId = TestDataGenerator.generateInstanceId();
        Map<String, Object> createInstanceRequest = Map.of(
                "instanceId", instanceId,
                "serviceId", deletionTestServiceId,
                "host", TestDataGenerator.generateTestHost(),
                "port", TestDataGenerator.generateTestPort(),
                "environment", "dev",
                "version", TestDataGenerator.generateTestVersion()
        );
        
        Response instanceResponse = ApiClient.given(getUser1Token())
                .body(createInstanceRequest)
                .when()
                .post("/service-instances")
                .then()
                .statusCode(201)
                .extract().response();

        String createdInstanceId = instanceResponse.jsonPath().getString("instanceId");
        ApiClient.given(getUser1Token())
                .when()
                .delete("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(204);

        // Verify instance is deleted
        ApiClient.given(getUser1Token())
                .when()
                .get("/service-instances/" + createdInstanceId)
                .then()
                .statusCode(404);

        // Clean up service
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + deletionTestServiceId)
                .then()
                .statusCode(anyOf(is(204), is(404)));

        logTestResult("Instance Deletion", "Instance deleted successfully by team member");
    }
}
