package com.example.control.infrastructure.seeding.factory;

import com.example.control.application.service.kv.KVTypeCodec;
import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.model.kv.KVListManifest;
import com.example.control.domain.model.kv.KVType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Factory for generating realistic list KV entries.
 * <p>
 * Generates list structures with manifest and items stored in Consul KV.
 * Lists are stored with KVType.LIST flag on the manifest.
 * </p>
 *
 * <p>
 * <strong>Generation Strategy:</strong>
 * </p>
 * <ul>
 * <li>Config: Allowed IPs, whitelisted domains, retry policies (3-10 items)</li>
 * <li>Secrets: API key rotations, token refresh history (3-8 items)</li>
 * <li>Feature Flags: Feature flag rollouts, experiment participants (3-10 items)</li>
 * </ul>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KVListFactory {

    /**
     * List templates for config category.
     */
    private static final List<String> CONFIG_LIST_PREFIXES = List.of(
            "allowed-ips", "whitelisted-domains", "retry-policies", "endpoints"
    );

    /**
     * List templates for secrets category.
     */
    private static final List<String> SECRETS_LIST_PREFIXES = List.of(
            "api-key-rotations", "token-refresh-history", "certificate-chain"
    );

    /**
     * List templates for feature flags category.
     */
    private static final List<String> FEATURE_FLAG_LIST_PREFIXES = List.of(
            "rollouts", "experiment-participants", "feature-groups"
    );

    private final Faker faker;
    private final KVTypeCodec kvTypeCodec;

    /**
     * Generates a list KV entry for config category.
     *
     * @param serviceId service ID
     * @param prefix    list prefix (e.g., "allowed-ips")
     * @param itemCount number of items in the list
     * @return list of KV entries representing the list structure
     */
    public List<KVEntry> generateConfigList(String serviceId, String prefix, int itemCount) {
        List<Map<String, Object>> items = generateConfigListItems(prefix, itemCount);
        return createListEntries(serviceId, "config", prefix, items);
    }

    /**
     * Generates a list KV entry for secrets category.
     *
     * @param serviceId service ID
     * @param prefix    list prefix (e.g., "api-key-rotations")
     * @param itemCount number of items in the list
     * @return list of KV entries representing the list structure
     */
    public List<KVEntry> generateSecretList(String serviceId, String prefix, int itemCount) {
        List<Map<String, Object>> items = generateSecretListItems(prefix, itemCount);
        return createListEntries(serviceId, "secrets", prefix, items);
    }

    /**
     * Generates a list KV entry for feature flags category.
     *
     * @param serviceId service ID
     * @param prefix    list prefix (e.g., "rollouts")
     * @param itemCount number of items in the list
     * @return list of KV entries representing the list structure
     */
    public List<KVEntry> generateFeatureFlagList(String serviceId, String prefix, int itemCount) {
        List<Map<String, Object>> items = generateFeatureFlagListItems(prefix, itemCount);
        return createListEntries(serviceId, "feature-flags", prefix, items);
    }

    /**
     * Generates a random config list prefix.
     *
     * @return list prefix
     */
    public String generateConfigListPrefix() {
        return CONFIG_LIST_PREFIXES.get(faker.random().nextInt(CONFIG_LIST_PREFIXES.size()));
    }

    /**
     * Generates a random secret list prefix.
     *
     * @return list prefix
     */
    public String generateSecretListPrefix() {
        return SECRETS_LIST_PREFIXES.get(faker.random().nextInt(SECRETS_LIST_PREFIXES.size()));
    }

    /**
     * Generates a random feature flag list prefix.
     *
     * @return list prefix
     */
    public String generateFeatureFlagListPrefix() {
        return FEATURE_FLAG_LIST_PREFIXES.get(faker.random().nextInt(FEATURE_FLAG_LIST_PREFIXES.size()));
    }

    /**
     * Generates config list items.
     *
     * @param prefix    list prefix
     * @param itemCount number of items
     * @return list of item data maps
     */
    private List<Map<String, Object>> generateConfigListItems(String prefix, int itemCount) {
        List<Map<String, Object>> items = new ArrayList<>();

        if ("allowed-ips".equals(prefix)) {
            for (int i = 0; i < itemCount; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("ip", faker.internet().ipV4Address());
                item.put("description", faker.lorem().sentence());
                item.put("addedAt", faker.date().past(30, java.util.concurrent.TimeUnit.DAYS).toString());
                items.add(item);
            }
        } else if ("whitelisted-domains".equals(prefix)) {
            for (int i = 0; i < itemCount; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("domain", faker.internet().domainName());
                item.put("enabled", faker.random().nextBoolean());
                items.add(item);
            }
        } else if ("retry-policies".equals(prefix)) {
            for (int i = 0; i < itemCount; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", faker.lorem().word());
                item.put("maxRetries", faker.number().numberBetween(1, 5));
                item.put("backoff", faker.number().numberBetween(100, 1000));
                items.add(item);
            }
        } else {
            // Generic config list items
            for (int i = 0; i < itemCount; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", "item-" + (i + 1));
                item.put("value", faker.lorem().word());
                item.put("enabled", faker.random().nextBoolean());
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Generates secret list items.
     *
     * @param prefix    list prefix
     * @param itemCount number of items
     * @return list of item data maps
     */
    private List<Map<String, Object>> generateSecretListItems(String prefix, int itemCount) {
        List<Map<String, Object>> items = new ArrayList<>();

        if ("api-key-rotations".equals(prefix)) {
            for (int i = 0; i < itemCount; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("keyId", faker.internet().uuid());
                item.put("rotatedAt", faker.date().past(90, java.util.concurrent.TimeUnit.DAYS).toString());
                item.put("status", faker.options().option("active", "expired", "revoked"));
                items.add(item);
            }
        } else if ("token-refresh-history".equals(prefix)) {
            for (int i = 0; i < itemCount; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("tokenId", faker.internet().uuid());
                item.put("refreshedAt", faker.date().past(30, java.util.concurrent.TimeUnit.DAYS).toString());
                item.put("expiresAt", faker.date().future(30, java.util.concurrent.TimeUnit.DAYS).toString());
                items.add(item);
            }
        } else {
            // Generic secret list items
            for (int i = 0; i < itemCount; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", "secret-" + (i + 1));
                item.put("value", faker.internet().uuid().replace("-", ""));
                item.put("createdAt", faker.date().past(30, java.util.concurrent.TimeUnit.DAYS).toString());
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Generates feature flag list items.
     *
     * @param prefix    list prefix
     * @param itemCount number of items
     * @return list of item data maps
     */
    private List<Map<String, Object>> generateFeatureFlagListItems(String prefix, int itemCount) {
        List<Map<String, Object>> items = new ArrayList<>();

        if ("rollouts".equals(prefix) || "experiment-participants".equals(prefix)) {
            for (int i = 0; i < itemCount; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", faker.internet().uuid());
                item.put("variant", faker.options().option("A", "B", "control"));
                item.put("percentage", faker.number().numberBetween(0, 101));
                items.add(item);
            }
        } else {
            // Generic feature flag list items
            for (int i = 0; i < itemCount; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("flag", faker.lorem().word());
                item.put("enabled", faker.random().nextBoolean());
                item.put("value", faker.lorem().word());
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Creates list KV entries with manifest and items.
     *
     * @param serviceId service ID
     * @param category  category (config, secrets, feature-flags)
     * @param prefix    list prefix
     * @param items     list of item data maps
     * @return list of KV entries
     */
    private List<KVEntry> createListEntries(String serviceId, String category, String prefix,
                                            List<Map<String, Object>> items) {
        List<KVEntry> entries = new ArrayList<>();
        String basePath = String.format("apps/%s/kv/%s/%s", serviceId, category, prefix);

        // Generate item IDs
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            itemIds.add("item-" + (i + 1));
        }

        // Create manifest
        KVListManifest manifest = KVListManifest.withOrder(itemIds, null, faker.internet().uuid());
        byte[] manifestBytes = kvTypeCodec.writeManifest(manifest);

        entries.add(KVEntry.builder()
                .key(basePath + "/.manifest")
                .value(manifestBytes)
                .modifyIndex(0)
                .createIndex(0)
                .flags(KVType.LIST.getFlagValue())
                .lockIndex(0)
                .session(null)
                .build());

        // Create item entries
        for (int i = 0; i < items.size(); i++) {
            String itemId = itemIds.get(i);
            Map<String, Object> itemData = items.get(i);

            // Flatten item data into KV entries
            for (Map.Entry<String, Object> entry : itemData.entrySet()) {
                String itemKeyPath = basePath + "/items/" + itemId + "/" + entry.getKey();
                String value = String.valueOf(entry.getValue());
                byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

                entries.add(KVEntry.builder()
                        .key(itemKeyPath)
                        .value(valueBytes)
                        .modifyIndex(0)
                        .createIndex(0)
                        .flags(KVType.LEAF.getFlagValue())
                        .lockIndex(0)
                        .session(null)
                        .build());
            }
        }

        log.debug("Generated list KV entries: {} ({} items, {} total entries)",
                basePath, items.size(), entries.size());

        return entries;
    }
}

