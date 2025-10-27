package com.example.control.e2e.client;

import com.example.control.e2e.base.TestConfig;
import io.qameta.allure.Allure;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

/**
 * REST Assured wrapper for E2E API testing.
 * <p>
 * Provides a fluent API for making HTTP requests with automatic authentication,
 * logging, and Allure reporting integration.
 * </p>
 */
@Slf4j
public class ApiClient {

    private static final TestConfig config = TestConfig.getInstance();
    private static boolean initialized = false;

    static {
        initializeRestAssured();
    }

    /**
     * Initialize REST Assured configuration.
     */
    private static void initializeRestAssured() {
        if (!initialized) {
            RestAssured.baseURI = config.getApiBaseUrl();
            RestAssured.requestSpecification = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .filter(new AllureRestAssured());
            
            log.info("REST Assured initialized with base URI: {}", config.getApiBaseUrl());
            initialized = true;
        }
    }

    /**
     * Create a new request specification with authentication.
     *
     * @param token the authentication token
     * @return RequestSpecification with auth header
     */
    public static RequestSpecification given(String token) {
        return RestAssured.given()
                .header("Authorization", "Bearer " + token);
    }

    /**
     * Create a new request specification without authentication.
     *
     * @return RequestSpecification without auth header
     */
    public static RequestSpecification given() {
        return RestAssured.given();
    }

    /**
     * Create a new request specification with admin authentication.
     *
     * @return RequestSpecification with admin auth
     */
    public static RequestSpecification givenAdmin() {
        String adminToken = com.example.control.e2e.base.AuthTokenManager.getInstance().getAdminToken();
        return given(adminToken);
    }

    /**
     * Create a new request specification with user1 authentication.
     *
     * @return RequestSpecification with user1 auth
     */
    public static RequestSpecification givenUser1() {
        String user1Token = com.example.control.e2e.base.AuthTokenManager.getInstance().getUser1Token();
        return given(user1Token);
    }

    /**
     * Create a new request specification with user2 authentication.
     *
     * @return RequestSpecification with user2 auth
     */
    public static RequestSpecification givenUser2() {
        String user2Token = com.example.control.e2e.base.AuthTokenManager.getInstance().getUser2Token();
        return given(user2Token);
    }

    /**
     * Create a new request specification with user3 authentication.
     *
     * @return RequestSpecification with user3 auth
     */
    public static RequestSpecification givenUser3() {
        String user3Token = com.example.control.e2e.base.AuthTokenManager.getInstance().getUser3Token();
        return given(user3Token);
    }

    /**
     * Create a new request specification with user4 authentication.
     *
     * @return RequestSpecification with user4 auth
     */
    public static RequestSpecification givenUser4() {
        String user4Token = com.example.control.e2e.base.AuthTokenManager.getInstance().getUser4Token();
        return given(user4Token);
    }

    /**
     * Create a new request specification with user5 authentication.
     *
     * @return RequestSpecification with user5 auth
     */
    public static RequestSpecification givenUser5() {
        String user5Token = com.example.control.e2e.base.AuthTokenManager.getInstance().getUser5Token();
        return given(user5Token);
    }

    /**
     * Execute a GET request and return response.
     *
     * @param spec the request specification
     * @param path the request path
     * @return Response object
     */
    public static Response get(RequestSpecification spec, String path) {
        return executeRequest(spec, "GET", path, null);
    }

    /**
     * Execute a POST request and return response.
     *
     * @param spec the request specification
     * @param path the request path
     * @param body the request body
     * @return Response object
     */
    public static Response post(RequestSpecification spec, String path, Object body) {
        return executeRequest(spec, "POST", path, body);
    }

    /**
     * Execute a PUT request and return response.
     *
     * @param spec the request specification
     * @param path the request path
     * @param body the request body
     * @return Response object
     */
    public static Response put(RequestSpecification spec, String path, Object body) {
        return executeRequest(spec, "PUT", path, body);
    }

    /**
     * Execute a DELETE request and return response.
     *
     * @param spec the request specification
     * @param path the request path
     * @return Response object
     */
    public static Response delete(RequestSpecification spec, String path) {
        return executeRequest(spec, "DELETE", path, null);
    }

    /**
     * Execute a PATCH request and return response.
     *
     * @param spec the request specification
     * @param path the request path
     * @param body the request body
     * @return Response object
     */
    public static Response patch(RequestSpecification spec, String path, Object body) {
        return executeRequest(spec, "PATCH", path, body);
    }

    /**
     * Execute a request with logging and Allure reporting.
     *
     * @param spec the request specification
     * @param method the HTTP method
     * @param path the request path
     * @param body the request body
     * @return Response object
     */
    private static Response executeRequest(RequestSpecification spec, String method, String path, Object body) {
        Instant startTime = Instant.now();
        
        log.debug("Executing {} {} {}", method, config.getApiBaseUrl(), path);
        
        try {
            RequestSpecification requestSpec = spec;
            
            if (body != null) {
                requestSpec = requestSpec.body(body);
            }
            
            Response response = switch (method.toUpperCase()) {
                case "GET" -> requestSpec.get(path);
                case "POST" -> requestSpec.post(path);
                case "PUT" -> requestSpec.put(path);
                case "DELETE" -> requestSpec.delete(path);
                case "PATCH" -> requestSpec.patch(path);
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            };
            
            Duration duration = Duration.between(startTime, Instant.now());
            
            log.debug("Request completed: {} {} - Status: {} - Duration: {}ms", 
                    method, path, response.getStatusCode(), duration.toMillis());
            
            // Add request/response details to Allure
            Allure.addAttachment("Request Details", "text/plain", 
                    String.format("Method: %s\nPath: %s\nBody: %s\nDuration: %dms", 
                            method, path, body, duration.toMillis()));
            
            Allure.addAttachment("Response Details", "text/plain", 
                    String.format("Status: %d\nBody: %s", 
                            response.getStatusCode(), response.getBody().asString()));
            
            return response;
            
        } catch (Exception e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.error("Request failed: {} {} - Duration: {}ms - Error: {}", 
                    method, path, duration.toMillis(), e.getMessage());
            
            Allure.addAttachment("Request Failure", "text/plain", 
                    String.format("Method: %s\nPath: %s\nError: %s\nDuration: %dms", 
                            method, path, e.getMessage(), duration.toMillis()));
            
            throw e;
        }
    }

    /**
     * Reset REST Assured configuration.
     * <p>
     * Useful for test cleanup or when changing base configuration.
     * </p>
     */
    public static void reset() {
        RestAssured.reset();
        initialized = false;
        initializeRestAssured();
        log.info("REST Assured configuration reset");
    }

    /**
     * Get the current base URI.
     *
     * @return current base URI
     */
    public static String getBaseUri() {
        return config.getApiBaseUrl();
    }
}
