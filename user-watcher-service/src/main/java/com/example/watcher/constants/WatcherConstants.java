package com.example.watcher.constants;

public final class WatcherConstants {

  // Kafka Consumer Group
  public static final String CONSUMER_GROUP_ID = "user-watcher-group";

  // Container Factory Names
  public static final String CONTAINER_FACTORY_AVRO = "avroKafkaListenerContainerFactory";

  // Kafka Template Names
  public static final String KAFKA_TEMPLATE_AVRO = "avroKafkaTemplate";

  // Cache Names
  public static final String CACHE_USER_BY_ID = "userById";
  public static final String CACHE_USERS_BY_CRITERIA = "usersByCriteria";
  public static final String CACHE_COUNT_BY_CRITERIA = "countByCriteria";

  // Default Values
  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 20;
  public static final boolean DEFAULT_INCLUDE_DELETED = false;

  // Error Messages
  public static final String ERROR_PING_FAILED = "Ping failed";
  public static final String ERROR_USER_CREATION_FAILED = "User creation failed";
  public static final String ERROR_USER_RETRIEVAL_FAILED = "User retrieval failed";
  public static final String ERROR_USER_UPDATE_FAILED = "User update failed";
  public static final String ERROR_USER_DELETION_FAILED = "User deletion failed";
  public static final String ERROR_USER_LISTING_FAILED = "User listing failed";

  // Success Messages
  public static final String SUCCESS_PING = "Service is healthy";
  public static final String SUCCESS_USER_CREATED = "User created successfully";
  public static final String SUCCESS_USER_RETRIEVED = "User retrieved successfully";
  public static final String SUCCESS_USER_UPDATED = "User updated successfully";
  public static final String SUCCESS_USER_DELETED = "User deleted successfully";
  public static final String SUCCESS_USERS_RETRIEVED = "Users retrieved successfully";

  private WatcherConstants() {
    // Utility class
  }
}
