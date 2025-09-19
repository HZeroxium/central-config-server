package com.example.user.adapter.thrift;

import com.example.common.domain.User;
import com.example.common.domain.UserQueryCriteria;
import com.example.user.service.port.UserServicePort;
import com.example.user.thrift.*;
import com.example.common.exception.DatabaseException;
import com.example.common.exception.UserServiceException;
import com.example.user.adapter.thrift.mapper.UserThriftMapper;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Thrift service handler bridging Thrift-generated API to domain service port.
 * Performs translation between {@link TUser} wire model and domain {@link com.example.common.domain.User}.
 * 
 * Enhanced with comprehensive profiling via @Timed.
 */
@Slf4j
@Component
public class UserServiceHandler implements UserService.Iface {

  private final UserServicePort userService;

  /**
   * Construct handler with a domain service port.
   *
   * @param userService domain service to delegate business logic
   */
  public UserServiceHandler(UserServicePort userService) {
    this.userService = userService;
  }

  // Mappings are delegated to UserThriftMapper

  // New structured API methods

  @Override
  @Timed(value = "thrift.server.ping", description = "Time taken to handle Thrift ping request")
  public TPingResponse ping() throws TException {
    log.debug("Thrift structured ping request received");
    try {
      String response = userService.ping();
      log.debug("Thrift structured ping response: {}", response);

      TPingResponse pingResponse = new TPingResponse();
      pingResponse.setStatus(0); // SUCCESS
      pingResponse.setMessage("Service is healthy");
      pingResponse.setResponse(response);

      return pingResponse;
    } catch (UserServiceException e) {
      log.error("Thrift structured ping failed: {}", e.getMessage(), e);
      TPingResponse pingResponse = new TPingResponse();
      pingResponse.setStatus(1); // SERVICE_ERROR
      pingResponse.setMessage("Service ping failed: " + e.getMessage());
      pingResponse.setResponse("");
      return pingResponse;
    } catch (Exception e) {
      log.error("Unexpected error during Thrift structured ping", e);
      TPingResponse pingResponse = new TPingResponse();
      pingResponse.setStatus(1); // SERVICE_ERROR
      pingResponse.setMessage("Unexpected error during ping: " + e.getMessage());
      pingResponse.setResponse("");
      return pingResponse;
    }
  }

  @Override
  @Timed(value = "thrift.server.create.user", description = "Time taken to handle Thrift create user request")
  public TCreateUserResponse createUser(TCreateUserRequest request) throws TException {
    log.debug("Thrift structured createUser request received: {}", request);
    try {
      User domainUser = User.builder()
          .id(null) // Will be generated
          .name(request.getName())
          .phone(request.getPhone())
          .address(request.getAddress())
          .status(request.getStatus() != null ? User.UserStatus.valueOf(request.getStatus().name()) : User.UserStatus.ACTIVE)
          .role(request.getRole() != null ? User.UserRole.valueOf(request.getRole().name()) : User.UserRole.USER)
          .createdAt(java.time.LocalDateTime.now())
          .createdBy("admin")
          .updatedAt(java.time.LocalDateTime.now())
          .updatedBy("admin")
          .version(null) // Let repository handle version
          .deleted(false)
          .deletedAt(null)
          .deletedBy(null)
          .build();

      User created = userService.create(domainUser);
      log.info("User created via Thrift structured API with ID: {}", created.getId());

      TUser thriftUser = UserThriftMapper.toThrift(created);
      TCreateUserResponse response = new TCreateUserResponse();
      response.setStatus(0); // SUCCESS
      response.setMessage("User created successfully");
      response.setUser(thriftUser);

      return response;
    } catch (DatabaseException e) {
      log.error("Database error during user creation: {}", e.getMessage(), e);
      TCreateUserResponse response = new TCreateUserResponse();
      response.setStatus(2); // DATABASE_ERROR
      response.setMessage("Database error during user creation: " + e.getMessage());
      response.setUser(null);
      return response;
    } catch (UserServiceException e) {
      log.error("Service error during user creation: {}", e.getMessage(), e);
      TCreateUserResponse response = new TCreateUserResponse();
      response.setStatus(1); // VALIDATION_ERROR
      response.setMessage("Service error during user creation: " + e.getMessage());
      response.setUser(null);
      return response;
    } catch (Exception e) {
      log.error("Unexpected error during user creation: {}", e.getMessage(), e);
      TCreateUserResponse response = new TCreateUserResponse();
      response.setStatus(100); // INTERNAL_SERVER_ERROR
      response.setMessage("Unexpected error during user creation: " + e.getMessage());
      response.setUser(null);
      return response;
    }
  }

