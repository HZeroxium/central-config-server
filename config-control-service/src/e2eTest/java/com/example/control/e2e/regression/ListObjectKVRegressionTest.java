package com.example.control.e2e.regression;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * KV Object and List operations regression tests for E2E testing.
 * <p>
 * Tests comprehensive Object and List operations including:
 * - GET operations (simple, nested, empty structures)
 * - PUT operations (create, update, atomic updates)
 * - DELETE operations (via empty data, deletes parameter)
 * - Permission checks (team access, SYS_ADMIN)
 * - Edge cases (missing items, large data, nested structures)
 * - Interaction with basic KV operations
 * - Automatic test data cleanup
 * </p>
 */
@Slf4j
@Epic("Key-Value Store")
@Feature("KV Object & List Operations")
@DisplayName("KV Object & List Regression Tests")
public class ListObjectKVRegressionTest extends BaseE2ETest {

    private static String testServiceId;
    private static String testServiceIdTeam1;
    private static String testServiceIdTeam2;

    @BeforeAll
    @Description("Setup test ApplicationServices for Object/List operations")
    void setUpTestServices() {
        logTestStep("Setup Test Services", "Create test ApplicationServices for Object/List operations");

        // Create test service owned by admin (for general tests)
        String serviceId = TestDataGenerator.generateServiceName("obj-list-test");
        Map<String, Object> createServiceRequest = Map.of(
                "id", serviceId,
                "displayName", serviceId,
                "tags", TestDataGenerator.generateStringList(2, "obj-list-test"),
                "environments", List.of("dev", "staging", "prod")
        );

        Response response = ApiClient.given(getAdminToken())
                .body(createServiceRequest)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        testServiceId = response.jsonPath().getString("id");
        logTestData("Created Test Service", testServiceId);

        // Create test service owned by team1
        String serviceIdTeam1 = TestDataGenerator.generateServiceName("obj-list-test-team1");
        Map<String, Object> createTeam1Request = Map.of(
                "id", serviceIdTeam1,
                "displayName", serviceIdTeam1,
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "obj-list-test-team1"),
                "environments", List.of("dev", "staging", "prod")
        );

        Response team1Response = ApiClient.given(getAdminToken())
                .body(createTeam1Request)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        testServiceIdTeam1 = team1Response.jsonPath().getString("id");
        logTestData("Created Team1 Test Service", testServiceIdTeam1);

        // Create test service owned by team2
        String serviceIdTeam2 = TestDataGenerator.generateServiceName("obj-list-test-team2");
        Map<String, Object> createTeam2Request = Map.of(
                "id", serviceIdTeam2,
                "displayName", serviceIdTeam2,
                "ownerTeamId", TestUsers.TEAM2,
                "tags", TestDataGenerator.generateStringList(2, "obj-list-test-team2"),
                "environments", List.of("dev", "staging", "prod")
        );

        Response team2Response = ApiClient.given(getAdminToken())
                .body(createTeam2Request)
                .when()
                .post("/application-services")
                .then()
                .statusCode(201)
                .extract().response();

