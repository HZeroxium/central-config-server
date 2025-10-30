package com.example.control.infrastructure.config.cache;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Utility class for generating safe, deterministic cache keys with standardized
 * format.
 * <p>
 * Key format: {@code {applicationName}::{cacheName}:v{version}:{keyHash}}
 * <p>
 * Provides methods to generate cache keys that are:
 * <ul>
 * <li>Deterministic: same input produces same key</li>
 * <li>Collision-resistant: uses hashing for complex/long keys</li>
 * <li>Stable: not affected by object identity or hashCode() variations</li>
 * <li>Versioned: includes cache version for invalidation</li>
 * <li>Namespaced: includes application name to avoid conflicts</li>
 * </ul>
 * 
 * <h2>Key Generation Strategies</h2>
 * <ul>
 * <li><b>Short keys (&lt;255 chars):</b> Full key string preserved</li>
 * <li><b>Long keys (&gt;=255 chars):</b> SHA-256 hash used for brevity</li>
 * <li><b>Collections:</b> Sorted and joined with delimiter</li>
 * <li><b>Complex objects:</b> SHA-256 hash for deterministic keys</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public final class CacheKeyGenerator {

  private static final int MAX_KEY_LENGTH = 255;

  private CacheKeyGenerator() {
    // Utility class - prevent instantiation
  }

  /**
   * Generate a standardized cache key with format:
   * {applicationName}::{cacheName}:v{version}:{keyHash}
   * 
   * @param applicationName application name (namespace)
   * @param cacheName       cache name
   * @param version         cache version
   * @param keyParts        key parts to include (will be hashed if too long)
   * @return standardized cache key
   */
  public static String generateStandardKey(String applicationName, String cacheName, int version, String... keyParts) {
    String keyContent = String.join(":", keyParts);
    String keyHash;

    if (keyContent.length() > MAX_KEY_LENGTH) {
      // Hash long keys
      keyHash = sha256Hash(keyContent);
    } else {
      // Use key content directly for short keys
      keyHash = keyContent;
    }

    return String.format("%s::%s:v%d:%s", applicationName, cacheName, version, keyHash);
  }

  /**
   * Generate a cache key from a collection of strings.
   * <p>
   * The collection is sorted and joined to ensure deterministic ordering.
   * 
   * @param prefix     key prefix (e.g., "userIds")
   * @param collection collection of strings to include in key
   * @return deterministic cache key
   * @deprecated Use {@link #generateStandardKey(String, String, int, String...)}
   *             instead
   */
  @Deprecated
  public static String generateKey(String prefix, Collection<String> collection) {
    if (collection == null || collection.isEmpty()) {
      return prefix + ":empty";
    }

    String sorted = collection.stream()
        .sorted()
        .collect(Collectors.joining(","));

    return prefix + ":" + sorted;
  }

  /**
   * Generate a cache key from a collection with custom delimiter.
   * 
   * @param prefix     key prefix
   * @param collection collection of strings
   * @param delimiter  delimiter to use for joining
   * @return deterministic cache key
   */
  public static String generateKey(String prefix, Collection<String> collection, String delimiter) {
    if (collection == null || collection.isEmpty()) {
      return prefix + ":empty";
    }

    String sorted = collection.stream()
        .sorted()
        .collect(Collectors.joining(delimiter));

    return prefix + ":" + sorted;
  }

  /**
   * Generate a cache key from an object's string representation.
   * <p>
   * For simple objects, uses toString(). For complex objects, consider using
   * {@link #generateKeyFromHash(String, Object)} for better collision resistance.
   * 
   * @param prefix key prefix
   * @param object object to include in key
   * @return cache key
   */
  public static String generateKey(String prefix, Object object) {
    if (object == null) {
      return prefix + ":null";
    }
    return prefix + ":" + object.toString();
  }

  /**
   * Generate a cache key using SHA-256 hash for collision resistance.
   * <p>
   * Useful for complex objects or criteria where toString() may not be stable
   * or may produce very long keys.
   * 
   * @param prefix key prefix
   * @param object object to hash
   * @return cache key with SHA-256 hash suffix
   */
  public static String generateKeyFromHash(String prefix, Object object) {
    if (object == null) {
      return prefix + ":null";
    }

    String content = object.toString();
    String hash = sha256Hash(content);
    return prefix + ":" + hash;
  }

  /**
   * Generate a cache key from multiple parts.
   * 
   * @param parts variable number of key parts
   * @return joined cache key with ":" separator
   */
  public static String generateKey(String... parts) {
    if (parts == null || parts.length == 0) {
      return "empty";
    }
    return String.join(":", parts);
  }

  /**
   * Generate a cache key with criteria and pageable.
   * 
   * @param prefix   key prefix
   * @param criteria criteria object
   * @param pageable pageable object (may be null)
   * @return cache key
   */
  public static String generateKey(String prefix, Object criteria, Object pageable) {
    StringBuilder key = new StringBuilder(prefix);

    if (criteria != null) {
      key.append(":").append(generateKeyFromHash("criteria", criteria));
    } else {
      key.append(":criteria:null");
    }

    if (pageable != null) {
      key.append(":").append(pageable.toString());
    } else {
      key.append(":pageable:null");
    }

    return key.toString();
  }

  /**
   * Compute SHA-256 hash of a string.
   * 
   * @param input input string
   * @return hexadecimal hash string (first 16 characters for brevity)
   */
  static String sha256Hash(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(input.getBytes());

      // Convert to hex string and take first 16 chars for brevity
      StringBuilder hexString = new StringBuilder();
      for (int i = 0; i < Math.min(8, hashBytes.length); i++) {
        String hex = Integer.toHexString(0xff & hashBytes[i]);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      log.error("SHA-256 algorithm not available, falling back to hashCode", e);
      // Fallback to hashCode if SHA-256 is not available (shouldn't happen)
      return String.valueOf(input.hashCode());
    }
  }
}