  @Override
  @Timed(value = "thrift.server.get.user", description = "Time taken to handle Thrift get user request")
  public TGetUserResponse getUser(TGetUserRequest request) throws TException {
    log.debug("Thrift structured getUser request received for ID: {}", request.getId());
    try {
      return userService.getById(request.getId())
          .map(user -> {
            log.debug("User found via Thrift structured API: {}", user);
            TUser thriftUser = UserThriftMapper.toThrift(user);
            TGetUserResponse response = new TGetUserResponse();
            response.setStatus(0); // SUCCESS
            response.setMessage("User retrieved successfully");
            response.setUser(thriftUser);
            return response;
          })
          .orElseGet(() -> {
            log.debug("User not found via Thrift structured API for ID: {}", request.getId());
            TGetUserResponse response = new TGetUserResponse();
            response.setStatus(2); // USER_NOT_FOUND
            response.setMessage("User not found");
            response.setUser(null);
            return response;
          });
    } catch (DatabaseException e) {
      log.error("Database error during user retrieval: {}", e.getMessage(), e);
      TGetUserResponse response = new TGetUserResponse();
      response.setStatus(101); // DATABASE_ERROR
      response.setMessage("Database error during user retrieval: " + e.getMessage());
      response.setUser(null);
      return response;
    } catch (Exception e) {
      log.error("Unexpected error during user retrieval: {}", e.getMessage(), e);
      TGetUserResponse response = new TGetUserResponse();
      response.setStatus(100); // INTERNAL_SERVER_ERROR
      response.setMessage("Unexpected error during user retrieval: " + e.getMessage());
      response.setUser(null);
      return response;
    }
  }

  @Override
  @Timed(value = "thrift.server.update.user", description = "Time taken to handle Thrift update user request")
  public TUpdateUserResponse updateUser(TUpdateUserRequest request) throws TException {
    log.debug("Thrift structured updateUser request received: {}", request);
    try {
      // Check if user exists first
      Optional<User> existingUser = userService.getById(request.getId());
      if (existingUser.isEmpty()) {
        log.debug("User not found for update via Thrift structured API for ID: {}", request.getId());
        TUpdateUserResponse response = new TUpdateUserResponse();
        response.setStatus(1); // USER_NOT_FOUND
        response.setMessage("User not found");
        response.setUser(null);
        return response;
      }

      User domainUser = User.builder()
          .id(request.getId())
          .name(request.getName())
          .phone(request.getPhone())
          .address(request.getAddress())
          .status(request.getStatus() != null ? User.UserStatus.valueOf(request.getStatus().name()) : User.UserStatus.ACTIVE)
          .role(request.getRole() != null ? User.UserRole.valueOf(request.getRole().name()) : User.UserRole.USER)
          .createdAt(existingUser.get().getCreatedAt()) // Preserve existing values
          .createdBy(existingUser.get().getCreatedBy())
          .updatedAt(java.time.LocalDateTime.now())
          .updatedBy("admin")
          .version(request.getVersion() > 0 ? request.getVersion() : existingUser.get().getVersion())
          .deleted(false)
          .deletedAt(null)
          .deletedBy(null)
          .build();

      User updated = userService.update(domainUser);
      log.info("User updated via Thrift structured API with ID: {}", updated.getId());

      TUser thriftUser = UserThriftMapper.toThrift(updated);
      TUpdateUserResponse response = new TUpdateUserResponse();
      response.setStatus(0); // SUCCESS
      response.setMessage("User updated successfully");
      response.setUser(thriftUser);

      return response;
    } catch (DatabaseException e) {
      log.error("Database error during user update: {}", e.getMessage(), e);
      TUpdateUserResponse response = new TUpdateUserResponse();
      response.setStatus(3); // DATABASE_ERROR
      response.setMessage("Database error during user update: " + e.getMessage());
      response.setUser(null);
      return response;
    } catch (UserServiceException e) {
      log.error("Service error during user update: {}", e.getMessage(), e);
      TUpdateUserResponse response = new TUpdateUserResponse();
      response.setStatus(2); // VALIDATION_ERROR
      response.setMessage("Service error during user update: " + e.getMessage());
      response.setUser(null);
      return response;
    } catch (Exception e) {
      log.error("Unexpected error during user update: {}", e.getMessage(), e);
      TUpdateUserResponse response = new TUpdateUserResponse();
      response.setStatus(100); // INTERNAL_SERVER_ERROR
      response.setMessage("Unexpected error during user update: " + e.getMessage());
      response.setUser(null);
      return response;
    }
  }

