package com.example.rest.user.adapter;

import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.common.exception.UserNotFoundException;
import com.example.common.exception.ThriftServiceException;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.user.thrift.*;
import com.vng.zing.zcm.client.ClientApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Thrift client adapter using ZCM SDK for service discovery and load balancing
 */
@Slf4j
@Component("sdkThriftUserClientAdapter")
@RequiredArgsConstructor
public class SdkThriftUserClientAdapter implements ThriftUserClientPort {

  private final ClientApi zcmClient;

  private UserService.Client createClient() throws Exception {
    // Use SDK to discover and select thrift server instance
    ServiceInstance instance = zcmClient.choose("user-thrift-server-service");
    if (instance == null) {
      throw new ThriftServiceException("No healthy instances found for user-thrift-server-service", "discovery");
    }

    String host = instance.getHost();
    int thriftPort = getThriftPort(instance);

    log.debug("Creating Thrift client connection to discovered instance {}:{} (thrift port)", host, thriftPort);

    TTransport transport = new TSocket(host, thriftPort, 5000);
    transport.open();
    TBinaryProtocol protocol = new TBinaryProtocol(transport);

    log.debug("Thrift client connection established successfully to {}:{}", host, thriftPort);
    return new UserService.Client(protocol);
  }

  private int getThriftPort(ServiceInstance instance) {
    // Try to get thrift port from service metadata
    if (instance.getMetadata() != null && instance.getMetadata().containsKey("thrift-port")) {
      try {
        return Integer.parseInt(instance.getMetadata().get("thrift-port"));
      } catch (NumberFormatException e) {
        log.warn("Invalid thrift-port in metadata: {}", instance.getMetadata().get("thrift-port"));
      }
    }
    // Fallback to default thrift port
    return 9090;
  }

  private void closeClient(UserService.Client client) {
    if (client != null && client.getInputProtocol() != null) {
      try {
        TTransport transport = client.getInputProtocol().getTransport();
        if (transport != null && transport.isOpen()) {
          transport.close();
          log.debug("Thrift client connection closed successfully");
        }
      } catch (Exception e) {
        log.warn("Failed to close Thrift client connection", e);
      }
    }
  }

  private static User toDomain(TUser t) {
    return User.builder()
        .id(t.getId())
        .name(t.getName())
        .phone(t.getPhone())
        .address(t.getAddress())
        .status(t.getStatus() != null ? User.UserStatus.valueOf(t.getStatus().name()) : User.UserStatus.ACTIVE)
        .role(t.getRole() != null ? User.UserRole.valueOf(t.getRole().name()) : User.UserRole.USER)
        .createdAt(t.getCreatedAt() > 0
            ? java.time.LocalDateTime.ofEpochSecond(t.getCreatedAt() / 1000, 0, java.time.ZoneOffset.UTC)
            : null)
        .createdBy(t.getCreatedBy())
        .updatedAt(t.getUpdatedAt() > 0
            ? java.time.LocalDateTime.ofEpochSecond(t.getUpdatedAt() / 1000, 0, java.time.ZoneOffset.UTC)
            : null)
        .updatedBy(t.getUpdatedBy())
        .version(t.getVersion())
        .deleted(t.isDeleted())
        .deletedAt(t.getDeletedAt() > 0
            ? java.time.LocalDateTime.ofEpochSecond(t.getDeletedAt() / 1000, 0, java.time.ZoneOffset.UTC)
            : null)
        .deletedBy(t.getDeletedBy())
        .build();
  }

  @Override
  public String ping() {
    log.debug("Pinging Thrift service via SDK service discovery");
    UserService.Client client = null;
    try {
      client = createClient();
      TPingResponse response = client.ping();
      if (response.getStatus() == 0) {
        log.debug("Thrift service ping successful: {}", response.getResponse());
        return response.getResponse();
      } else {
        log.error("Thrift service ping failed with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Thrift service ping failed: " + response.getMessage(), "ping");
      }
    } catch (Exception e) {
      log.error("Thrift service ping failed via SDK service discovery", e);
      throw new ThriftServiceException("Failed to ping Thrift service", "ping", e);
    } finally {
      closeClient(client);
    }
  }

