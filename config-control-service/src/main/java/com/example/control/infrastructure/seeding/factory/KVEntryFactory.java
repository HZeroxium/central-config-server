package com.example.control.infrastructure.seeding.factory;

import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.model.kv.KVType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Factory for generating realistic leaf KV entries.
 * <p>
 * Generates simple key-value pairs with realistic values for config, secrets,
 * and feature flags categories.
 * </p>
 *
 * <p>
 * <strong>Generation Strategy:</strong>
 * </p>
 * <ul>
 * <li>Config: Database URLs, connection strings, API endpoints, timeouts</li>
 * <li>Secrets: API keys, JWT secrets, encryption keys, tokens</li>
 * <li>Feature Flags: Boolean flags, string flags, numeric flags</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KVEntryFactory {

    /**
     * Config key templates.
     */
    private static final List<String> CONFIG_KEYS = List.of(
            "database.url", "database.host", "database.port", "database.name",
            "database.username", "database.pool.size", "database.timeout",
            "api.endpoint", "api.timeout", "api.retry.count", "api.retry.delay",
            "cache.ttl", "cache.size", "cache.eviction.policy",
            "logging.level", "logging.format", "logging.file.path",
            "messaging.broker.url", "messaging.queue.name", "messaging.batch.size"
    );

    /**
     * Secrets key templates.
     */
    private static final List<String> SECRETS_KEYS = List.of(
            "api.key", "jwt.secret", "encryption.key", "oauth.client.id",
            "oauth.client.secret", "ssl.certificate", "ssl.private.key",
            "database.password", "redis.password", "aws.access.key",
            "aws.secret.key", "github.token", "docker.registry.token"
    );

    /**
     * Feature flag key templates.
     */
    private static final List<String> FEATURE_FLAG_KEYS = List.of(
            "enable.new.feature", "enable.experimental.api", "enable.cache",
            "enable.metrics", "enable.tracing", "enable.rate.limit",
            "feature.rollout.percentage", "feature.ab.test.variant",
            "maintenance.mode", "readonly.mode", "debug.mode"
    );

    private final Faker faker;

    /**
     * Generates a leaf KV entry for config category.
     *
     * @param serviceId service ID
     * @param key       relative key path (e.g., "database.url")
     * @return KV entry with LEAF type
     */
    public KVEntry generateConfigLeaf(String serviceId, String key) {
        String value = generateConfigValue(key);
        return createLeafEntry(serviceId, "config", key, value);
    }

    /**
     * Generates a leaf KV entry for secrets category.
     *
     * @param serviceId service ID
     * @param key       relative key path (e.g., "api.key")
     * @return KV entry with LEAF type
     */
    public KVEntry generateSecretLeaf(String serviceId, String key) {
        String value = generateSecretValue(key);
        return createLeafEntry(serviceId, "secrets", key, value);
    }

    /**
     * Generates a leaf KV entry for feature flags category.
     *
     * @param serviceId service ID
     * @param key       relative key path (e.g., "enable.new.feature")
     * @return KV entry with LEAF type
     */
    public KVEntry generateFeatureFlagLeaf(String serviceId, String key) {
        String value = generateFeatureFlagValue(key);
        return createLeafEntry(serviceId, "feature-flags", key, value);
    }

    /**
     * Generates a random config key.
     *
     * @return config key
     */
    public String generateConfigKey() {
        return CONFIG_KEYS.get(faker.random().nextInt(CONFIG_KEYS.size()));
    }

    /**
     * Generates a random secret key.
     *
     * @return secret key
     */
    public String generateSecretKey() {
        return SECRETS_KEYS.get(faker.random().nextInt(SECRETS_KEYS.size()));
    }

    /**
     * Generates a random feature flag key.
     *
     * @return feature flag key
     */
    public String generateFeatureFlagKey() {
        return FEATURE_FLAG_KEYS.get(faker.random().nextInt(FEATURE_FLAG_KEYS.size()));
    }

    /**
     * Generates realistic config value based on key.
     *
     * @param key config key
     * @return config value
     */
    private String generateConfigValue(String key) {
        if (key.contains("database.url") || key.contains("database.host")) {
            return String.format("jdbc:postgresql://%s:%d/%s",
                    faker.internet().domainName(),
                    faker.number().numberBetween(5432, 5433),
                    faker.lorem().word());
        }
        if (key.contains("database.port")) {
            return String.valueOf(faker.number().numberBetween(5432, 65535));
        }
        if (key.contains("database.name") || key.contains("database.username")) {
            return faker.lorem().word();
        }
        if (key.contains("timeout") || key.contains("delay")) {
            return String.valueOf(faker.number().numberBetween(1000, 30000));
        }
        if (key.contains("retry.count") || key.contains("pool.size") || key.contains("batch.size")) {
            return String.valueOf(faker.number().numberBetween(1, 100));
        }
        if (key.contains("api.endpoint")) {
            return String.format("https://api.%s.com/v%d",
                    faker.internet().domainName(),
                    faker.number().numberBetween(1, 3));
        }
        if (key.contains("cache.ttl")) {
            return String.valueOf(faker.number().numberBetween(60, 3600));
        }
        if (key.contains("logging.level")) {
            return faker.options().option("DEBUG", "INFO", "WARN", "ERROR");
        }
        if (key.contains("logging.format")) {
            return faker.options().option("JSON", "TEXT", "XML");
        }
        if (key.contains("messaging.broker.url")) {
            return String.format("kafka://%s:%d",
                    faker.internet().domainName(),
                    faker.number().numberBetween(9092, 9093));
        }
        // Default: generic string value
        return faker.lorem().sentence();
    }

    /**
     * Generates realistic secret value based on key.
     *
     * @param key secret key
     * @return secret value (base64-like or token-like)
     */
    private String generateSecretValue(String key) {
        if (key.contains("api.key") || key.contains("jwt.secret") || key.contains("encryption.key")) {
            // Generate base64-like string
            return faker.internet().uuid().replace("-", "") + faker.internet().uuid().replace("-", "");
        }
        if (key.contains("password")) {
            return faker.internet().password(16, 32, true, true, true);
        }
        if (key.contains("token") || key.contains("certificate")) {
            // Generate long token-like string
            return faker.internet().uuid().replace("-", "") +
                    faker.internet().uuid().replace("-", "") +
                    faker.internet().uuid().replace("-", "");
        }
        if (key.contains("client.id")) {
            return faker.internet().uuid();
        }
        if (key.contains("client.secret") || key.contains("secret.key")) {
            return faker.internet().uuid().replace("-", "") + faker.internet().uuid().replace("-", "");
        }
        // Default: UUID-like secret
        return faker.internet().uuid().replace("-", "") + faker.internet().uuid().replace("-", "");
    }

    /**
     * Generates realistic feature flag value based on key.
     *
     * @param key feature flag key
     * @return feature flag value
     */
    private String generateFeatureFlagValue(String key) {
        if (key.contains("enable.") || key.contains(".mode")) {
            // Boolean flags
            return String.valueOf(faker.random().nextBoolean());
        }
        if (key.contains("percentage") || key.contains("rollout")) {
            // Percentage (0-100)
            return String.valueOf(faker.number().numberBetween(0, 101));
        }
        if (key.contains("variant") || key.contains("test")) {
            // String variant
            return faker.options().option("A", "B", "control", "treatment", "default");
        }
        // Default: boolean
        return String.valueOf(faker.random().nextBoolean());
    }

    /**
     * Creates a leaf KV entry.
     *
     * @param serviceId service ID
     * @param category category (config, secrets, feature-flags)
     * @param key      relative key
     * @param value    value as string
     * @return KV entry
     */
    private KVEntry createLeafEntry(String serviceId, String category, String key, String value) {
        String absoluteKey = String.format("apps/%s/kv/%s/%s", serviceId, category, key);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

        log.debug("Generated leaf KV entry: {} = {}", absoluteKey, value);

        return KVEntry.builder()
                .key(absoluteKey)
                .value(valueBytes)
                .modifyIndex(0)
                .createIndex(0)
                .flags(KVType.LEAF.getFlagValue())
                .lockIndex(0)
                .session(null)
                .build();
    }
}

