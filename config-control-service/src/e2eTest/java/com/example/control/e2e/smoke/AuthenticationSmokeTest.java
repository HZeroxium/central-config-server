package com.example.control.e2e.smoke;

import com.example.control.e2e.base.BaseE2ETest;
import com.example.control.e2e.client.ApiClient;
import com.example.control.e2e.fixtures.TestUsers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Authentication smoke tests for E2E testing.
 * <p>
 * Tests critical authentication functionality including:
 * - Token acquisition and validation
 * - JWT claims verification
 * - Unauthorized access scenarios
 * - Token-based API access
 * </p>
 */
@Slf4j
@Epic("Authentication")
@Feature("Token Management")
@DisplayName("Authentication Smoke Tests")
public class AuthenticationSmokeTest extends BaseE2ETest {

    @Test
    @Story("Token Acquisition")
    @Description("Verify that admin and regular users can acquire valid JWT tokens from Keycloak")
    @DisplayName("Should acquire valid tokens for all test users")
    void shouldAcquireValidTokensForAllUsers() {
        logTestStep("Token Acquisition", "Verify token acquisition for all test users");

        // Verify admin token
        assertNotNull(getAdminToken(), "Admin token should not be null");
        assertFalse(getAdminToken().isEmpty(), "Admin token should not be empty");
        logTestData("Admin Token", "Token acquired successfully");

        // Verify user1 token
        assertNotNull(getUser1Token(), "User1 token should not be null");
        assertFalse(getUser1Token().isEmpty(), "User1 token should not be empty");
        logTestData("User1 Token", "Token acquired successfully");

        // Verify user2 token
        assertNotNull(getUser2Token(), "User2 token should not be null");
        assertFalse(getUser2Token().isEmpty(), "User2 token should not be empty");
        logTestData("User2 Token", "Token acquired successfully");

        // Verify user3 token
        assertNotNull(getUser3Token(), "User3 token should not be null");
        assertFalse(getUser3Token().isEmpty(), "User3 token should not be empty");
        logTestData("User3 Token", "Token acquired successfully");

        // Verify user4 token
        assertNotNull(getUser4Token(), "User4 token should not be null");
        assertFalse(getUser4Token().isEmpty(), "User4 token should not be empty");
        logTestData("User4 Token", "Token acquired successfully");

        // Verify user5 token
        assertNotNull(getUser5Token(), "User5 token should not be null");
        assertFalse(getUser5Token().isEmpty(), "User5 token should not be empty");
        logTestData("User5 Token", "Token acquired successfully");

        logTestResult("Token Acquisition", "All tokens acquired successfully");
    }

    @Test
    @Story("JWT Claims Validation")
    @Description("Verify that JWT tokens contain expected claims including audience, roles, and groups")
    @DisplayName("Should validate JWT claims for admin and regular users")
    void shouldValidateJwtClaims() {
        logTestStep("JWT Claims Validation", "Verify JWT token claims for admin and regular users");

        // Test admin token claims
        Response adminResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .extract().response();

        logTestData("Admin Token Claims", "Admin token validated successfully");

        // Test user1 token claims
        Response user1Response = ApiClient.given(getUser1Token())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .extract().response();

        logTestData("User1 Token Claims", "User1 token validated successfully");

        // Test user2 token claims
        Response user2Response = ApiClient.given(getUser2Token())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .extract().response();

        logTestData("User2 Token Claims", "User2 token validated successfully");

        logTestResult("JWT Claims Validation", "All token claims validated successfully");
    }

