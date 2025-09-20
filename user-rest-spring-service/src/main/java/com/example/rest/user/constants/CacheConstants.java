package com.example.rest.user.constants;

/**
 * Constants for cache configuration.
 */
public final class CacheConstants {

  // Cache names
  public static final String USER_BY_ID_CACHE = "userById";
  public static final String USERS_BY_CRITERIA_CACHE = "usersByCriteria";
  public static final String COUNT_BY_CRITERIA_CACHE = "countByCriteria";

  // Cache key prefixes
  public static final String USER_SERVICE_GET_BY_ID_KEY_PREFIX = "UserService:getById:v1:";
  public static final String USER_SERVICE_LIST_BY_CRITERIA_KEY_PREFIX = "UserService:listByCriteria:v1:";
  public static final String USER_SERVICE_COUNT_BY_CRITERIA_KEY_PREFIX = "UserService:countByCriteria:v1:";

  private CacheConstants() {
    // Utility class
  }
}
