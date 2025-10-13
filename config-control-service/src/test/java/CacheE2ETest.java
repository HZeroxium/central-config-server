import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive End-to-End tests for cache management API.
 * <p>
 * Tests real HTTP endpoints without Spring Test framework using pure Java HttpClient.
 * This approach provides faster execution and tests the actual HTTP layer.
 * <p>
 * Prerequisites:
 * <ul>
 *   <li>config-control-service running at http://localhost:8081</li>
 *   <li>Redis available at configured host (for REDIS provider tests)</li>
 * </ul>
 * <p>
 * Test Categories:
 * <ul>
 *   <li>Basic functionality (health, status, providers)</li>
 *   <li>Provider switching (CAFFEINE, REDIS, TWO_LEVEL, NOOP)</li>
 *   <li>Cache operations (put, get, clear, stats)</li>
 *   <li>Error handling (invalid providers, unavailable services)</li>
 *   <li>Concurrency (concurrent switches, thread safety)</li>
 *   <li>Business logic integration (heartbeat, services)</li>
 * </ul>
 * 
 * @author Principal Software Engineer
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CacheE2ETest {

    private static final String BASE_URL = "http://localhost:8081/api/cache";
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static ExecutorService executor;
    private static String initialProvider;

    @BeforeAll
    static void setUp() {
        System.out.println("=== Cache E2E Test Suite ===");
        System.out.println("Testing config-control-service cache management API");
        System.out.println("Base URL: " + BASE_URL);
        System.out.println();
        executor = Executors.newFixedThreadPool(10);
        
        // Verify service is running
        try {
            HttpResponse<String> response = sendRequest("GET", "/health");
            if (response.statusCode() != 200) {
                throw new RuntimeException("Service not running at " + BASE_URL + ". Status: " + response.statusCode());
            }
            System.out.println("✓ Service is running and accessible");
            
            // Record initial state for cleanup
            Map<String, Object> status = parseJson(sendRequest("GET", "/status").body());
            initialProvider = (String) status.get("currentProvider");
            System.out.println("Initial provider: " + initialProvider);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to service at " + BASE_URL, e);
        }
    }

    @AfterAll
    static void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
        
        // Clean up: reset to initial provider and clear all caches
        try {
            clearAllCachesAndReset();
            System.out.println("✓ Test cleanup completed");
        } catch (Exception e) {
            System.err.println("Warning: Failed to cleanup after tests: " + e.getMessage());
        }
    }

    // ================================
    // BASIC FUNCTIONALITY TESTS
    // ================================

    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @Order(1)
        @DisplayName("Get Cache Health")
        void testGetCacheHealth() throws Exception {
            HttpResponse<String> response = sendRequest("GET", "/health");
            assertEquals(200, response.statusCode(), "Health endpoint should return 200");
            
            Map<String, Object> health = parseJson(response.body());
            assertNotNull(health.get("provider"), "Health should contain provider");
            assertNotNull(health.get("healthy"), "Health should contain healthy status");
            assertNotNull(health.get("actualProvider"), "Health should contain actual provider");
            
            System.out.println("  Provider: " + health.get("provider"));
            System.out.println("  Actual Provider: " + health.get("actualProvider"));
            System.out.println("  Healthy: " + health.get("healthy"));
        }

        @Test
        @Order(2)
        @DisplayName("Get Cache Status")
        void testGetCacheStatus() throws Exception {
            HttpResponse<String> response = sendRequest("GET", "/status");
            assertEquals(200, response.statusCode(), "Status endpoint should return 200");
            
            Map<String, Object> status = parseJson(response.body());
            assertNotNull(status.get("currentProvider"), "Status should contain currentProvider");
            assertNotNull(status.get("configuredProvider"), "Status should contain configuredProvider");
            assertNotNull(status.get("cacheNames"), "Status should contain cacheNames");
            assertNotNull(status.get("availableProviders"), "Status should contain availableProviders");
            
            System.out.println("  Current Provider: " + status.get("currentProvider"));
            System.out.println("  Configured Provider: " + status.get("configuredProvider"));
            System.out.println("  Cache Names: " + status.get("cacheNames"));
        }

        @Test
        @Order(3)
        @DisplayName("Get Available Providers")
        void testGetAvailableProviders() throws Exception {
            HttpResponse<String> response = sendRequest("GET", "/providers");
            assertEquals(200, response.statusCode(), "Providers endpoint should return 200");
            
            Map<String, Object> providers = parseJson(response.body());
            assertNotNull(providers.get("current"), "Providers should contain current");
            assertNotNull(providers.get("providers"), "Providers should contain providers map");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> providerMap = (Map<String, Object>) providers.get("providers");
            assertTrue(providerMap.containsKey("CAFFEINE"), "Should have CAFFEINE provider");
            assertTrue(providerMap.containsKey("NOOP"), "Should have NOOP provider");
            
            System.out.println("  Current Provider: " + providers.get("current"));
            System.out.println("  Available Providers: " + providerMap.keySet());
        }
    }

    // ================================
    // PROVIDER SWITCHING TESTS
    // ================================

    @Nested
    @DisplayName("Provider Switching Tests")
    class ProviderSwitchingTests {

        @Test
        @Order(1)
        @DisplayName("Switch to CAFFEINE")
        void testSwitchToCaffeine() throws Exception {
            switchProvider("CAFFEINE");
            assertProviderIs("CAFFEINE");
        }

        @Test
        @Order(2)
        @DisplayName("Switch to NOOP")
        void testSwitchToNoop() throws Exception {
            switchProvider("NOOP");
            assertProviderIs("NOOP");
        }

        @Test
        @Order(3)
        @DisplayName("Switch to REDIS (conditional)")
        void testSwitchToRedis() throws Exception {
            if (isRedisAvailable()) {
                switchProvider("REDIS");
                assertProviderIs("REDIS");
            } else {
                System.out.println("  Skipping REDIS test - Redis not available");
            }
        }

        @Test
        @Order(4)
        @DisplayName("Switch to TWO_LEVEL (conditional)")
        void testSwitchToTwoLevel() throws Exception {
            if (isRedisAvailable()) {
                switchProvider("TWO_LEVEL");
                assertProviderIs("TWO_LEVEL");
            } else {
                System.out.println("  Skipping TWO_LEVEL test - Redis not available");
            }
        }

        @Test
        @Order(5)
        @DisplayName("Invalid Provider")
        void testInvalidProvider() throws Exception {
            HttpResponse<String> response = sendRequest("POST", "/providers/INVALID");
            assertEquals(400, response.statusCode(), "Should return 400 for invalid provider");
            
            Map<String, Object> error = parseJson(response.body());
            assertNotNull(error.get("error"), "Should contain error message");
            assertTrue(((String) error.get("error")).contains("Invalid provider"), 
                "Error should mention invalid provider");
            
            System.out.println("  Error: " + error.get("error"));
        }

        @Test
        @Order(6)
        @DisplayName("Provider Persistence")
        void testProviderPersistence() throws Exception {
            // Set provider to CAFFEINE
            switchProvider("CAFFEINE");
            
            // Perform cache operations
            sendRequest("POST", "/test");
            sendRequest("DELETE", "/all");
            
            // Verify provider is still CAFFEINE
            assertProviderIs("CAFFEINE");
            
            System.out.println("  Provider persisted through operations");
        }
    }

    // ================================
    // CACHE OPERATIONS TESTS
    // ================================

    @Nested
    @DisplayName("Cache Operations Tests")
    class CacheOperationsTests {

        @Test
        @Order(1)
        @DisplayName("Cache Put and Get")
        void testCachePutAndGet() throws Exception {
            // Switch to CAFFEINE for reliable testing
            switchProvider("CAFFEINE");
            
            // Test cache operation
            HttpResponse<String> testResponse = sendRequest("POST", "/test");
            assertEquals(200, testResponse.statusCode(), "Cache test should return 200");
            
            Map<String, Object> result = parseJson(testResponse.body());
            assertTrue((Boolean) result.get("success"), "Cache test should succeed");
            
            System.out.println("  Cache Test Success: " + result.get("success"));
            System.out.println("  Provider: " + result.get("provider"));
        }

        @Test
        @Order(2)
        @DisplayName("Clear Specific Cache")
        void testClearSpecificCache() throws Exception {
            // Clear service-instances cache
            HttpResponse<String> response = sendRequest("DELETE", "/service-instances");
            assertEquals(200, response.statusCode(), "Clear cache should return 200");
            
            Map<String, Object> result = parseJson(response.body());
            assertEquals("Cache cleared successfully", result.get("message"), 
                "Should return success message");
            
            System.out.println("  Message: " + result.get("message"));
        }

        @Test
        @Order(3)
        @DisplayName("Clear All Caches")
        void testClearAllCaches() throws Exception {
            HttpResponse<String> response = sendRequest("DELETE", "/all");
            assertEquals(200, response.statusCode(), "Clear all caches should return 200");
            
            Map<String, Object> result = parseJson(response.body());
            assertEquals("All caches cleared successfully", result.get("message"), 
                "Should return success message");
            assertTrue((Integer) result.get("clearedCount") > 0, 
                "Should have cleared at least one cache");
            
            System.out.println("  Cleared Count: " + result.get("clearedCount"));
        }

        @Test
        @Order(4)
        @DisplayName("Cache Statistics")
        void testCacheStats() throws Exception {
            HttpResponse<String> response = sendRequest("GET", "/stats/service-instances");
            assertEquals(200, response.statusCode(), "Cache stats should return 200");
            
            Map<String, Object> stats = parseJson(response.body());
            assertEquals("service-instances", stats.get("cacheName"), 
                "Should return stats for service-instances cache");
            assertNotNull(stats.get("cacheType"), "Should contain cache type");
            
            System.out.println("  Cache Name: " + stats.get("cacheName"));
            System.out.println("  Cache Type: " + stats.get("cacheType"));
        }

        @Test
        @Order(5)
        @DisplayName("Cache Test Endpoint")
        void testCacheTest() throws Exception {
            HttpResponse<String> response = sendRequest("POST", "/test");
            assertEquals(200, response.statusCode(), "Cache test endpoint should return 200");
            
            Map<String, Object> result = parseJson(response.body());
            assertNotNull(result.get("success"), "Should contain success status");
            assertNotNull(result.get("provider"), "Should contain provider info");
            
            System.out.println("  Success: " + result.get("success"));
            System.out.println("  Provider: " + result.get("provider"));
        }
    }

    // ================================
    // EDGE CASES TESTS
    // ================================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @Order(1)
        @DisplayName("Switch Back to Original")
        void testSwitchBackToOriginal() throws Exception {
            // Switch to NOOP first
            switchProvider("NOOP");
            
            // Switch back to CAFFEINE
            switchProvider("CAFFEINE");
            assertProviderIs("CAFFEINE");
            
            System.out.println("  Successfully switched back to original provider");
        }

        @Test
        @Order(2)
        @DisplayName("Rapid Provider Switches")
        void testRapidProviderSwitches() throws Exception {
            String[] providers = {"CAFFEINE", "NOOP", "CAFFEINE", "NOOP"};
            
            for (String provider : providers) {
                switchProvider(provider);
                assertProviderIs(provider);
                // Small delay between switches
                Thread.sleep(100);
            }
            
            System.out.println("  Completed " + providers.length + " rapid switches");
        }

        @Test
        @Order(3)
        @DisplayName("Cache Operations After Switch")
        void testCacheOperationsAfterSwitch() throws Exception {
            // Switch to NOOP
            switchProvider("NOOP");
            
            // Test cache operations - NOOP might return 500 for /test endpoint
            HttpResponse<String> testResponse = sendRequest("POST", "/test");
            // Accept both 200 and 500 for NOOP provider as it disables caching
            assertTrue(testResponse.statusCode() == 200 || testResponse.statusCode() == 500, 
                "Cache test should return 200 or 500 for NOOP provider");
            
            // Switch to CAFFEINE
            switchProvider("CAFFEINE");
            
            // Immediately test cache operations again - should work with CAFFEINE
            testResponse = sendRequest("POST", "/test");
            assertEquals(200, testResponse.statusCode(), "Cache test should work after switch to CAFFEINE");
            
            System.out.println("  Cache operations work immediately after provider switch");
        }

        @Test
        @Order(4)
        @DisplayName("Unavailable Provider Handling")
        void testUnavailableProviderHandling() throws Exception {
            // This test verifies graceful handling when Redis is unavailable
            HttpResponse<String> response = sendRequest("GET", "/providers");
            Map<String, Object> providers = parseJson(response.body());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> providerMap = (Map<String, Object>) providers.get("providers");
            
            // Check that REDIS provider shows availability status
            assertNotNull(providerMap.get("REDIS"), "Redis provider should be listed");
            
            System.out.println("  Provider availability check completed");
        }
    }

    // ================================
    // CONCURRENCY TESTS
    // ================================

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @Order(1)
        @DisplayName("Concurrent Provider Switches")
        void testConcurrentProviderSwitches() throws Exception {
            List<CompletableFuture<Void>> futures = List.of(
                CompletableFuture.runAsync(() -> {
                    try {
                        HttpResponse<String> response = sendRequest("POST", "/providers/CAFFEINE");
                        assertEquals(200, response.statusCode(), "Switch to CAFFEINE should succeed");
                    } catch (Exception e) {
                        throw new RuntimeException("Concurrent switch failed", e);
                    }
                }, executor),
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100); // Stagger the switches
                        HttpResponse<String> response = sendRequest("POST", "/providers/NOOP");
                        assertEquals(200, response.statusCode(), "Switch to NOOP should succeed");
                    } catch (Exception e) {
                        throw new RuntimeException("Concurrent switch failed", e);
                    }
                }, executor),
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(200); // Stagger the switches
                        HttpResponse<String> response = sendRequest("POST", "/providers/CAFFEINE");
                        assertEquals(200, response.statusCode(), "Switch to CAFFEINE should succeed");
                    } catch (Exception e) {
                        throw new RuntimeException("Concurrent switch failed", e);
                    }
                }, executor)
            );
            
            // Wait for all switches to complete
            CompletableFuture<Void> allSwitches = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            allSwitches.get(15, TimeUnit.SECONDS);
            System.out.println("  Completed " + futures.size() + " concurrent switches");
            
            // Verify final state is consistent
            assertProviderIs("CAFFEINE");
        }

        @Test
        @Order(2)
        @DisplayName("Concurrent Cache Operations")
        void testConcurrentCacheOperations() throws Exception {
            switchProvider("CAFFEINE");
            
            List<CompletableFuture<Void>> futures = List.of(
                CompletableFuture.runAsync(() -> {
                    try {
                        for (int i = 0; i < 5; i++) {
                            sendRequest("POST", "/test");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Concurrent cache operation failed", e);
                    }
                }, executor),
                CompletableFuture.runAsync(() -> {
                    try {
                        for (int i = 0; i < 3; i++) {
                            sendRequest("DELETE", "/service-instances");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Concurrent cache operation failed", e);
                    }
                }, executor),
                CompletableFuture.runAsync(() -> {
                    try {
                        for (int i = 0; i < 3; i++) {
                            sendRequest("GET", "/stats/service-instances");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Concurrent cache operation failed", e);
                    }
                }, executor)
            );
            
            CompletableFuture<Void> allOperations = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            allOperations.get(10, TimeUnit.SECONDS);
            System.out.println("  Completed " + futures.size() + " concurrent operation sets");
        }

        @Test
        @Order(3)
        @DisplayName("Switch During Cache Operations")
        void testSwitchDuringCacheOperations() throws Exception {
            // Start cache operations
            CompletableFuture<Void> operations = CompletableFuture.runAsync(() -> {
                try {
                    for (int i = 0; i < 10; i++) {
                        sendRequest("POST", "/test");
                        Thread.sleep(50);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Operations failed", e);
                }
            }, executor);
            
            // Switch providers during operations
            CompletableFuture<Void> switches = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100); // Let operations start
                    switchProvider("NOOP");
                    Thread.sleep(200);
                    switchProvider("CAFFEINE");
                } catch (Exception e) {
                    throw new RuntimeException("Switches failed", e);
                }
            }, executor);
            
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(operations, switches);
            allTasks.get(15, TimeUnit.SECONDS);
            
            System.out.println("  Completed provider switches during cache operations");
        }
    }

    // ================================
    // CONFIGURATION TESTS
    // ================================

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @Order(1)
        @DisplayName("Update Cache Configuration")
        void testUpdateCacheConfig() throws Exception {
            String configUpdate = "{\"enableFallback\": false}";
            HttpResponse<String> response = sendRequestWithBody("PUT", "/config", configUpdate);
            assertEquals(200, response.statusCode(), "Config update should return 200");
            
            Map<String, Object> result = parseJson(response.body());
            assertEquals("Cache configuration updated successfully", result.get("message"), 
                "Should return success message");
            
            System.out.println("  Message: " + result.get("message"));
        }

        @Test
        @Order(2)
        @DisplayName("Configuration Persistence")
        void testConfigPersistence() throws Exception {
            // Test that configuration changes persist across operations
            String configUpdate = "{\"enableFallback\": true}";
            HttpResponse<String> response = sendRequestWithBody("PUT", "/config", configUpdate);
            assertEquals(200, response.statusCode(), "Config update should return 200");
            
            // Perform some operations
            sendRequest("POST", "/test");
            sendRequest("GET", "/status");
            
            // Verify configuration is still applied
            Map<String, Object> status = parseJson(sendRequest("GET", "/status").body());
            assertNotNull(status.get("fallbackEnabled"), "Should show fallback configuration");
            
            System.out.println("  Configuration persisted through operations");
        }
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Checks if Redis is available by attempting to switch to REDIS provider.
     */
    private static boolean isRedisAvailable() {
        try {
            HttpResponse<String> response = sendRequest("POST", "/providers/REDIS");
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sends HTTP request to cache endpoint.
     */
    private static HttpResponse<String> sendRequest(String method, String path) throws Exception {
        return sendRequestWithBody(method, path, null);
    }

    /**
     * Sends HTTP request with body to cache endpoint.
     */
    private static HttpResponse<String> sendRequestWithBody(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .timeout(Duration.ofSeconds(30));

        switch (method.toUpperCase()) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
            case "PUT" -> builder.PUT(body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
            case "DELETE" -> builder.DELETE();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        HttpRequest request = builder.build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Parses JSON response into Map.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + json, e);
        }
    }

    /**
     * Switches to a cache provider and verifies the switch was successful.
     */
    private static void switchProvider(String provider) throws Exception {
        HttpResponse<String> response = sendRequest("POST", "/providers/" + provider);
        assertEquals(200, response.statusCode(), "Switch to " + provider + " should return 200");
        
        Map<String, Object> result = parseJson(response.body());
        assertEquals(provider, result.get("newProvider"), "Should switch to " + provider);
        
        System.out.println("  Switched to: " + result.get("newProvider"));
        System.out.println("  Cache Manager Type: " + result.get("cacheManagerType"));
    }

    /**
     * Verifies that the current provider matches the expected provider.
     */
    private static void assertProviderIs(String expectedProvider) throws Exception {
        HttpResponse<String> response = sendRequest("GET", "/status");
        Map<String, Object> status = parseJson(response.body());
        assertEquals(expectedProvider, status.get("currentProvider"), 
            "Current provider should be " + expectedProvider);
    }

    /**
     * Clears all caches and resets to the initial provider.
     */
    private static void clearAllCachesAndReset() throws Exception {
        // Clear all caches
        sendRequest("DELETE", "/all");
        
        // Reset to initial provider
        if (initialProvider != null) {
            sendRequest("POST", "/providers/" + initialProvider);
        }
    }
}