  @Override
  @Timed(value = "thrift.server.delete.user", description = "Time taken to handle Thrift delete user request")
  public TDeleteUserResponse deleteUser(TDeleteUserRequest request) throws TException {
    log.debug("Thrift structured deleteUser request received for ID: {}", request.getId());
    try {
      // Check if user exists first
      Optional<User> existingUser = userService.getById(request.getId());
      if (existingUser.isEmpty()) {
        log.debug("User not found for deletion via Thrift structured API for ID: {}", request.getId());
        TDeleteUserResponse response = new TDeleteUserResponse();
        response.setStatus(1); // USER_NOT_FOUND
        response.setMessage("User not found");
        return response;
      }

      userService.delete(request.getId());
      log.info("User deleted via Thrift structured API with ID: {}", request.getId());

      TDeleteUserResponse response = new TDeleteUserResponse();
      response.setStatus(0); // SUCCESS
      response.setMessage("User deleted successfully");
      return response;
    } catch (DatabaseException e) {
      log.error("Database error during user deletion: {}", e.getMessage(), e);
      TDeleteUserResponse response = new TDeleteUserResponse();
      response.setStatus(2); // DATABASE_ERROR
      response.setMessage("Database error during user deletion: " + e.getMessage());
      return response;
    } catch (Exception e) {
      log.error("Unexpected error during user deletion: {}", e.getMessage(), e);
      TDeleteUserResponse response = new TDeleteUserResponse();
      response.setStatus(100); // INTERNAL_SERVER_ERROR
      response.setMessage("Unexpected error during user deletion: " + e.getMessage());
      return response;
    }
  }

  @Override
  @Timed(value = "thrift.server.list.users", description = "Time taken to handle Thrift list users request")
  public TListUsersResponse listUsers(TListUsersRequest request) throws TException {
    log.debug("Thrift structured listUsers request received - page: {}, size: {}", request.getPage(), request.getSize());
    try {
      // Convert Thrift request to domain criteria
      UserQueryCriteria criteria = UserThriftMapper.toQueryCriteria(request);
      log.debug("Converted Thrift request to query criteria: {}", criteria);

      List<User> users = userService.listByCriteria(UserThriftMapper.toQueryCriteria(request));
      log.debug("Retrieved {} users from service for Thrift structured API", users.size());

      long total = userService.countByCriteria(criteria);
      log.debug("Total user count for Thrift structured API: {}", total);

      int totalPages = (int) Math.ceil((double) total / (double) request.getSize());
      List<TUser> thriftUsers = users.stream()
          .map(UserThriftMapper::toThrift)
          .collect(Collectors.toList());

      TListUsersResponse response = new TListUsersResponse();
      response.setStatus(0); // SUCCESS
      response.setMessage("Users retrieved successfully");
      response.setItems(thriftUsers); 
      response.setPage(request.getPage());
      response.setSize(request.getSize());
      response.setTotal(total);
      response.setTotalPages(totalPages);

      log.debug("Thrift structured listUsers response - items: {}, page: {}, size: {}, total: {}, totalPages: {}", 
                thriftUsers.size(), request.getPage(), request.getSize(), total, totalPages);
      return response;
    } catch (DatabaseException e) {
      log.error("Database error during user listing: {}", e.getMessage(), e);
      TListUsersResponse response = new TListUsersResponse();
      response.setStatus(2); // DATABASE_ERROR
      response.setMessage("Database error during user listing: " + e.getMessage());
      response.setItems(List.of());
      response.setPage(request.getPage());
      response.setSize(request.getSize());
      response.setTotal(0);
      response.setTotalPages(0);
      return response;
    } catch (Exception e) {
      log.error("Unexpected error during user listing: {}", e.getMessage(), e);
      TListUsersResponse response = new TListUsersResponse();
      response.setStatus(100); // INTERNAL_SERVER_ERROR
      response.setMessage("Unexpected error during user listing: " + e.getMessage());
      response.setItems(List.of());
      response.setPage(request.getPage());
      response.setSize(request.getSize());
      response.setTotal(0);
      response.setTotalPages(0);
      return response;
    }
  }

  // Legacy API removed
}
