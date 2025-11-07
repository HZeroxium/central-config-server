package com.example.control.benchmark.kv;

import com.example.control.application.command.ApplicationServiceCommandService;
import com.example.control.application.service.KVService;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import com.example.control.infrastructure.config.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Base class for KV benchmark tests.
 * <p>
 * Provides Spring Boot test context with KVService and KVStorePort injection.
 * Sets up test ApplicationService and test user context for benchmarks.
 * </p>
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseKVBenchmarkTest {

    @Autowired
    protected KVService kvService;

    @Autowired
    protected KVStorePort kvStorePort;

    @Autowired
    protected ApplicationServiceCommandService applicationServiceCommandService;

    @Autowired
    protected PrefixPolicy prefixPolicy;

    protected BenchmarkConfig config;
    protected HierarchicalDatasetGenerator datasetGenerator;
    protected KVBenchmarkRunner benchmarkRunner;
    protected BenchmarkDataCleanup cleanup;
    protected ApplicationService testService;
    protected UserContext testUserContext;

    @BeforeEach
    void setUp() {
        // Load configuration from system properties or use defaults
        config = BenchmarkConfig.fromSystemProperties();

        // Create dataset generator
        datasetGenerator = new HierarchicalDatasetGenerator(config);

        // Create benchmark runner
        benchmarkRunner = new KVBenchmarkRunner(config, datasetGenerator, prefixPolicy);

        // Create cleanup utility
        cleanup = new BenchmarkDataCleanup(config, prefixPolicy);

        // Create test ApplicationService
        testService = createTestApplicationService(config.serviceId());

        // Create test user context (SYS_ADMIN for full access)
        testUserContext = createTestUserContext();

        log.info("Benchmark test setup completed for service: {}", config.serviceId());
    }

    @AfterEach
    void tearDown() {
        // Cleanup test data
        try {
            Set<String> generatedKeys = datasetGenerator.getGeneratedKeys();
            if (!generatedKeys.isEmpty()) {
                cleanup.cleanupService(kvService, testUserContext, generatedKeys);
                cleanup.cleanupPort(kvStorePort, generatedKeys);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup test data: {}", e.getMessage());
        }

        // Cleanup test ApplicationService
        try {
            if (testService != null) {
                applicationServiceCommandService.deleteById(testService.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup test ApplicationService: {}", e.getMessage());
        }

        log.info("Benchmark test teardown completed");
    }

    /**
     * Create test ApplicationService.
     */
    private ApplicationService createTestApplicationService(String serviceId) {
        ApplicationService service = ApplicationService.builder()
                .id(ApplicationServiceId.of(serviceId))
                .displayName("Benchmark Test Service")
                .ownerTeamId("benchmark-team")
                .environments(List.of("dev", "staging", "prod"))
                .tags(List.of("benchmark", "test"))
                .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("benchmark-user")
                .build();

        return applicationServiceCommandService.save(service);
    }

    /**
     * Create test user context with SYS_ADMIN role.
     */
    private UserContext createTestUserContext() {
        return UserContext.builder()
                .userId("benchmark-user-id")
                .username("benchmark-user")
                .email("benchmark@example.com")
                .teamIds(List.of("benchmark-team"))
                .roles(List.of("SYS_ADMIN", "USER"))
                .build();
    }
}

