package com.example.control.infrastructure.seeding.factory;

import com.example.control.domain.model.kv.KVListManifest;
import com.example.control.domain.model.kv.KVListStructure;
import com.example.control.domain.model.kv.KVType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Factory for generating realistic list KV entries.
 * <p>
 * Generates list structures with manifest and items as KVListStructure.
 * Lists will be stored by KVService.putList().
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

    /**
     * Generates a list KV entry for config category.
     *
     * @param prefix    list prefix (e.g., "allowed-ips")
     * @param itemCount number of items in the list
     * @return KVListStructure with items and manifest
     */
    public KVListStructure generateConfigList(String prefix, int itemCount) {
        List<Map<String, Object>> itemDataList = generateConfigListItems(prefix, itemCount);
        return buildListStructure(itemDataList);
    }

    /**
     * Generates a list KV entry for secrets category.
     *
     * @param prefix    list prefix (e.g., "api-key-rotations")
     * @param itemCount number of items in the list
     * @return KVListStructure with items and manifest
     */
    public KVListStructure generateSecretList(String prefix, int itemCount) {
        List<Map<String, Object>> itemDataList = generateSecretListItems(prefix, itemCount);
        return buildListStructure(itemDataList);
    }

    /**
     * Generates a list KV entry for feature flags category.
     *
     * @param prefix    list prefix (e.g., "rollouts")
     * @param itemCount number of items in the list
     * @return KVListStructure with items and manifest
     */
    public KVListStructure generateFeatureFlagList(String prefix, int itemCount) {
        List<Map<String, Object>> itemDataList = generateFeatureFlagListItems(prefix, itemCount);
        return buildListStructure(itemDataList);
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
     * Builds a KVListStructure from item data maps.
     *
     * @param itemDataList list of item data maps
     * @return KVListStructure with items and manifest
     */
    private KVListStructure buildListStructure(List<Map<String, Object>> itemDataList) {
        List<KVListStructure.Item> items = new ArrayList<>();
        List<String> itemIds = new ArrayList<>();

        for (int i = 0; i < itemDataList.size(); i++) {
            String itemId = "item-" + (i + 1);
            itemIds.add(itemId);
            Map<String, Object> itemData = itemDataList.get(i);
            items.add(new KVListStructure.Item(itemId, itemData));
        }

        KVListManifest manifest = KVListManifest.withOrder(itemIds, null, faker.internet().uuid());

        log.debug("Generated list KV structure: {} items", items.size());

        return new KVListStructure(items, manifest, KVType.LIST);
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
}

