package com.vng.zing.zcm.client.kv;

import com.vng.zing.zcm.kv.KVStructuredFormat;
import com.vng.zing.zcm.kv.dto.*;
import com.vng.zing.zcm.kv.exceptions.KVAccessDeniedException;
import com.vng.zing.zcm.kv.exceptions.KVAuthenticationException;
import com.vng.zing.zcm.kv.exceptions.KVClientException;

import java.util.List;
import java.util.Optional;

/**
 * Key-Value API for reading KV entries from config-control-service.
 * <p>
 * This API provides read-only access to the Key-Value store managed by
 * config-control-service. All operations require authentication via JWT token
 * (either pass-through from SecurityContext or client credentials).
 */
public interface KVApi {

  /**
   * Gets a single KV entry by key path.
   *
   * @param serviceId the service ID (e.g., "sample-service")
   * @param key       the key path relative to service root (e.g., "config/db.url")
   * @return optional KV entry, empty if not found or access denied
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  Optional<KVEntry> get(String serviceId, String key);

  /**
   * Gets a KV entry value as raw bytes.
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root
   * @return optional byte array, empty if not found or access denied
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  Optional<byte[]> getRaw(String serviceId, String key);

  /**
   * Gets a KV entry value as UTF-8 string.
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root
   * @return optional string value, empty if not found or access denied
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  Optional<String> getString(String serviceId, String key);

  /**
   * Gets a LEAF_LIST entry and parses it as a comma-separated list.
   * <p>
   * This method retrieves a KV entry with flag=3 (LEAF_LIST) and parses
   * its value as a comma-separated string into a list of elements.
   * Whitespace around elements is trimmed, and empty elements are filtered out.
   * </p>
   * <p>
   * Note: This method is distinct from {@link #getList(String, String)} which
   * returns a structured LIST (with manifest) stored under a prefix. This method
   * takes a single key and returns a simple list of strings parsed from the value.
   * </p>
   *
   * @param serviceId the service ID
   * @param key       the key path relative to service root (single key, not prefix)
   * @return optional list of strings, empty if not found or access denied
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  Optional<List<String>> getLeafList(String serviceId, String key);

  /**
   * Lists all KV entries under a prefix.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix to list (relative to service root, empty string for root)
   * @return list of KV entries (empty list if none found or access denied)
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  List<KVEntry> list(String serviceId, String prefix);

  /**
   * Lists only keys (not full entries) under a prefix.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix to list (relative to service root, empty string for root)
   * @return list of key paths (empty list if none found or access denied)
   * @throws KVClientException if network or client error occurs
   * @throws KVAuthenticationException if authentication fails (401)
   * @throws KVAccessDeniedException if access is denied (403)
   */
  List<String> listKeys(String serviceId, String prefix);

  /**
   * Retrieves a logical list stored under a prefix.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix relative to service root
   * @return optional list result
   */
  Optional<KVListResult> getList(String serviceId, String prefix);

  /**
   * Writes list items and manifest atomically.
   *
   * @param serviceId the service ID
   * @param prefix    the prefix relative to service root
   * @param request   list write request
   * @return transaction result
   */
  KVTransactionResult putList(String serviceId, String prefix, KVListWriteRequest request);

  /**
   * Executes low-level KV transaction operations atomically.
   *
   * @param serviceId   the service ID
   * @param operations  transaction operations
   * @return transaction result
   */
  KVTransactionResult executeTransaction(String serviceId, List<KVTransactionOperationRequest> operations);

  /**
   * Renders a prefix as structured content (JSON/YAML/Properties).
   *
   * @param serviceId the service ID
   * @param prefix    the prefix relative to service root
   * @param format    target format
   * @return formatted content
   */
  Optional<String> view(String serviceId, String prefix, KVStructuredFormat format);
}