  @Override
  public User create(User user) {
    log.debug("Creating user via Thrift service using SDK: {}", user);
    UserService.Client client = null;
    try {
      client = createClient();
      TCreateUserRequest request = new TCreateUserRequest()
          .setName(user.getName())
          .setPhone(user.getPhone())
          .setAddress(user.getAddress())
          .setStatus(user.getStatus() != null ? TUserStatus.valueOf(user.getStatus().name()) : TUserStatus.ACTIVE)
          .setRole(user.getRole() != null ? TUserRole.valueOf(user.getRole().name()) : TUserRole.USER);

      TCreateUserResponse response = client.createUser(request);
      if (response.getStatus() == 0) {
        log.debug("User created via Thrift service: {}", response.getUser());
        User created = toDomain(response.getUser());
        log.debug("Mapped Thrift user to domain user: {}", created);
        return created;
      } else {
        log.error("Failed to create user via Thrift service with status {}: {}", response.getStatus(),
            response.getMessage());
        throw new ThriftServiceException("Failed to create user: " + response.getMessage(), "create");
      }
    } catch (Exception e) {
      log.error("Failed to create user via Thrift service: {}", user, e);
      throw new ThriftServiceException("Failed to create user via Thrift service", "create", e);
    } finally {
      closeClient(client);
    }
  }

  @Override
  public Optional<User> getById(String id) {
    log.debug("Retrieving user by ID via Thrift service using SDK: {}", id);
    UserService.Client client = null;
    try {
      client = createClient();
      TGetUserRequest request = new TGetUserRequest().setId(id);
      TGetUserResponse response = client.getUser(request);

      if (response.getStatus() == 0) {
        log.debug("User found via Thrift service: {}", response.getUser());
        User domainUser = toDomain(response.getUser());
        log.debug("Mapped Thrift user to domain user: {}", domainUser);
        return Optional.of(domainUser);
      } else if (response.getStatus() == 2) { // USER_NOT_FOUND
        log.debug("User not found via Thrift service for ID: {}", id);
        return Optional.empty();
      } else {
        log.error("Failed to retrieve user via Thrift service with status {}: {}", response.getStatus(),
            response.getMessage());
        throw new ThriftServiceException("Failed to retrieve user: " + response.getMessage(), "getById");
      }
    } catch (Exception e) {
      log.error("Failed to retrieve user via Thrift service for ID: {}", id, e);
      throw new ThriftServiceException("Failed to retrieve user via Thrift service", "getById", e);
    } finally {
      closeClient(client);
    }
  }

  @Override
  public User update(User user) {
    log.debug("Updating user via Thrift service using SDK: {}", user);
    UserService.Client client = null;
    try {
      client = createClient();
      TUpdateUserRequest request = new TUpdateUserRequest()
          .setId(user.getId())
          .setName(user.getName())
          .setPhone(user.getPhone())
          .setAddress(user.getAddress())
          .setStatus(user.getStatus() != null ? TUserStatus.valueOf(user.getStatus().name()) : TUserStatus.ACTIVE)
          .setRole(user.getRole() != null ? TUserRole.valueOf(user.getRole().name()) : TUserRole.USER)
          .setVersion(user.getVersion() != null ? user.getVersion() : 1);

      TUpdateUserResponse response = client.updateUser(request);
      if (response.getStatus() == 0) {
        log.debug("User updated via Thrift service: {}", response.getUser());
        User updated = toDomain(response.getUser());
        log.debug("Mapped Thrift user to domain user: {}", updated);
        return updated;
      } else if (response.getStatus() == 1) { // USER_NOT_FOUND
        log.error("User not found for update via Thrift service: {}", user.getId());
        throw new UserNotFoundException(user.getId(), "Thrift service");
      } else {
        log.error("Failed to update user via Thrift service with status {}: {}", response.getStatus(),
            response.getMessage());
        throw new ThriftServiceException("Failed to update user: " + response.getMessage(), "update");
      }
    } catch (Exception e) {
      log.error("Failed to update user via Thrift service: {}", user, e);
      throw new ThriftServiceException("Failed to update user via Thrift service", "update", e);
    } finally {
      closeClient(client);
    }
  }

