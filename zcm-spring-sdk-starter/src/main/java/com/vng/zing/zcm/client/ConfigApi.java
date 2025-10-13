package com.vng.zing.zcm.client;

import java.util.Map;

/**
 * Configuration API for accessing environment properties and configuration snapshots.
 */
public interface ConfigApi {
  
  /**
   * Retrieves a property value from the environment.
   * 
   * @param key the property key
   * @return the resolved value, or null if not found
   */
  String get(String key);
  
  /**
   * Returns all properties with the given prefix.
   * 
   * @param prefix the property key prefix to filter
   * @return map of matching key/value pairs
   */
  Map<String, Object> getAll(String prefix);
  
  /**
   * Returns the SHA-256 hash of the current configuration.
   * 
   * @return hexadecimal hash string
   */
  String hash();
  
  /**
   * Returns the configuration snapshot as a map.
   * 
   * @return map with application, profile, version, and properties
   */
  Map<String, Object> snapshot();
}
