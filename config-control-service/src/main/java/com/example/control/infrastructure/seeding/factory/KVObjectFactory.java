package com.example.control.infrastructure.seeding.factory;

import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.model.kv.KVType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Factory for generating realistic object KV entries.
 * <p>
 * Generates nested configuration objects that are flattened into hierarchical
 * keys in Consul KV store. Objects are stored with KVType.OBJECT flag.
 * </p>
 *
 * <p>
 * <strong>Generation Strategy:</strong>
 * </p>
 * <ul>
 * <li>Config: Database config, API config, logging config (1-3 levels nesting)</li>
 * <li>Secrets: OAuth credentials, SSL certificates metadata</li>
 * <li>Feature Flags: Feature flag groups, A/B test configs</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KVObjectFactory {

    /**
     * Object templates for config category.
     */
    private static final List<String> CONFIG_OBJECT_PREFIXES = List.of(
            "database", "api", "cache", "logging", "messaging", "monitoring"
    );

    /**
     * Object templates for secrets category.
     */
    private static final List<String> SECRETS_OBJECT_PREFIXES = List.of(
            "oauth", "ssl", "aws", "github", "docker"
    );

    /**
     * Object templates for feature flags category.
     */
    private static final List<String> FEATURE_FLAG_OBJECT_PREFIXES = List.of(
            "experiments", "rollouts", "ab-tests", "feature-groups"
    );

    private final Faker faker;

    /**
     * Generates an object KV entry for config category.
     *
     * @param serviceId service ID
     * @param prefix    object prefix (e.g., "database")
     * @return list of KV entries representing the flattened object
     */
    public List<KVEntry> generateConfigObject(String serviceId, String prefix) {
        Map<String, Object> objectData = generateConfigObjectData(prefix);
        return flattenObject(serviceId, "config", prefix, objectData);
    }

    /**
     * Generates an object KV entry for secrets category.
     *
     * @param serviceId service ID
     * @param prefix    object prefix (e.g., "oauth")
     * @return list of KV entries representing the flattened object
     */
    public List<KVEntry> generateSecretObject(String serviceId, String prefix) {
        Map<String, Object> objectData = generateSecretObjectData(prefix);
        return flattenObject(serviceId, "secrets", prefix, objectData);
    }

    /**
     * Generates an object KV entry for feature flags category.
     *
     * @param serviceId service ID
     * @param prefix    object prefix (e.g., "experiments")
     * @return list of KV entries representing the flattened object
     */
    public List<KVEntry> generateFeatureFlagObject(String serviceId, String prefix) {
        Map<String, Object> objectData = generateFeatureFlagObjectData(prefix);
        return flattenObject(serviceId, "feature-flags", prefix, objectData);
    }

    /**
     * Generates a random config object prefix.
     *
     * @return object prefix
     */
    public String generateConfigObjectPrefix() {
        return CONFIG_OBJECT_PREFIXES.get(faker.random().nextInt(CONFIG_OBJECT_PREFIXES.size()));
    }

    /**
     * Generates a random secret object prefix.
     *
     * @return object prefix
     */
    public String generateSecretObjectPrefix() {
        return SECRETS_OBJECT_PREFIXES.get(faker.random().nextInt(SECRETS_OBJECT_PREFIXES.size()));
    }

    /**
     * Generates a random feature flag object prefix.
     *
     * @return object prefix
     */
    public String generateFeatureFlagObjectPrefix() {
        return FEATURE_FLAG_OBJECT_PREFIXES.get(faker.random().nextInt(FEATURE_FLAG_OBJECT_PREFIXES.size()));
    }

    /**
     * Generates config object data.
     *
     * @param prefix object prefix
     * @return object data map
     */
    private Map<String, Object> generateConfigObjectData(String prefix) {
        Map<String, Object> data = new LinkedHashMap<>();

        if ("database".equals(prefix)) {
            data.put("host", faker.internet().domainName());
            data.put("port", faker.number().numberBetween(5432, 65535));
            data.put("name", faker.lorem().word());
            data.put("username", faker.lorem().word());
            data.put("pool", Map.of(
                    "minSize", faker.number().numberBetween(1, 5),
                    "maxSize", faker.number().numberBetween(10, 50),
                    "timeout", faker.number().numberBetween(5000, 30000)
            ));
        } else if ("api".equals(prefix)) {
            data.put("endpoint", String.format("https://api.%s.com", faker.internet().domainName()));
            data.put("timeout", faker.number().numberBetween(1000, 30000));
            data.put("retry", Map.of(
                    "count", faker.number().numberBetween(1, 5),
                    "delay", faker.number().numberBetween(100, 1000)
            ));
            data.put("version", String.format("v%d", faker.number().numberBetween(1, 3)));
        } else if ("cache".equals(prefix)) {
            data.put("ttl", faker.number().numberBetween(60, 3600));
            data.put("size", faker.number().numberBetween(100, 10000));
            data.put("evictionPolicy", faker.options().option("LRU", "LFU", "FIFO"));
        } else if ("logging".equals(prefix)) {
            data.put("level", faker.options().option("DEBUG", "INFO", "WARN", "ERROR"));
            data.put("format", faker.options().option("JSON", "TEXT"));
            data.put("file", Map.of(
                    "path", "/var/log/" + faker.lorem().word() + ".log",
                    "maxSize", faker.number().numberBetween(10, 100) + "MB"
            ));
        } else if ("messaging".equals(prefix)) {
            data.put("broker", String.format("kafka://%s:%d",
                    faker.internet().domainName(),
                    faker.number().numberBetween(9092, 9093)));
            data.put("topic", faker.lorem().word());
            data.put("batchSize", faker.number().numberBetween(10, 1000));
        } else {
            // Generic config object
            data.put("enabled", faker.random().nextBoolean());
            data.put("timeout", faker.number().numberBetween(1000, 30000));
            data.put("retries", faker.number().numberBetween(1, 5));
        }

        return data;
    }

    /**
     * Generates secret object data.
     *
     * @param prefix object prefix
     * @return object data map
     */
    private Map<String, Object> generateSecretObjectData(String prefix) {
        Map<String, Object> data = new LinkedHashMap<>();

        if ("oauth".equals(prefix)) {
            data.put("clientId", faker.internet().uuid());
            data.put("clientSecret", faker.internet().uuid().replace("-", "") +
                    faker.internet().uuid().replace("-", ""));
            data.put("tokenUrl", String.format("https://auth.%s.com/oauth/token",
                    faker.internet().domainName()));
            data.put("scope", faker.lorem().word());
        } else if ("ssl".equals(prefix)) {
            data.put("certificate", faker.internet().uuid().replace("-", "") +
                    faker.internet().uuid().replace("-", ""));
            data.put("privateKey", faker.internet().uuid().replace("-", "") +
                    faker.internet().uuid().replace("-", ""));
            data.put("caBundle", faker.internet().uuid().replace("-", ""));
        } else if ("aws".equals(prefix)) {
            data.put("accessKeyId", faker.internet().uuid().replace("-", ""));
            data.put("secretAccessKey", faker.internet().uuid().replace("-", "") +
                    faker.internet().uuid().replace("-", ""));
            data.put("region", faker.options().option("us-east-1", "us-west-2", "eu-west-1"));
        } else {
            // Generic secret object
            data.put("key", faker.internet().uuid().replace("-", "") +
                    faker.internet().uuid().replace("-", ""));
            data.put("secret", faker.internet().uuid().replace("-", "") +
                    faker.internet().uuid().replace("-", ""));
        }

        return data;
    }

    /**
     * Generates feature flag object data.
     *
     * @param prefix object prefix
     * @return object data map
     */
    private Map<String, Object> generateFeatureFlagObjectData(String prefix) {
        Map<String, Object> data = new LinkedHashMap<>();

        if ("experiments".equals(prefix) || "ab-tests".equals(prefix)) {
            data.put("enabled", faker.random().nextBoolean());
            data.put("variant", faker.options().option("A", "B", "control"));
            data.put("percentage", faker.number().numberBetween(0, 101));
            data.put("startDate", faker.date().past(30, java.util.concurrent.TimeUnit.DAYS).toString());
        } else if ("rollouts".equals(prefix)) {
            data.put("percentage", faker.number().numberBetween(0, 101));
            data.put("regions", Arrays.asList(
                    faker.options().option("us-east-1", "us-west-2", "eu-west-1")));
            data.put("enabled", faker.random().nextBoolean());
        } else {
            // Generic feature flag group
            data.put("enabled", faker.random().nextBoolean());
            data.put("value", faker.lorem().word());
        }

        return data;
    }

    /**
     * Flattens an object into hierarchical KV entries.
     *
     * @param serviceId service ID
     * @param category category (config, secrets, feature-flags)
     * @param prefix   object prefix
     * @param data     object data map
     * @return list of flattened KV entries
     */
    private List<KVEntry> flattenObject(String serviceId, String category, String prefix,
                                       Map<String, Object> data) {
        List<KVEntry> entries = new ArrayList<>();
        String basePath = String.format("apps/%s/kv/%s/%s", serviceId, category, prefix);

        flattenEntry(basePath, data, entries);

        log.debug("Generated object KV entries: {} ({} entries)", basePath, entries.size());

        return entries;
    }

    /**
     * Recursively flattens an entry into KV entries.
     *
     * @param basePath base path for keys
     * @param value    value to flatten
     * @param entries  list to add entries to
     */
    private void flattenEntry(String basePath, Object value, List<KVEntry> entries) {
        if (value instanceof Map<?, ?> mapValue) {
            mapValue.forEach((key, val) -> {
                String newPath = basePath + "/" + key;
                flattenEntry(newPath, val, entries);
            });
        } else {
            // Leaf value
            String stringValue = String.valueOf(value);
            byte[] valueBytes = stringValue.getBytes(StandardCharsets.UTF_8);

            entries.add(KVEntry.builder()
                    .key(basePath)
                    .value(valueBytes)
                    .modifyIndex(0)
                    .createIndex(0)
                    .flags(KVType.LEAF.getFlagValue()) // Leaf values within object
                    .lockIndex(0)
                    .session(null)
                    .build());
        }
    }
}