  @Override
  public void delete(String id) {
    log.debug("Deleting user via Thrift service using SDK: {}", id);
    UserService.Client client = null;
    try {
      client = createClient();
      TDeleteUserRequest request = new TDeleteUserRequest().setId(id);
      TDeleteUserResponse response = client.deleteUser(request);

      if (response.getStatus() == 0) {
        log.debug("User deleted successfully via Thrift service: {}", id);
      } else if (response.getStatus() == 1) { // USER_NOT_FOUND
        log.error("User not found for deletion via Thrift service: {}", id);
        throw new UserNotFoundException(id, "Thrift service");
      } else {
        log.error("Failed to delete user via Thrift service with status {}: {}", response.getStatus(),
            response.getMessage());
        throw new ThriftServiceException("Failed to delete user: " + response.getMessage(), "delete");
      }
    } catch (Exception e) {
      log.error("Failed to delete user via Thrift service for ID: {}", id, e);
      throw new ThriftServiceException("Failed to delete user via Thrift service", "delete", e);
    } finally {
      closeClient(client);
    }
  }

  @Override
  public List<User> listByCriteria(UserQueryCriteria criteria) {
    log.debug("Listing users by criteria via Thrift service using SDK: {}", criteria);
    UserService.Client client = null;
    try {
      client = createClient();
      TListUsersRequest request = toThriftRequest(criteria);

      TListUsersResponse response = client.listUsers(request);
      if (response.getStatus() == 0) {
        log.debug("Retrieved {} users via Thrift service for criteria", response.getItems().size());

        List<User> users = response.getItems().stream()
            .map(thriftUser -> {
              log.debug("Mapping Thrift user to domain user: {}", thriftUser);
              return toDomain(thriftUser);
            })
            .collect(Collectors.toList());

        log.debug("Mapped {} Thrift users to domain users for criteria", users.size());
        return users;
      } else {
        log.error("Failed to list users by criteria via Thrift service with status {}: {}", response.getStatus(),
            response.getMessage());
        throw new ThriftServiceException("Failed to list users by criteria: " + response.getMessage(),
            "listByCriteria");
      }
    } catch (Exception e) {
      log.error("Failed to list users by criteria via Thrift service: {}", criteria, e);
      throw new ThriftServiceException("Failed to list users by criteria via Thrift service", "listByCriteria", e);
    } finally {
      closeClient(client);
    }
  }

  @Override
  public long countByCriteria(UserQueryCriteria criteria) {
    log.debug("Counting users by criteria via Thrift service using SDK: {}", criteria);
    UserService.Client client = null;
    try {
      client = createClient();
      TListUsersRequest request = toThriftRequest(criteria);

      TListUsersResponse response = client.listUsers(request);
      if (response.getStatus() == 0) {
        long count = response.getTotal();
        log.debug("User count by criteria via Thrift service: {}", count);
        return count;
      } else {
        log.error("Failed to count users by criteria via Thrift service with status {}: {}", response.getStatus(),
            response.getMessage());
        throw new ThriftServiceException("Failed to count users by criteria: " + response.getMessage(),
            "countByCriteria");
      }
    } catch (Exception e) {
      log.error("Failed to count users by criteria via Thrift service: {}", criteria, e);
      throw new ThriftServiceException("Failed to count users by criteria via Thrift service", "countByCriteria", e);
    } finally {
      closeClient(client);
    }
  }

  /**
   * Convert domain query criteria to Thrift request.
   */
  private TListUsersRequest toThriftRequest(UserQueryCriteria criteria) {
    TListUsersRequest request = new TListUsersRequest()
        .setPage(criteria.getPage())
        .setSize(criteria.getSize())
        .setSearch(criteria.getSearch())
        .setIncludeDeleted(criteria.getIncludeDeleted() != null ? criteria.getIncludeDeleted() : false);

    if (criteria.getStatus() != null) {
      request.setStatus(TUserStatus.valueOf(criteria.getStatus().name()));
    }

    if (criteria.getRole() != null) {
      request.setRole(TUserRole.valueOf(criteria.getRole().name()));
    }

    if (criteria.getCreatedAfter() != null) {
      request.setCreatedAfter(criteria.getCreatedAfter().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    if (criteria.getCreatedBefore() != null) {
      request.setCreatedBefore(criteria.getCreatedBefore().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    if (criteria.getSortCriteria() != null && !criteria.getSortCriteria().isEmpty()) {
      List<SortCriterion> thriftSortCriteria = criteria.getSortCriteria().stream()
          .map(sc -> new SortCriterion()
              .setFieldName(sc.getFieldName())
              .setDirection(sc.getDirection()))
          .collect(Collectors.toList());
      request.setSortCriteria(thriftSortCriteria);
    }

    return request;
  }
}
