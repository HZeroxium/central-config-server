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

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * KV Store regression tests for E2E testing.
 * <p>
 * Tests comprehensive KV CRUD operations including:
 * - GET operations (JSON metadata, raw bytes, 404 handling)
 * - LIST operations (keys only, entries, prefix filtering, recurse)
 * - PUT operations (create, update, CAS, If-Match header)
 * - DELETE operations (single key, recurse, CAS)
 * - Permission checks (team access, SYS_ADMIN, shared services)
 * - Automatic test data cleanup
 * </p>
 */
@Slf4j
@Epic("Key-Value Store")
@Feature("KV CRUD Operations")
@DisplayName("KV Store Regression Tests")
public class KVRegressionTest extends BaseE2ETest {

    private static String testServiceId;
    private static String testServiceIdTeam1;
    private static String testServiceIdTeam2;

    @BeforeAll
    @Description("Setup test ApplicationServices for KV operations")
    void setUpTestServices() {
        logTestStep("Setup Test Services", "Create test ApplicationServices for KV operations");

        // Create test service owned by admin (for general tests)
        String serviceId = TestDataGenerator.generateServiceName("kv-test");
        Map<String, Object> createServiceRequest = Map.of(
                "id", serviceId,
                "displayName", serviceId,
                "tags", TestDataGenerator.generateStringList(2, "kv-test"),
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
        String serviceIdTeam1 = TestDataGenerator.generateServiceName("kv-test-team1");
        Map<String, Object> createTeam1Request = Map.of(
                "id", serviceIdTeam1,
                "displayName", serviceIdTeam1,
                "ownerTeamId", TestUsers.TEAM1,
                "tags", TestDataGenerator.generateStringList(2, "kv-test-team1"),
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
        String serviceIdTeam2 = TestDataGenerator.generateServiceName("kv-test-team2");
        Map<String, Object> createTeam2Request = Map.of(
                "id", serviceIdTeam2,
                "displayName", serviceIdTeam2,
                "ownerTeamId", TestUsers.TEAM2,
                "tags", TestDataGenerator.generateStringList(2, "kv-test-team2"),
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
    @Description("Cleanup test data: delete all KV keys and test ApplicationServices")
    void tearDownTestData() {
        logTestStep("Cleanup Test Data", "Delete all test KV keys and ApplicationServices");

        try {
            // Cleanup KV keys for testServiceId (delete recursively)
            try {
                ApiClient.given(getAdminToken())
                        .when()
                        .delete("/application-services/" + testServiceId + "/kv?recurse=true")
                        .then()
                        .statusCode(anyOf(is(200), is(404)));
                log.debug("Cleaned up KV keys for service: {}", testServiceId);
            } catch (Exception e) {
                log.warn("Failed to cleanup KV keys for service {}: {}", testServiceId, e.getMessage());
            }

            // Cleanup KV keys for testServiceIdTeam1
            try {
                ApiClient.given(getAdminToken())
                        .when()
                        .delete("/application-services/" + testServiceIdTeam1 + "/kv?recurse=true")
                        .then()
                        .statusCode(anyOf(is(200), is(404)));
                log.debug("Cleaned up KV keys for service: {}", testServiceIdTeam1);
            } catch (Exception e) {
                log.warn("Failed to cleanup KV keys for service {}: {}", testServiceIdTeam1, e.getMessage());
            }

            // Cleanup KV keys for testServiceIdTeam2
            try {
                ApiClient.given(getAdminToken())
                        .when()
                        .delete("/application-services/" + testServiceIdTeam2 + "/kv?recurse=true")
                        .then()
                        .statusCode(anyOf(is(200), is(404)));
                log.debug("Cleaned up KV keys for service: {}", testServiceIdTeam2);
            } catch (Exception e) {
                log.warn("Failed to cleanup KV keys for service {}: {}", testServiceIdTeam2, e.getMessage());
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
    // GET Operations
    // ============================================================================

    @Test
    @Story("GET Operations")
    @Description("Verify GET operation returns JSON metadata for existing key")
    @DisplayName("Should get KV entry with JSON metadata")
    void shouldGetKVEntryWithJsonMetadata() {
        logTestStep("GET JSON Metadata", "Get KV entry with JSON metadata format");

        // Create a test key
        String keyPath = "config/test-key-1";
        Map<String, Object> putRequest = Map.of(
                "value", "test-value-1",
                "encoding", "utf8"
        );

        ApiClient.given(getAdminToken())
                .body(putRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // Get the key with JSON metadata
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .body("path", equalTo(keyPath))
                .body("valueBase64", notNullValue())
                .body("modifyIndex", notNullValue())
                .extract().response();

        // Verify value can be decoded
        String valueBase64 = response.jsonPath().getString("valueBase64");
        String decodedValue = new String(Base64.getDecoder().decode(valueBase64));
        assertEquals("test-value-1", decodedValue);

        logTestResult("GET JSON Metadata", "Successfully retrieved KV entry with JSON metadata");
    }

    @Test
    @Story("GET Operations")
    @Description("Verify GET operation returns raw bytes when raw=true")
    @DisplayName("Should get KV entry as raw bytes")
    void shouldGetKVEntryAsRawBytes() {
        logTestStep("GET Raw Bytes", "Get KV entry as raw bytes");

        // Create a test key
        String keyPath = "config/test-key-raw";
        String testValue = "raw-test-value-123";
        Map<String, Object> putRequest = Map.of(
                "value", testValue,
                "encoding", "utf8"
        );

        ApiClient.given(getAdminToken())
                .body(putRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Get the key as raw bytes
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath + "?raw=true")
                .then()
                .statusCode(200)
                .contentType(containsString("application/octet-stream"))
                .extract().response();

        String rawValue = response.asString();
        assertEquals(testValue, rawValue);

        logTestResult("GET Raw Bytes", "Successfully retrieved KV entry as raw bytes");
    }

    @Test
    @Story("GET Operations")
    @Description("Verify GET operation returns 404 for non-existent key")
    @DisplayName("Should return 404 for non-existent key")
    void shouldReturn404ForNonExistentKey() {
        logTestStep("GET Non-Existent Key", "Verify 404 response for non-existent key");

        String nonExistentKey = "config/non-existent-key-" + System.currentTimeMillis();

        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + nonExistentKey)
                .then()
                .statusCode(404);

        logTestResult("GET Non-Existent Key", "Correctly returned 404 for non-existent key");
    }

    @Test
    @Story("GET Operations")
    @Description("Verify GET operation returns 404 for invalid service")
    @DisplayName("Should return 404 for invalid service")
    void shouldReturn404ForInvalidService() {
        logTestStep("GET Invalid Service", "Verify 404 response for invalid service");

        String invalidServiceId = "non-existent-service-" + System.currentTimeMillis();

        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + invalidServiceId + "/kv/config/test")
                .then()
                .statusCode(404);

        logTestResult("GET Invalid Service", "Correctly returned 404 for invalid service");
    }

    // ============================================================================
    // LIST Operations
    // ============================================================================

    @Test
    @Story("LIST Operations")
    @Description("Verify LIST operation returns all keys when keysOnly=true")
    @DisplayName("Should list all KV keys")
    void shouldListAllKVKeys() {
        logTestStep("LIST Keys", "List all KV keys with keysOnly=true");

        // Create multiple test keys
        String[] keyPaths = {
                "config/key1",
                "config/key2",
                "config/subdir/key3"
        };

        for (String keyPath : keyPaths) {
            Map<String, Object> putRequest = Map.of(
                    "value", "value-for-" + keyPath,
                    "encoding", "utf8"
            );
            ApiClient.given(getAdminToken())
                    .body(putRequest)
                    .when()
                    .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                    .then()
                    .statusCode(200);
        }

        // List all keys
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv?keysOnly=true&recurse=true")
                .then()
                .statusCode(200)
                .body("keys", notNullValue())
                .extract().response();

        List<String> keys = response.jsonPath().getList("keys");
        assertTrue(keys.size() >= keyPaths.length, "Should return at least the created keys");
        assertTrue(keys.containsAll(List.of(keyPaths)), "Should contain all created keys");

        logTestResult("LIST Keys", "Successfully listed all KV keys");
    }

    @Test
    @Story("LIST Operations")
    @Description("Verify LIST operation returns all entries when keysOnly=false")
    @DisplayName("Should list all KV entries")
    void shouldListAllKVEntries() {
        logTestStep("LIST Entries", "List all KV entries with keysOnly=false");

        // Create a test key
        String keyPath = "config/list-test-key";
        Map<String, Object> putRequest = Map.of(
                "value", "list-test-value",
                "encoding", "utf8"
        );

        ApiClient.given(getAdminToken())
                .body(putRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // List all entries
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv?keysOnly=false&recurse=true")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .extract().response();

        List<Map<String, Object>> items = response.jsonPath().getList("items");
        assertTrue(items.size() > 0, "Should return at least one entry");

        // Verify entry structure
        boolean found = items.stream()
                .anyMatch(item -> keyPath.equals(item.get("path")));
        assertTrue(found, "Should contain the created key");

        logTestResult("LIST Entries", "Successfully listed all KV entries");
    }

    @Test
    @Story("LIST Operations")
    @Description("Verify LIST operation with prefix filter")
    @DisplayName("Should list KV keys with prefix filter")
    void shouldListKVKeysWithPrefixFilter() {
        logTestStep("LIST with Prefix", "List KV keys with prefix filter");

        // Create keys under different prefixes
        String[] keyPaths = {
                "config/prefix-test-1",
                "config/prefix-test-2",
                "other/prefix-test-3"
        };

        for (String keyPath : keyPaths) {
            Map<String, Object> putRequest = Map.of(
                    "value", "value-for-" + keyPath,
                    "encoding", "utf8"
            );
            ApiClient.given(getAdminToken())
                    .body(putRequest)
                    .when()
                    .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                    .then()
                    .statusCode(200);
        }

        // List keys with prefix filter
        Response response = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv?prefix=config/&keysOnly=true&recurse=true")
                .then()
                .statusCode(200)
                .extract().response();

        List<String> keys = response.jsonPath().getList("keys");
        assertTrue(keys.stream().anyMatch(k -> k.contains("config/prefix-test-1")), "Should contain config prefix keys");
        assertTrue(keys.stream().anyMatch(k -> k.contains("config/prefix-test-2")), "Should contain config prefix keys");
        assertFalse(keys.stream().anyMatch(k -> k.contains("other/prefix-test-3")), "Should not contain other prefix keys");

        logTestResult("LIST with Prefix", "Successfully filtered keys by prefix");
    }

    // ============================================================================
    // PUT Operations
    // ============================================================================

    @Test
    @Story("PUT Operations")
    @Description("Verify PUT operation creates new key")
    @DisplayName("Should create new KV entry")
    void shouldCreateNewKVEntry() {
        logTestStep("PUT Create", "Create new KV entry");

        String keyPath = "config/create-test-key";
        String testValue = "create-test-value";
        Map<String, Object> putRequest = Map.of(
                "value", testValue,
                "encoding", "utf8"
        );

        ApiClient.given(getAdminToken())
                .body(putRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("modifyIndex", notNullValue());

        // Verify key was created
        Response getResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .extract().response();

        String valueBase64 = getResponse.jsonPath().getString("valueBase64");
        String decodedValue = new String(Base64.getDecoder().decode(valueBase64));
        assertEquals(testValue, decodedValue);

        logTestResult("PUT Create", "Successfully created new KV entry");
    }

    @Test
    @Story("PUT Operations")
    @Description("Verify PUT operation updates existing key")
    @DisplayName("Should update existing KV entry")
    void shouldUpdateExistingKVEntry() {
        logTestStep("PUT Update", "Update existing KV entry");

        String keyPath = "config/update-test-key";
        String initialValue = "initial-value";
        String updatedValue = "updated-value";

        // Create initial key
        Map<String, Object> createRequest = Map.of(
                "value", initialValue,
                "encoding", "utf8"
        );
        ApiClient.given(getAdminToken())
                .body(createRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Get initial modifyIndex
        Response initialResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .extract().response();

        Long initialModifyIndex = initialResponse.jsonPath().getLong("modifyIndex");

        // Update the key
        Map<String, Object> updateRequest = Map.of(
                "value", updatedValue,
                "encoding", "utf8"
        );
        Response updateResponse = ApiClient.given(getAdminToken())
                .body(updateRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .extract().response();

        Long newModifyIndex = updateResponse.jsonPath().getLong("modifyIndex");
        assertTrue(newModifyIndex > initialModifyIndex, "ModifyIndex should increase after update");

        // Verify value was updated
        Response getResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .extract().response();

        String valueBase64 = getResponse.jsonPath().getString("valueBase64");
        String decodedValue = new String(Base64.getDecoder().decode(valueBase64));
        assertEquals(updatedValue, decodedValue);

        logTestResult("PUT Update", "Successfully updated existing KV entry");
    }

    @Test
    @Story("PUT Operations")
    @Description("Verify PUT operation with CAS succeeds when index matches")
    @DisplayName("Should update KV entry with CAS when index matches")
    void shouldUpdateKVEntryWithCASWhenIndexMatches() {
        logTestStep("PUT with CAS Success", "Update KV entry with CAS when index matches");

        String keyPath = "config/cas-test-key";
        String initialValue = "cas-initial-value";
        String updatedValue = "cas-updated-value";

        // Create initial key
        Map<String, Object> createRequest = Map.of(
                "value", initialValue,
                "encoding", "utf8"
        );
        ApiClient.given(getAdminToken())
                .body(createRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Get current modifyIndex
        Response getResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .extract().response();

        Long currentModifyIndex = getResponse.jsonPath().getLong("modifyIndex");

        // Update with CAS using current modifyIndex
        Map<String, Object> updateRequest = Map.of(
                "value", updatedValue,
                "encoding", "utf8",
                "cas", currentModifyIndex
        );
        ApiClient.given(getAdminToken())
                .body(updateRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        logTestResult("PUT with CAS Success", "Successfully updated KV entry with CAS");
    }

    @Test
    @Story("PUT Operations")
    @Description("Verify PUT operation with CAS fails when index doesn't match")
    @DisplayName("Should return 409 when CAS index doesn't match")
    void shouldReturn409WhenCASIndexDoesNotMatch() {
        logTestStep("PUT with CAS Conflict", "Verify CAS conflict when index doesn't match");

        String keyPath = "config/cas-conflict-test-key";
        String initialValue = "cas-conflict-initial";

        // Create initial key
        Map<String, Object> createRequest = Map.of(
                "value", initialValue,
                "encoding", "utf8"
        );
        ApiClient.given(getAdminToken())
                .body(createRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Get current modifyIndex
        Response getResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .extract().response();

        Long currentModifyIndex = getResponse.jsonPath().getLong("modifyIndex");

        // Try to update with wrong CAS index
        Map<String, Object> updateRequest = Map.of(
                "value", "cas-conflict-updated",
                "encoding", "utf8",
                "cas", currentModifyIndex - 1 // Wrong index
        );
        ApiClient.given(getAdminToken())
                .body(updateRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(409)
                .body("success", equalTo(false));

        logTestResult("PUT with CAS Conflict", "Correctly returned 409 for CAS conflict");
    }

    @Test
    @Story("PUT Operations")
    @Description("Verify PUT operation with If-Match header for CAS")
    @DisplayName("Should update KV entry with If-Match header")
    void shouldUpdateKVEntryWithIfMatchHeader() {
        logTestStep("PUT with If-Match", "Update KV entry using If-Match header for CAS");

        String keyPath = "config/ifmatch-test-key";
        String initialValue = "ifmatch-initial";
        String updatedValue = "ifmatch-updated";

        // Create initial key
        Map<String, Object> createRequest = Map.of(
                "value", initialValue,
                "encoding", "utf8"
        );
        ApiClient.given(getAdminToken())
                .body(createRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Get current modifyIndex
        Response getResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .extract().response();

        Long currentModifyIndex = getResponse.jsonPath().getLong("modifyIndex");

        // Update with If-Match header
        Map<String, Object> updateRequest = Map.of(
                "value", updatedValue,
                "encoding", "utf8"
        );
        ApiClient.given(getAdminToken())
                .header("If-Match", String.valueOf(currentModifyIndex))
                .body(updateRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        logTestResult("PUT with If-Match", "Successfully updated KV entry with If-Match header");
    }

    // ============================================================================
    // DELETE Operations
    // ============================================================================

    @Test
    @Story("DELETE Operations")
    @Description("Verify DELETE operation deletes single key")
    @DisplayName("Should delete single KV entry")
    void shouldDeleteSingleKVEntry() {
        logTestStep("DELETE Single Key", "Delete single KV entry");

        String keyPath = "config/delete-test-key";
        Map<String, Object> putRequest = Map.of(
                "value", "delete-test-value",
                "encoding", "utf8"
        );

        // Create key
        ApiClient.given(getAdminToken())
                .body(putRequest)
                .when()
                .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Verify key exists
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Delete key
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // Verify key is deleted
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv/" + keyPath)
                .then()
                .statusCode(404);

        logTestResult("DELETE Single Key", "Successfully deleted single KV entry");
    }

    @Test
    @Story("DELETE Operations")
    @Description("Verify DELETE operation with recurse deletes all keys under prefix")
    @DisplayName("Should delete KV entries recursively")
    void shouldDeleteKVEntriesRecursively() {
        logTestStep("DELETE Recurse", "Delete KV entries recursively");

        // Create multiple keys under a prefix
        String[] keyPaths = {
                "recurse-test/key1",
                "recurse-test/key2",
                "recurse-test/subdir/key3"
        };

        for (String keyPath : keyPaths) {
            Map<String, Object> putRequest = Map.of(
                    "value", "value-for-" + keyPath,
                    "encoding", "utf8"
            );
            ApiClient.given(getAdminToken())
                    .body(putRequest)
                    .when()
                    .put("/application-services/" + testServiceId + "/kv/" + keyPath)
                    .then()
                    .statusCode(200);
        }

        // Verify keys exist
        Response listResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv?prefix=recurse-test/&keysOnly=true&recurse=true")
                .then()
                .statusCode(200)
                .extract().response();

        List<String> keysBefore = listResponse.jsonPath().getList("keys");
        assertTrue(keysBefore.size() >= keyPaths.length, "Should have created keys");

        // Delete recursively
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + testServiceId + "/kv/recurse-test?recurse=true")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // Verify all keys are deleted
        Response listAfterResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceId + "/kv?prefix=recurse-test/&keysOnly=true&recurse=true")
                .then()
                .statusCode(200)
                .extract().response();

        List<String> keysAfter = listAfterResponse.jsonPath().getList("keys");
        assertTrue(keysAfter.isEmpty() || !keysAfter.stream().anyMatch(k -> k.startsWith("recurse-test/")),
                "All keys under prefix should be deleted");

        logTestResult("DELETE Recurse", "Successfully deleted KV entries recursively");
    }

    // ============================================================================
    // Permission Tests
    // ============================================================================

    @Test
    @Story("Permission Tests")
    @Description("Verify team member can CRUD KV for own team's service")
    @DisplayName("Should allow team member to CRUD own team's KV")
    void shouldAllowTeamMemberToCRUDOwnTeamsKV() {
        logTestStep("Team Member CRUD", "Verify team member can CRUD own team's KV");

        String keyPath = "config/team1-test-key";
        Map<String, Object> putRequest = Map.of(
                "value", "team1-value",
                "encoding", "utf8"
        );

        // User1 (team1) should be able to create KV for team1 service
        ApiClient.given(getUser1Token())
                .body(putRequest)
                .when()
                .put("/application-services/" + testServiceIdTeam1 + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // User1 should be able to read
        ApiClient.given(getUser1Token())
                .when()
                .get("/application-services/" + testServiceIdTeam1 + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // User1 should be able to delete
        ApiClient.given(getUser1Token())
                .when()
                .delete("/application-services/" + testServiceIdTeam1 + "/kv/" + keyPath)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        logTestResult("Team Member CRUD", "Team member can CRUD own team's KV");
    }

    @Test
    @Story("Permission Tests")
    @Description("Verify team member cannot access other team's KV")
    @DisplayName("Should prevent team member from accessing other team's KV")
    void shouldPreventTeamMemberFromAccessingOtherTeamsKV() {
        logTestStep("Cross-Team Access", "Verify team member cannot access other team's KV");

        String keyPath = "config/team2-protected-key";
        Map<String, Object> putRequest = Map.of(
                "value", "team2-protected-value",
                "encoding", "utf8"
        );

        // User3 (team2) creates a key in team2 service
        ApiClient.given(getUser3Token())
                .body(putRequest)
                .when()
                .put("/application-services/" + testServiceIdTeam2 + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // User1 (team1) should not be able to read team2's KV
        ApiClient.given(getUser1Token())
                .when()
                .get("/application-services/" + testServiceIdTeam2 + "/kv/" + keyPath)
                .then()
                .statusCode(404); // Returns 404 to hide existence

        // User1 should not be able to update
        Map<String, Object> updateRequest = Map.of(
                "value", "unauthorized-update",
                "encoding", "utf8"
        );
        ApiClient.given(getUser1Token())
                .body(updateRequest)
                .when()
                .put("/application-services/" + testServiceIdTeam2 + "/kv/" + keyPath)
                .then()
                .statusCode(404);

        // User1 should not be able to delete
        ApiClient.given(getUser1Token())
                .when()
                .delete("/application-services/" + testServiceIdTeam2 + "/kv/" + keyPath)
                .then()
                .statusCode(404);

        logTestResult("Cross-Team Access", "Correctly prevented cross-team KV access");
    }

    @Test
    @Story("Permission Tests")
    @Description("Verify SYS_ADMIN can access all KV")
    @DisplayName("Should allow SYS_ADMIN to access all KV")
    void shouldAllowSysAdminToAccessAllKV() {
        logTestStep("SYS_ADMIN Access", "Verify SYS_ADMIN can access all KV");

        String keyPath = "config/admin-test-key";
        Map<String, Object> putRequest = Map.of(
                "value", "admin-test-value",
                "encoding", "utf8"
        );

        // User3 (team2) creates a key in team2 service
        ApiClient.given(getUser3Token())
                .body(putRequest)
                .when()
                .put("/application-services/" + testServiceIdTeam2 + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Admin should be able to read
        ApiClient.given(getAdminToken())
                .when()
                .get("/application-services/" + testServiceIdTeam2 + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Admin should be able to update
        Map<String, Object> updateRequest = Map.of(
                "value", "admin-updated-value",
                "encoding", "utf8"
        );
        ApiClient.given(getAdminToken())
                .body(updateRequest)
                .when()
                .put("/application-services/" + testServiceIdTeam2 + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        // Admin should be able to delete
        ApiClient.given(getAdminToken())
                .when()
                .delete("/application-services/" + testServiceIdTeam2 + "/kv/" + keyPath)
                .then()
                .statusCode(200);

        logTestResult("SYS_ADMIN Access", "SYS_ADMIN can access all KV");
    }
}

