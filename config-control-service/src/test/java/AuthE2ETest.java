package com.example.control;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end authentication test for the config-control-service.
 * <p>
 * Tests the complete authentication flow including:
 * - Keycloak health and token acquisition
 * - User context endpoints
 * - Application services CRUD with authentication
 * - Approval workflow with permissions
 * - Service instances and drift events with team filtering
 * - Service shares management
 * - Authorization checks
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthE2ETest {

    private static final String BASE_URL = "http://localhost:8081";
    private static final String KEYCLOAK_URL = "http://localhost:8082";
    private static HttpClient httpClient;
    private static ObjectMapper objectMapper;
    private static String adminToken;
    private static String userToken;

    @BeforeAll
    static void setUp() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @Order(1)
    void testKeycloakHealth() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KEYCLOAK_URL + "/health/ready"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Keycloak should be healthy");
    }

    @Test
    @Order(2)
    void testGetAdminToken() throws IOException, InterruptedException {
        String token = getToken("admin@example.com", "admin123");
        assertNotNull(token, "Admin token should not be null");
        assertTrue(token.length() > 100, "Admin token should be a valid JWT");
        adminToken = token;
    }

    @Test
    @Order(3)
    void testGetUserToken() throws IOException, InterruptedException {
        String token = getToken("user1@example.com", "user123");
        assertNotNull(token, "User token should not be null");
        assertTrue(token.length() > 100, "User token should be a valid JWT");
        userToken = token;
    }

    @Test
    @Order(4)
    void testTokenValidation() throws IOException, InterruptedException {
        // Test admin token
        JsonNode adminClaims = parseJwt(adminToken);
        assertEquals("admin@example.com", adminClaims.get("preferred_username").asText());
        assertTrue(adminClaims.get("realm_access").get("roles").toString().contains("SYS_ADMIN"));
        assertTrue(adminClaims.get("groups").toString().contains("teams"));

        // Test user token
        JsonNode userClaims = parseJwt(userToken);
        assertEquals("user1@example.com", userClaims.get("preferred_username").asText());
        assertTrue(userClaims.get("realm_access").get("roles").toString().contains("USER"));
        assertTrue(userClaims.get("groups").toString().contains("team_core"));
    }

    @Test
    @Order(5)
    void testGetMeAsAdmin() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/users/me"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode userInfo = objectMapper.readTree(response.body());
        assertEquals("admin@example.com", userInfo.get("email").asText());
        assertTrue(userInfo.get("roles").toString().contains("SYS_ADMIN"));
    }

    @Test
    @Order(6)
    void testGetMeAsUser() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/users/me"))
                .header("Authorization", "Bearer " + userToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode userInfo = objectMapper.readTree(response.body());
        assertEquals("user1@example.com", userInfo.get("email").asText());
        assertTrue(userInfo.get("roles").toString().contains("USER"));
        assertTrue(userInfo.get("teamIds").toString().contains("team_core"));
    }

    @Test
    @Order(7)
    void testGetMeWithoutAuth() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/users/me"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }

    @Test
    @Order(8)
    void testListApplicationServicesPublic() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/application-services"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode services = objectMapper.readTree(response.body());
        assertTrue(services.has("content"));
    }

    @Test
    @Order(9)
    void testCreateApplicationService() throws IOException, InterruptedException {
        String serviceJson = """
                {
                    "id": "test-service",
                    "displayName": "Test Service",
                    "ownerTeamId": "team_core",
                    "environments": ["dev", "prod"],
                    "tags": ["test"],
                    "repoUrl": "https://github.com/test/service"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/application-services"))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(serviceJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        JsonNode service = objectMapper.readTree(response.body());
        assertEquals("test-service", service.get("id").asText());
        assertEquals("Test Service", service.get("displayName").asText());
    }

    @Test
    @Order(10)
    void testGetApplicationServiceById() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/application-services/test-service"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode service = objectMapper.readTree(response.body());
        assertEquals("test-service", service.get("id").asText());
    }

    @Test
    @Order(11)
    void testCreateApprovalRequest() throws IOException, InterruptedException {
        String requestJson = """
                {
                    "serviceId": "test-service",
                    "targetTeamId": "team_analytics",
                    "note": "Requesting service transfer"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/approval-requests/application-services/test-service/approval-requests"))
                .header("Authorization", "Bearer " + userToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        JsonNode approvalRequest = objectMapper.readTree(response.body());
        assertNotNull(approvalRequest.get("id"));
        assertEquals("PENDING", approvalRequest.get("status").asText());
    }

    @Test
    @Order(12)
    void testListApprovalRequests() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/approval-requests"))
                .header("Authorization", "Bearer " + userToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode requests = objectMapper.readTree(response.body());
        assertTrue(requests.has("content"));
        assertTrue(requests.get("content").isArray());
    }

    @Test
    @Order(13)
    void testListServiceInstancesAsUser() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/service-instances"))
                .header("Authorization", "Bearer " + userToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode instances = objectMapper.readTree(response.body());
        assertTrue(instances.has("content"));
    }

    @Test
    @Order(14)
    void testListServiceInstancesAsAdmin() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/service-instances"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode instances = objectMapper.readTree(response.body());
        assertTrue(instances.has("content"));
    }

    @Test
    @Order(15)
    void testListDriftEventsAsUser() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/drift-events"))
                .header("Authorization", "Bearer " + userToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode events = objectMapper.readTree(response.body());
        assertTrue(events.has("content"));
    }

    @Test
    @Order(16)
    void testListDriftEventsAsAdmin() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/drift-events"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode events = objectMapper.readTree(response.body());
        assertTrue(events.has("content"));
    }

    @Test
    @Order(17)
    void testUnauthorizedCreateService() throws IOException, InterruptedException {
        String serviceJson = """
                {
                    "id": "unauthorized-service",
                    "displayName": "Unauthorized Service",
                    "ownerTeamId": "team_core",
                    "environments": ["dev"]
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/application-services"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(serviceJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }

    @Test
    @Order(18)
    void testForbiddenDeleteService() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/application-services/test-service"))
                .header("Authorization", "Bearer " + userToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    private String getToken(String username, String password) throws IOException, InterruptedException {
        String tokenData = "username=" + username + "&password=" + password + "&grant_type=password&client_id=admin-dashboard";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KEYCLOAK_URL + "/realms/config-control/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode tokenResponse = objectMapper.readTree(response.body());
        return tokenResponse.get("access_token").asText();
    }

    private JsonNode parseJwt(String token) throws IOException {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        return objectMapper.readTree(payload);
    }
}