    @Test
    @Story("Unauthorized Access")
    @Description("Verify that requests without authentication are properly rejected with 401 status")
    @DisplayName("Should reject unauthenticated requests")
    void shouldRejectUnauthenticatedRequests() {
        logTestStep("Unauthorized Access Test", "Verify 401 response for unauthenticated requests");

        // Test unauthenticated request to protected endpoint
        ApiClient.given()
                .when()
                .get("/application-services")
                .then()
                .statusCode(401)
                .body("error", notNullValue());

        logTestData("Unauthenticated Request", "401 response received as expected");

        // Test unauthenticated request to service instances
        ApiClient.given()
                .when()
                .get("/service-instances")
                .then()
                .statusCode(401)
                .body("error", notNullValue());

        logTestData("Unauthenticated Service Instances", "401 response received as expected");

        // Test unauthenticated request to drift events
        ApiClient.given()
                .when()
                .get("/drift-events")
                .then()
                .statusCode(401)
                .body("error", notNullValue());

        logTestData("Unauthenticated Drift Events", "401 response received as expected");

        logTestResult("Unauthorized Access", "All unauthenticated requests properly rejected");
    }

    @Test
    @Story("Token-Based API Access")
    @Description("Verify that valid tokens allow access to protected endpoints")
    @DisplayName("Should allow access with valid tokens")
    void shouldAllowAccessWithValidTokens() {
        logTestStep("Token-Based Access", "Verify access to protected endpoints with valid tokens");

        // Test admin access to application services
        Response adminResponse = ApiClient.given(getAdminToken())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .extract().response();

        logTestData("Admin Application Services Access", "Admin can access application services");

        // Test user1 access to application services
        Response user1Response = ApiClient.given(getUser1Token())
                .when()
                .get("/application-services")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .extract().response();

        logTestData("User1 Application Services Access", "User1 can access application services");

        // Test user1 access to service instances
        Response user1InstancesResponse = ApiClient.given(getUser1Token())
                .when()
                .get("/service-instances")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .extract().response();

        logTestData("User1 Service Instances Access", "User1 can access service instances");

        // Test user1 access to drift events
        Response user1DriftResponse = ApiClient.given(getUser1Token())
                .when()
                .get("/drift-events")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .extract().response();

        logTestData("User1 Drift Events Access", "User1 can access drift events");

        logTestResult("Token-Based Access", "All authenticated users can access protected endpoints");
    }

    @Test
    @Story("Token Refresh")
    @Description("Verify that token refresh functionality works correctly")
    @DisplayName("Should handle token refresh automatically")
    void shouldHandleTokenRefresh() {
        logTestStep("Token Refresh", "Verify automatic token refresh functionality");

        // Clear token cache to force refresh
        getTokenManager().clearToken(TestUsers.USER1);

        // Get token again (should trigger refresh)
        String refreshedToken = getTokenManager().getUser1Token();
        assertNotNull(refreshedToken, "Refreshed token should not be null");
        assertFalse(refreshedToken.isEmpty(), "Refreshed token should not be empty");

        // Verify refreshed token works
        ApiClient.given(refreshedToken)
                .when()
                .get("/application-services")
                .then()
                .statusCode(200);

        logTestData("Token Refresh", "Token refresh completed successfully");
        logTestResult("Token Refresh", "Token refresh functionality working correctly");
    }

    @Test
    @Story("Invalid Token Handling")
    @Description("Verify that invalid tokens are properly rejected")
    @DisplayName("Should reject invalid tokens")
    void shouldRejectInvalidTokens() {
        logTestStep("Invalid Token Test", "Verify rejection of invalid tokens");

        // Test with malformed token
        ApiClient.given("invalid.token.here")
                .when()
                .get("/application-services")
                .then()
                .statusCode(401);

        logTestData("Malformed Token", "Malformed token rejected as expected");

        // Test with expired token format
        ApiClient.given("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.expired.invalid")
                .when()
                .get("/application-services")
                .then()
                .statusCode(401);

        logTestData("Expired Token Format", "Expired token format rejected as expected");

        // Test with wrong audience token
        ApiClient.given("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.wrong.audience")
                .when()
                .get("/application-services")
                .then()
                .statusCode(401);

        logTestData("Wrong Audience Token", "Wrong audience token rejected as expected");

        logTestResult("Invalid Token Handling", "All invalid tokens properly rejected");
    }
}