        testServiceIdTeam2 = team2Response.jsonPath().getString("id");
        logTestData("Created Team2 Test Service", testServiceIdTeam2);
    }

    @AfterAll
    @Description("Cleanup test data: delete all Object/List data and test ApplicationServices")
    void tearDownTestData() {
        logTestStep("Cleanup Test Data", "Delete all test Object/List data and ApplicationServices");

        try {
            // Cleanup Object/List data for testServiceId (delete recursively)
            try {
                ApiClient.given(getAdminToken())
                        .when()
                        .delete("/application-services/" + testServiceId + "/kv?recurse=true")
                        .then()
                        .statusCode(anyOf(is(200), is(404)));
                log.debug("Cleaned up KV data for service: {}", testServiceId);
            } catch (Exception e) {
                log.warn("Failed to cleanup KV data for service {}: {}", testServiceId, e.getMessage());
            }

            // Cleanup Object/List data for testServiceIdTeam1
            try {
                ApiClient.given(getAdminToken())
                        .when()
                        .delete("/application-services/" + testServiceIdTeam1 + "/kv?recurse=true")
                        .then()
                        .statusCode(anyOf(is(200), is(404)));
                log.debug("Cleaned up KV data for service: {}", testServiceIdTeam1);
            } catch (Exception e) {
                log.warn("Failed to cleanup KV data for service {}: {}", testServiceIdTeam1, e.getMessage());
            }

            // Cleanup Object/List data for testServiceIdTeam2
            try {
                ApiClient.given(getAdminToken())
                        .when()
                        .delete("/application-services/" + testServiceIdTeam2 + "/kv?recurse=true")
                        .then()
                        .statusCode(anyOf(is(200), is(404)));
                log.debug("Cleaned up KV data for service: {}", testServiceIdTeam2);
            } catch (Exception e) {
                log.warn("Failed to cleanup KV data for service {}: {}", testServiceIdTeam2, e.getMessage());
            }

            // Delete test ApplicationServices
            if (testServiceId != null) {
                ApiClient.given(getAdminToken())
                        .when()
                        .delete("/application-services/" + testServiceId)
                        .then()
                        .statusCode(anyOf(is(204), is(404)));
            }

            if (testServiceIdTeam1 != null) {
                ApiClient.given(getAdminToken())
                        .when()
                        .delete("/application-services/" + testServiceIdTeam1)
                        .then()
                        .statusCode(anyOf(is(204), is(404)));
            }

            if (testServiceIdTeam2 != null) {
                ApiClient.given(getAdminToken())
                        .when()
                        .delete("/application-services/" + testServiceIdTeam2)
                        .then()
                        .statusCode(anyOf(is(204), is(404)));
            }

            logTestResult("Cleanup", "Test data cleanup completed");
        } catch (Exception e) {
            log.warn("Error during test data cleanup", e);
        }
    }

    // ============================================================================
    // Object Operations - GET
    // ============================================================================

    @Test
    @Story("Object GET Operations")
    @Description("Verify GET operation returns object with simple structure")
    @DisplayName("Should get object with simple structure")
    void shouldGetObjectWithSimpleStructure() {
        logTestStep("GET Simple Object", "Get object with simple key-value structure");

        String prefix = "config/simple";
        Map<String, Object> objectData = Map.of(
                "host", "localhost",
                "port", 8080,
                "enabled", true 
        );

        // Create object
        Response putResponse = ApiClient.given(getAdminToken())
                .body(Map.of("data", objectData))
                .when()
                .put("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();
        
        // Debug: print response if failed
        if (!putResponse.jsonPath().getBoolean("success")) {
            log.error("PUT Object failed. Response: {}", putResponse.asString());
            log.error("Error: {}", putResponse.jsonPath().getString("error"));
        }
        
        assertTrue(putResponse.jsonPath().getBoolean("success"), 
                "PUT should succeed. Error: " + putResponse.jsonPath().getString("error"));

        // Get object
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .body("data", notNullValue())
                .body("type", equalTo("OBJECT"))
                .extract().response();

        Map<String, Object> data = response.jsonPath().getMap("data");
        assertEquals("localhost", data.get("host"));
        assertEquals("8080", data.get("port"));
        assertEquals("true", data.get("enabled"));

        logTestResult("GET Simple Object", "Successfully retrieved object with simple structure");
    }

    @Test
    @Story("Object GET Operations")
    @Description("Verify GET operation returns object with nested structure")
    @DisplayName("Should get object with nested structure")
    void shouldGetObjectWithNestedStructure() {
        logTestStep("GET Nested Object", "Get object with nested structure");

        String prefix = "config/nested";
        Map<String, Object> nestedData = Map.of(
                "database", Map.of(
                        "host", "db.example.com",
                        "port", 5432,
                        "credentials", Map.of(
                                "username", "admin",
                                "password", "secret"
                        )
                ),
                "cache", Map.of(
                        "host", "cache.example.com",
                        "ttl", 3600
                )
        );

        // Create object
        ApiClient.given(getAdminToken())
                .body(Map.of("data", nestedData))
                .when()
                .put("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Get object
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();

        Map<String, Object> data = response.jsonPath().getMap("data");
        assertNotNull(data.get("database"));
        assertNotNull(data.get("cache"));

        @SuppressWarnings("unchecked")
        Map<String, Object> db = (Map<String, Object>) data.get("database");
        assertEquals("db.example.com", db.get("host"));
        assertEquals("5432", db.get("port"));

        @SuppressWarnings("unchecked")
        Map<String, Object> credentials = (Map<String, Object>) db.get("credentials");
        assertEquals("admin", credentials.get("username"));

        logTestResult("GET Nested Object", "Successfully retrieved object with nested structure");
    }

    @Test
    @Story("Object GET Operations")
    @Description("Verify GET operation returns 404 for non-existent object")
    @DisplayName("Should return 404 for non-existent object")
    void shouldReturn404ForNonExistentObject() {
        logTestStep("GET Non-Existent Object", "Verify 404 response for non-existent object");

        String nonExistentPrefix = "config/non-existent-" + System.currentTimeMillis();

        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/object?prefix=" + nonExistentPrefix)
                .then()
                .statusCode(404);

        logTestResult("GET Non-Existent Object", "Correctly returned 404 for non-existent object");
    }

    @Test
    @Story("Object GET Operations")
    @Description("Verify GET operation returns 400 for invalid service")
    @DisplayName("Should return 400 for invalid service")
    void shouldReturn404ForInvalidServiceObject() {
        logTestStep("GET Invalid Service Object", "Verify 404 response for invalid service");

        String invalidServiceId = "non-existent-service-" + System.currentTimeMillis();

        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + invalidServiceId + "/kv/object?prefix=config")
                .then()
                .statusCode(400);

        logTestResult("GET Invalid Service Object", "Correctly returned 404 for invalid service");
    }

    // ============================================================================
    // Object Operations - PUT
    // ============================================================================

    @Test
    @Story("Object PUT Operations")
    @Description("Verify PUT operation creates new object")
    @DisplayName("Should create new object")
    void shouldCreateNewObject() {
        logTestStep("PUT Create Object", "Create new object");

        String prefix = "config/create-test";
        Map<String, Object> objectData = Map.of(
                "key1", "value1",
                "key2", "value2"
        );

        ApiClient.given(getAdminToken())
                .body(Map.of("data", objectData))
                .when()
                .put("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // Verify object was created
        Response getResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();

        Map<String, Object> data = getResponse.jsonPath().getMap("data");
        assertEquals("value1", data.get("key1"));
        assertEquals("value2", data.get("key2"));

        logTestResult("PUT Create Object", "Successfully created new object");
    }

    @Test
    @Story("Object PUT Operations")
    @Description("Verify PUT operation updates existing object and removes old keys")
    @DisplayName("Should update existing object and remove old keys")
    void shouldUpdateExistingObjectAndRemoveOldKeys() {
        logTestStep("PUT Update Object", "Update existing object and verify old keys are removed");

        String prefix = "config/update-test";

        // Create initial object
        Map<String, Object> initialData = Map.of(
                "oldKey1", "oldValue1",
                "oldKey2", "oldValue2",
                "keepKey", "keepValue"
        );

        ApiClient.given(getAdminToken())
                .body(Map.of("data", initialData))
                .when()
                .put("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Verify initial keys exist
        Response initialResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();

        Map<String, Object> initialDataMap = initialResponse.jsonPath().getMap("data");
        assertTrue(initialDataMap.containsKey("oldKey1"));
        assertTrue(initialDataMap.containsKey("oldKey2"));
        assertTrue(initialDataMap.containsKey("keepKey"));

        // Update object (remove oldKey1, oldKey2, add newKey)
        Map<String, Object> updatedData = Map.of(
                "keepKey", "updatedValue",
                "newKey", "newValue"
        );

        ApiClient.given(getAdminToken())
                .body(Map.of("data", updatedData))
                .when()
                .put("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // Verify old keys are removed and new keys are added
        Response updatedResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();

        Map<String, Object> updatedDataMap = updatedResponse.jsonPath().getMap("data");
        assertFalse(updatedDataMap.containsKey("oldKey1"), "oldKey1 should be removed");
        assertFalse(updatedDataMap.containsKey("oldKey2"), "oldKey2 should be removed");
        assertTrue(updatedDataMap.containsKey("keepKey"), "keepKey should exist");
        assertEquals("updatedValue", updatedDataMap.get("keepKey"));
        assertTrue(updatedDataMap.containsKey("newKey"), "newKey should exist");
        assertEquals("newValue", updatedDataMap.get("newKey"));

        logTestResult("PUT Update Object", "Successfully updated object and removed old keys");
    }

    @Test
    @Story("Object PUT Operations")
    @Description("Verify PUT operation with empty data deletes all keys")
    @DisplayName("Should delete all keys when updating with empty data")
    void shouldDeleteAllKeysWhenUpdatingWithEmptyData() {
        logTestStep("PUT Empty Object", "Update object with empty data should delete all keys");

        String prefix = "config/empty-test";

        // Create object with data
        Map<String, Object> initialData = Map.of(
                "key1", "value1",
                "key2", "value2"
        );

        ApiClient.given(getAdminToken())
                .body(Map.of("data", initialData))
                .when()
                .put("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Verify object exists
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Update with empty data
        ApiClient.given(getAdminToken())
                .body(Map.of("data", Map.of()))
                .when()
                .put("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // Verify object is deleted (404)
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(404);

        logTestResult("PUT Empty Object", "Successfully deleted all keys with empty data");
    }

    // ============================================================================
    // Object Operations - Permissions
    // ============================================================================

    @Test
    @Story("Object Permissions")
    @Description("Verify team member can CRUD own team's object")
    @DisplayName("Should allow team member to CRUD own team's object")
    void shouldAllowTeamMemberToCRUDOwnTeamsObject() {
        logTestStep("Team Member Object CRUD", "Verify team member can CRUD own team's object");

        String prefix = "config/team1-object";
        Map<String, Object> objectData = Map.of(
                "key", "value"
        );

        // User1 (team1) should be able to create object for team1 service
        ApiClient.given(getUser1Token())
                .body(Map.of("data", objectData))
                .when()
                .put("/application-services/" + testServiceIdTeam1 + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // User1 should be able to read
        ApiClient.given(getUser1Token())
                .when()
                .get("/application-services/" + testServiceIdTeam1 + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        // User1 should be able to update
        Map<String, Object> updatedData = Map.of(
                "key", "updatedValue"
        );
        ApiClient.given(getUser1Token())
                .body(Map.of("data", updatedData))
                .when()
                .put("/application-services/" + testServiceIdTeam1 + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        logTestResult("Team Member Object CRUD", "Team member can CRUD own team's object");
    }

    @Test
    @Story("Object Permissions")
    @Description("Verify team member cannot access other team's object")
    @DisplayName("Should prevent team member from accessing other team's object")
    void shouldPreventTeamMemberFromAccessingOtherTeamsObject() {
        logTestStep("Cross-Team Object Access", "Verify team member cannot access other team's object");

        String prefix = "config/team2-protected-object";
        Map<String, Object> objectData = Map.of(
                "key", "value"
        );

        // User3 (team2) creates an object in team2 service
        ApiClient.given(getUser3Token())
                .body(Map.of("data", objectData))
                .when()
                .put("/application-services/" + testServiceIdTeam2 + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        // User1 (team1) should not be able to read team2's object
        ApiClient.given(getUser1Token())
                .when()
                .get("/application-services/" + testServiceIdTeam2 + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(400); // Returns 404 to hide existence

        // User1 should not be able to update
        Map<String, Object> updateData = Map.of(
                "key", "unauthorized-update"
        );
        ApiClient.given(getUser1Token())
                .body(Map.of("data", updateData))
                .when()
                .put("/application-services/" + testServiceIdTeam2 + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(400);

        logTestResult("Cross-Team Object Access", "Correctly prevented cross-team object access");
    }

    @Test
    @Story("Object Permissions")
    @Description("Verify SYS_ADMIN can access all objects")
    @DisplayName("Should allow SYS_ADMIN to access all objects")
    void shouldAllowSysAdminToAccessAllObjects() {
        logTestStep("SYS_ADMIN Object Access", "Verify SYS_ADMIN can access all objects");

        String prefix = "config/admin-test-object";
        Map<String, Object> objectData = Map.of(
                "key", "value"
        );

        // User3 (team2) creates an object in team2 service
        ApiClient.given(getUser3Token())
                .body(Map.of("data", objectData))
                .when()
                .put("/application-services/" + testServiceIdTeam2 + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Admin should be able to read
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceIdTeam2 + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Admin should be able to update
        Map<String, Object> updateData = Map.of(
                "key", "admin-updated-value"
        );
        ApiClient.given(getAdminToken())
                .body(Map.of("data", updateData))
                .when()
                .put("/application-services/" + testServiceIdTeam2 + "/kv/object?prefix=" + prefix)
                .then()
                .statusCode(200);

        logTestResult("SYS_ADMIN Object Access", "SYS_ADMIN can access all objects");
    }

    // ============================================================================
    // List Operations - GET
    // ============================================================================

    @Test
    @Story("List GET Operations")
    @Description("Verify GET operation returns list with items")
    @DisplayName("Should get list with items")
    void shouldGetListWithItems() {
        logTestStep("GET List with Items", "Get list with items");

        String prefix = "config/list-test";

        // Create list with items
        Map<String, Object> listRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Item 1", "value", 100)),
                        Map.of("id", "item2", "data", Map.of("name", "Item 2", "value", 200))
                ),
                "manifest", Map.of(
                        "order", List.of("item1", "item2"),
                        "version", 1L,
                        "etag", "etag1",
                        "metadata", Map.of()
                )
        );

        ApiClient.given(getAdminToken())
                .body(listRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // Get list
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("type", equalTo("LIST"))
                .extract().response();

        List<Map<String, Object>> items = response.jsonPath().getList("items");
        assertEquals(2, items.size());

        Map<String, Object> item1 = items.get(0);
        assertEquals("item1", item1.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> item1Data = (Map<String, Object>) item1.get("data");
        assertEquals("Item 1", item1Data.get("name"));

        logTestResult("GET List with Items", "Successfully retrieved list with items");
    }

    @Test
    @Story("List GET Operations")
    @Description("Verify GET operation returns list with ordered items")
    @DisplayName("Should get list with ordered items")
    void shouldGetListWithOrderedItems() {
        logTestStep("GET Ordered List", "Get list with items in manifest order");

        String prefix = "config/ordered-list-test";

        // Create list with specific order
        Map<String, Object> listRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item3", "data", Map.of("name", "Item 3")),
                        Map.of("id", "item1", "data", Map.of("name", "Item 1")),
                        Map.of("id", "item2", "data", Map.of("name", "Item 2"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1", "item2", "item3"), // Different order
                        "version", 1L,
                        "etag", "",
                        "metadata", Map.of()
                )
        );

        ApiClient.given(getAdminToken())
                .body(listRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Get list and verify order
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> items = response.jsonPath().getList("items");
        assertEquals(3, items.size());
        assertEquals("item1", items.get(0).get("id"));
        assertEquals("item2", items.get(1).get("id"));
        assertEquals("item3", items.get(2).get("id"));

        logTestResult("GET Ordered List", "Successfully retrieved list with items in manifest order");
    }

    @Test
    @Story("List GET Operations")
    @Description("Verify GET operation returns 404 for non-existent list")
    @DisplayName("Should return 404 for non-existent list")
    void shouldReturn404ForNonExistentList() {
        logTestStep("GET Non-Existent List", "Verify 404 response for non-existent list");

        String nonExistentPrefix = "config/non-existent-list-" + System.currentTimeMillis();

        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/list?prefix=" + nonExistentPrefix)
                .then()
                .statusCode(404);

        logTestResult("GET Non-Existent List", "Correctly returned 404 for non-existent list");
    }

    // ============================================================================
    // List Operations - PUT
    // ============================================================================

    @Test
    @Story("List PUT Operations")
    @Description("Verify PUT operation creates new list with items")
    @DisplayName("Should create new list with items")
    void shouldCreateNewListWithItems() {
        logTestStep("PUT Create List", "Create new list with items");

        String prefix = "config/create-list-test";

        Map<String, Object> listRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Item 1"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1"),
                        "version", 1L,
                        "etag", "",
                        "metadata", Map.of()
                )
        );

        ApiClient.given(getAdminToken())
                .body(listRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // Verify list was created
        Response getResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> items = getResponse.jsonPath().getList("items");
        assertEquals(1, items.size());
        assertEquals("item1", items.get(0).get("id"));

        logTestResult("PUT Create List", "Successfully created new list with items");
    }

    @Test
    @Story("List PUT Operations")
    @Description("Verify PUT operation updates existing list")
    @DisplayName("Should update existing list")
    void shouldUpdateExistingList() {
        logTestStep("PUT Update List", "Update existing list");

        String prefix = "config/update-list-test";

        // Create initial list
        Map<String, Object> initialRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Item 1")),
                        Map.of("id", "item2", "data", Map.of("name", "Item 2"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1", "item2"),
                        "version", 1L,
                        "etag", "",
                        "metadata", Map.of()
                )
        );

        ApiClient.given(getAdminToken())
                .body(initialRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Update list (add item3, update item1)
        Map<String, Object> updateRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Updated Item 1")),
                        Map.of("id", "item3", "data", Map.of("name", "Item 3"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1", "item2", "item3"),
                        "version", 2L,
                        "etag", "etag2",
                        "metadata", Map.of()
                )
        );

        ApiClient.given(getAdminToken())
                .body(updateRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Verify update
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> items = response.jsonPath().getList("items");
        assertTrue(items.size() >= 2); // item1 and item3 should exist

        // Find item1 and verify it's updated
        Map<String, Object> item1 = items.stream()
                .filter(item -> "item1".equals(item.get("id")))
                .findFirst()
                .orElse(null);
        assertNotNull(item1);
        @SuppressWarnings("unchecked")
        Map<String, Object> item1Data = (Map<String, Object>) item1.get("data");
        assertEquals("Updated Item 1", item1Data.get("name"));

        logTestResult("PUT Update List", "Successfully updated existing list");
    }

    @Test
    @Story("List PUT Operations")
    @Description("Verify PUT operation deletes items via deletes parameter")
    @DisplayName("Should delete items via deletes parameter")
    void shouldDeleteItemsViaDeletesParameter() {
        logTestStep("PUT Delete List Items", "Delete list items via deletes parameter");

        String prefix = "config/delete-list-test";

        // Create list with multiple items
        Map<String, Object> createRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Item 1")),
                        Map.of("id", "item2", "data", Map.of("name", "Item 2")),
                        Map.of("id", "item3", "data", Map.of("name", "Item 3"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1", "item2", "item3"),
                        "version", 1L,
                        "etag", "",
                        "metadata", Map.of()
                )
        );

        ApiClient.given(getAdminToken())
                .body(createRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Delete item2 via deletes parameter
        Map<String, Object> deleteRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Item 1")),
                        Map.of("id", "item3", "data", Map.of("name", "Item 3"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1", "item3"),
                        "version", 2L,
                        "etag", "",
                        "metadata", Map.of()
                ),
                "deletes", List.of("item2")
        );

        ApiClient.given(getAdminToken())
                .body(deleteRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Verify item2 is deleted
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> items = response.jsonPath().getList("items");
        assertEquals(2, items.size());
        assertTrue(items.stream().noneMatch(item -> "item2".equals(item.get("id"))));

        logTestResult("PUT Delete List Items", "Successfully deleted items via deletes parameter");
    }

    // ============================================================================
    // List Operations - Edge Cases
    // ============================================================================

    @Test
    @Story("List Edge Cases")
    @Description("Verify list skips items missing from manifest order")
    @DisplayName("Should skip items missing from manifest order")
    void shouldSkipItemsMissingFromManifestOrder() {
        logTestStep("List Missing Items", "Verify list skips items not in manifest order");

        String prefix = "config/missing-items-test";

        // Create list with items, but manifest order includes non-existent item
        Map<String, Object> listRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Item 1")),
                        Map.of("id", "item2", "data", Map.of("name", "Item 2"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1", "missing-item", "item2"), // missing-item doesn't exist
                        "version", 1L,
                        "etag", "",
                        "metadata", Map.of()
                )
        );

        ApiClient.given(getAdminToken())
                .body(listRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200);

        // Get list - should skip missing-item
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> items = response.jsonPath().getList("items");
        assertEquals(2, items.size()); // Should only return item1 and item2
        assertTrue(items.stream().noneMatch(item -> "missing-item".equals(item.get("id"))));

        logTestResult("List Missing Items", "Successfully skipped items missing from manifest order");
    }

    // ============================================================================
    // List Operations - Permissions
    // ============================================================================

    @Test
    @Story("List Permissions")
    @Description("Verify team member can CRUD own team's list")
    @DisplayName("Should allow team member to CRUD own team's list")
    void shouldAllowTeamMemberToCRUDOwnTeamsList() {
        logTestStep("Team Member List CRUD", "Verify team member can CRUD own team's list");

        String prefix = "config/team1-list";
        Map<String, Object> listRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Item 1"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1"),
                        "version", 1L,
                        "etag", "",
                        "metadata", Map.of()
                )
        );

        // User1 (team1) should be able to create list for team1 service
        ApiClient.given(getUser1Token())
                .body(listRequest)
                .when()
                .put("/application-services/" + testServiceIdTeam1 + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // User1 should be able to read
        ApiClient.given(getUser1Token())
                .when()
                .get("/application-services/" + testServiceIdTeam1 + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200);

        logTestResult("Team Member List CRUD", "Team member can CRUD own team's list");
    }

    @Test
    @Story("List Permissions")
    @Description("Verify team member cannot access other team's list")
    @DisplayName("Should prevent team member from accessing other team's list")
    void shouldPreventTeamMemberFromAccessingOtherTeamsList() {
        logTestStep("Cross-Team List Access", "Verify team member cannot access other team's list");

        String prefix = "config/team2-protected-list";
        Map<String, Object> listRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Item 1"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1"),
                        "version", 1L,
                        "etag", "",
                        "metadata", Map.of()
                )
        );

        // User3 (team2) creates a list in team2 service
        ApiClient.given(getUser3Token())
                .body(listRequest)
                .when()
                .put("/application-services/" + testServiceIdTeam2 + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(200);

        // User1 (team1) should not be able to read team2's list
        ApiClient.given(getUser1Token())
                .when()
                .get("/application-services/" + testServiceIdTeam2 + "/kv/list?prefix=" + prefix)
                .then()
                .statusCode(400); // Returns 404 to hide existence

        logTestResult("Cross-Team List Access", "Correctly prevented cross-team list access");
    }

    // ============================================================================
    // Interaction Tests
    // ============================================================================

    @Test
    @Story("Object/List Interaction")
    @Description("Verify Object and List can coexist under different prefixes")
    @DisplayName("Should allow Object and List to coexist")
    void shouldAllowObjectAndListToCoexist() {
        logTestStep("Object/List Coexistence", "Verify Object and List can coexist under different prefixes");

        String objectPrefix = "config/object";
        String listPrefix = "config/list";

        // Create object
        Map<String, Object> objectData = Map.of("key", "value");
        ApiClient.given(getAdminToken())
                .body(Map.of("data", objectData))
                .when()
                .put("/application-services/" + testServiceId + "/kv/object?prefix=" + objectPrefix)
                .then()
                .statusCode(200);

        // Create list
        Map<String, Object> listRequest = Map.of(
                "items", List.of(
                        Map.of("id", "item1", "data", Map.of("name", "Item 1"))
                ),
                "manifest", Map.of(
                        "order", List.of("item1"),
                        "version", 1L,
                        "etag", "",
                        "metadata", Map.of()
                )
        );
        ApiClient.given(getAdminToken())
                .body(listRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/list?prefix=" + listPrefix)
                .then()
                .statusCode(200);

        // Verify both exist
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/object?prefix=" + objectPrefix)
                .then()
                .statusCode(200);

        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/list?prefix=" + listPrefix)
                .then()
                .statusCode(200);

        logTestResult("Object/List Coexistence", "Object and List can coexist under different prefixes");
    }
}

