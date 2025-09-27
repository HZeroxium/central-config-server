package com.example.rest.user.adapter.thrift;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.rest.user.port.ThriftUserClientPort;
import com.example.rest.user.config.ThriftClientProperties;
import com.example.rest.user.config.ConsulThriftClientConfig.ThriftClientFactory;
import com.example.user.thrift.*;
import com.example.common.exception.UserNotFoundException;
import com.example.common.exception.ThriftServiceException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThriftUserClientAdapter implements ThriftUserClientPort {

  private final ThriftClientFactory thriftClientFactory;

  private UserService.Client client() throws Exception {
    return thriftClientFactory.createClient();
  }

  private void closeClient(UserService.Client client) {
    thriftClientFactory.closeClient(client);
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
    log.debug("Pinging Thrift service via service discovery");
    UserService.Client client = null;
    try {
      client = client();
      TPingResponse response = client.ping();
      if (response.getStatus() == 0) {
        log.debug("Thrift service ping successful: {}", response.getResponse());
        return response.getResponse();
      } else {
        log.error("Thrift service ping failed with status {}: {}", response.getStatus(), response.getMessage());
        throw new ThriftServiceException("Thrift service ping failed: " + response.getMessage(), "ping");
      }
    } catch (Exception e) {
      log.error("Thrift service ping failed via service discovery", e);
      throw new ThriftServiceException("Failed to ping Thrift service", "ping", e);
    } finally {
      closeClient(client);
    }
  }

  @Override
  public User create(User user) {
    log.debug("Creating user via Thrift service: {}", user);
    UserService.Client client = null;
    try {
      client = client();
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
    log.debug("Retrieving user by ID via Thrift service: {}", id);
    UserService.Client client = null;
    try {
      client = client();
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
    log.debug("Updating user via Thrift service: {}", user);
    UserService.Client client = null;
    try {
      client = client();
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
    log.debug("Deleting user via Thrift service: {}", id);
    UserService.Client client = null;
    try {
      client = client();
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
    log.debug("Listing users by criteria via Thrift service: {}", criteria);
    UserService.Client client = null;
    try {
      client = client();
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
    log.debug("Counting users by criteria via Thrift service: {}", criteria);
    UserService.Client client = null;
    try {
      client = client();
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
