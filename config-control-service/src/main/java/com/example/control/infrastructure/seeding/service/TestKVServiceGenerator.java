package com.example.control.infrastructure.seeding.service;

import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.model.kv.KVType;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.infrastructure.seeding.factory.ApplicationServiceFactory;
import com.example.control.infrastructure.seeding.factory.KVEntryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generator for dedicated test-kv-service with primitive test data.
 * <p>
 * Creates a special orphan service "test-kv-service" with structured
 * primitive test data under "test-primitives" prefix for KVApi testing.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestKVServiceGenerator {

    private static final String TEST_SERVICE_ID = "test-kv-service";
    private static final String TEST_SERVICE_DISPLAY_NAME = "Test KV Service";
    private static final String TEST_PRIMITIVES_PREFIX = "test-primitives";

    private final ApplicationServiceFactory applicationServiceFactory;
    private final KVEntryFactory kvEntryFactory;

    /**
     * Generates the test-kv-service ApplicationService.
     *
     * @param createdBy user ID who created the service
     * @return test-kv-service
     */
    public ApplicationService generateTestService(String createdBy) {
        log.info("Generating test-kv-service for KVApi testing");

        // Use a high index to avoid conflicts with regular services
        ApplicationService service = applicationServiceFactory.generate(
                9999, // High index to ensure uniqueness
                null, // Orphan service
                createdBy
        );

        // Override with test service specific values
        return ApplicationService.builder()
                .id(ApplicationServiceId.of(TEST_SERVICE_ID))
                .displayName(TEST_SERVICE_DISPLAY_NAME)
                .ownerTeamId(null) // Orphan
                .environments(List.of("dev", "staging", "prod"))
                .tags(List.of("test", "kv-api", "primitives"))
                .repoUrl("https://github.com/test/test-kv-service")
                .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                .createdAt(Instant.now().minusSeconds(86400)) // 1 day ago
                .updatedAt(Instant.now().minusSeconds(3600)) // 1 hour ago
                .createdBy(createdBy)
                .attributes(Map.of(
                        "framework", "spring-boot",
                        "language", "java-21",
                        "purpose", "kv-api-testing"
                ))
                .build();
    }

    /**
     * Generates primitive test KV entries for test-kv-service.
     * <p>
     * Creates entries under "test-primitives" prefix:
     * - string-value (String)
     * - integer-value (Integer)
     * - long-value (Long)
     * - boolean-value (Boolean)
     * - double-value (Double)
     * - list-value (comma-separated list)
     * - map-value (structured map as JSON string)
     * - leaf-list-value (LEAF_LIST type)
     * </p>
     *
     * @return list of KV entries
     */
    public List<KVEntry> generatePrimitiveTestEntries() {
        log.info("Generating primitive test KV entries for test-kv-service");

        List<KVEntry> entries = new ArrayList<>();

        // String value
        String stringValue = kvEntryFactory.generateStringValue();
        entries.add(createTestEntry("string-value", stringValue, false));

        // Integer value
        String integerValue = kvEntryFactory.generateIntegerValue();
        entries.add(createTestEntry("integer-value", integerValue, false));

        // Long value
        String longValue = kvEntryFactory.generateLongValue();
        entries.add(createTestEntry("long-value", longValue, false));

        // Boolean value
        String booleanValue = kvEntryFactory.generateBooleanValue();
        entries.add(createTestEntry("boolean-value", booleanValue, false));

        // Double value
        String doubleValue = kvEntryFactory.generateDoubleValue();
        entries.add(createTestEntry("double-value", doubleValue, false));

        // List value (comma-separated)
        String listValue = kvEntryFactory.generateListValue();
        entries.add(createTestEntry("list-value", listValue, false));

        // Map value (JSON string)
        String mapValue = kvEntryFactory.generateMapValue();
        entries.add(createTestEntry("map-value", mapValue, false));

        // LEAF_LIST value
        String leafListValue = String.join(",",
                kvEntryFactory.generateStringValue(),
                kvEntryFactory.generateStringValue(),
                kvEntryFactory.generateStringValue(),
                kvEntryFactory.generateStringValue(),
                kvEntryFactory.generateStringValue()
        );
        entries.add(createTestEntry("leaf-list-value", leafListValue, true));

        log.info("Generated {} primitive test KV entries", entries.size());
        return entries;
    }

    /**
     * Creates a test KV entry under test-primitives prefix.
     * <p>
     * Uses "config" category to match the pattern used by other entries.
     * </p>
     *
     * @param key   relative key (e.g., "string-value")
     * @param value value as string
     * @param isLeafList whether this is a LEAF_LIST entry
     * @return KV entry
     */
    private KVEntry createTestEntry(String key, String value, boolean isLeafList) {
        // Use "config" category with test-primitives prefix
        String relativePath = TEST_PRIMITIVES_PREFIX + "/" + key;
        String absoluteKey = String.format("apps/%s/kv/config/%s", TEST_SERVICE_ID, relativePath);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        long flags = isLeafList ? KVType.LEAF_LIST.getFlagValue() : KVType.LEAF.getFlagValue();

        return KVEntry.builder()
                .key(absoluteKey)
                .value(valueBytes)
                .modifyIndex(0)
                .createIndex(0)
                .flags(flags)
                .lockIndex(0)
                .session(null)
                .build();
    }
}

