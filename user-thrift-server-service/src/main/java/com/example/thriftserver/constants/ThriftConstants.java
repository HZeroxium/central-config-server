package com.example.thriftserver.constants;

public final class ThriftConstants {

  // Kafka Consumer Group
  public static final String CONSUMER_GROUP_ID = "user-thrift-server-group";

  // Container Factory Names
  public static final String CONTAINER_FACTORY_AVRO = "avroKafkaListenerContainerFactory";
  public static final String CONTAINER_FACTORY_RPC = "rpcListenerFactory";

  // Kafka Template Names
  public static final String KAFKA_TEMPLATE_AVRO = "avroKafkaTemplate";

  // Response Status Codes
  public static final int STATUS_SUCCESS = 0;
  public static final int STATUS_USER_NOT_FOUND = 2;
  public static final int STATUS_ERROR = 1;

  // Error Messages
  public static final String ERROR_USER_NOT_FOUND = "User not found";
  public static final String ERROR_USER_CREATION_FAILED = "User creation failed";
  public static final String ERROR_USER_RETRIEVAL_FAILED = "Error retrieving user";
  public static final String ERROR_USER_UPDATE_FAILED = "Error updating user";
  public static final String ERROR_USER_DELETION_FAILED = "Error deleting user";
  public static final String ERROR_USER_LISTING_FAILED = "Error listing users";

  // Success Messages
  public static final String SUCCESS_USER_CREATED = "User created successfully";
  public static final String SUCCESS_USER_RETRIEVED = "User retrieved successfully";
  public static final String SUCCESS_USER_UPDATED = "User updated successfully";
  public static final String SUCCESS_USER_DELETED = "User deleted successfully";
  public static final String SUCCESS_USERS_RETRIEVED = "Users retrieved successfully";
  public static final String SUCCESS_PING = "Service is healthy";

  private ThriftConstants() {
    // Utility class
  }
}
