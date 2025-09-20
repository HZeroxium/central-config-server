package com.example.rest.user.constants;

/**
 * Constants for metrics and monitoring.
 */
public final class MetricsConstants {

  // Service layer metrics
  public static final String SERVICE_PING = "service.rest.ping";
  public static final String SERVICE_CREATE = "service.rest.create";
  public static final String SERVICE_GET_BY_ID = "service.rest.get.by.id";
  public static final String SERVICE_UPDATE = "service.rest.update";
  public static final String SERVICE_DELETE = "service.rest.delete";
  public static final String SERVICE_LIST_BY_CRITERIA = "service.rest.list.by.criteria";
  public static final String SERVICE_COUNT_BY_CRITERIA = "service.rest.count.by.criteria";

  // Controller layer metrics
  public static final String REST_API_PING = "rest.api.ping";
  public static final String REST_API_CREATE_USER = "rest.api.create.user";
  public static final String REST_API_GET_USER = "rest.api.get.user";
  public static final String REST_API_UPDATE_USER = "rest.api.update.user";
  public static final String REST_API_DELETE_USER = "rest.api.delete.user";
  public static final String REST_API_LIST_USERS = "rest.api.list.users";

  // Metric descriptions
  public static final String PING_DESCRIPTION = "Time taken to ping Thrift service";
  public static final String CREATE_DESCRIPTION = "Time taken to create user via Thrift";
  public static final String GET_BY_ID_DESCRIPTION = "Time taken to get user by ID via Thrift";
  public static final String UPDATE_DESCRIPTION = "Time taken to update user via Thrift";
  public static final String DELETE_DESCRIPTION = "Time taken to delete user via Thrift";
  public static final String LIST_BY_CRITERIA_DESCRIPTION = "Time taken to list users by criteria via Thrift";
  public static final String COUNT_BY_CRITERIA_DESCRIPTION = "Time taken to count users by criteria via Thrift";

  private MetricsConstants() {
    // Utility class
  }
}
