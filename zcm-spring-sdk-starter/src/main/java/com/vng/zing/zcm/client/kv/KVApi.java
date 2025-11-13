package com.vng.zing.zcm.client.kv;

import com.vng.zing.zcm.kv.KVStructuredFormat;
import com.vng.zing.zcm.kv.exceptions.KVAccessDeniedException;
import com.vng.zing.zcm.kv.exceptions.KVAuthenticationException;
import com.vng.zing.zcm.kv.exceptions.KVClientException;

import java.util.List;
import java.util.Map;

/**
 * Key-Value API for reading KV entries from config-control-service.
 * <p>
 * This API provides read-only access to the Key-Value store managed by
 * config-control-service. All operations require authentication via JWT token
 * (either pass-through from SecurityContext or client credentials).
 * <p>
 * The API returns simple primitive types (String, Integer, Boolean, etc.) instead
 * of complex DTOs for better usability. All methods return {@code null} if the
 * key is not found, or empty collections for list/map operations.
 * <p>
 * Type conversion errors are handled gracefully: the method returns {@code null}
 * and logs a warning instead of throwing an exception.
 * <p>
 * Example usage:
 * <pre>{@code
 * String dbUrl = kvApi.getString("my-service", "config/db.url");
 * Integer port = kvApi.getInteger("my-service", "config/server.port");
 * Boolean enabled = kvApi.getBoolean("my-service", "config/feature.enabled");
 * List<String> hosts = kvApi.getList("my-service", "config/hosts");
 * Map<String, Object> config = kvApi.getMap("my-service", "config/");
 * }</pre>
 */
public interface KVApi {

  /**
   * Gets a KV entry value as UTF-8 string.
   *
   * @param serviceId the service ID (e.g., "sample-service")
   * @param key       the key path relative to service root (e.g., "config/db.url")
   * @return string value, or {@code null} if not found
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  String getString(String serviceId, String key);

  /**
   * Gets a KV entry value as Integer.
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root
   * @return integer value, or {@code null} if not found or parse error
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  Integer getInteger(String serviceId, String key);

  /**
   * Gets a KV entry value as Long.
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root
   * @return long value, or {@code null} if not found or parse error
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  Long getLong(String serviceId, String key);

  /**
   * Gets a KV entry value as Boolean.
   * <p>
   * Accepts: "true"/"false" (case-insensitive), "1"/"0", "yes"/"no"
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root
   * @return boolean value, or {@code null} if not found or parse error
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  Boolean getBoolean(String serviceId, String key);

  /**
   * Gets a KV entry value as Double.
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root
   * @return double value, or {@code null} if not found or parse error
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  Double getDouble(String serviceId, String key);

  /**
   * Gets a KV entry value as raw bytes.
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root
   * @return byte array, or {@code null} if not found
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  byte[] getBytes(String serviceId, String key);

  /**
   * Gets a KV entry value as a list of strings.
   * <p>
   * This method handles both comma-separated strings and structured lists.
   * For comma-separated strings, it parses the value by splitting on commas
   * and trimming whitespace. For structured lists, it extracts the item data.
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root
   * @return list of strings, or empty list if not found
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  List<String> getList(String serviceId, String key);

  /**
   * Gets a structured list stored under a prefix as a list of maps.
   * <p>
   * This method retrieves a structured list (with manifest) and returns
   * only the items as a list of maps. The manifest is ignored.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix relative to service root
   * @return list of maps (each map represents an item), or empty list if not found
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  List<Map<String, Object>> getStructuredList(String serviceId, String prefix);

  /**
   * Gets all KV entries under a prefix as a flat map.
   * <p>
   * The map key is the entry path (relative to prefix), and the value is
   * the decoded string value of the entry.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix to list (relative to service root, empty string for root)
   * @return map of key-value pairs, or empty map if not found
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  Map<String, Object> getMap(String serviceId, String prefix);

  /**
   * Lists only keys (not full entries) under a prefix.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix to list (relative to service root, empty string for root)
   * @return list of key paths, or empty list if not found
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  List<String> listKeys(String serviceId, String prefix);

  /**
   * Renders a prefix as structured content (JSON/YAML/Properties).
   *
   * @param serviceId the service ID
   * @param prefix    the prefix relative to service root
   * @param format    target format
   * @return formatted content, or {@code null} if not found
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  String view(String serviceId, String prefix, KVStructuredFormat format);

  /**
   * Checks if a KV entry exists.
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root
   * @return {@code true} if the key exists, {@code false} otherwise
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  boolean exists(String serviceId, String key);
}

