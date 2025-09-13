package com.example.user.adapter.thrift;

import com.example.user.domain.User;
import com.example.user.service.port.UserServicePort;
import com.example.user.thrift.*;
import com.example.user.exception.DatabaseException;
import com.example.user.exception.UserServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Thrift service handler bridging Thrift-generated API to domain service port.
 * Performs translation between {@link TUser} wire model and domain {@link com.example.user.domain.User}.
 */
@Slf4j
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

  /** Convert domain to Thrift DTO. */
  private static TUser toThrift(User user) {
    TUser t = new TUser();
    t.setId(user.getId());
    t.setName(user.getName());
    t.setPhone(user.getPhone());
    t.setAddress(user.getAddress());
    return t;
  }

  /** Convert Thrift DTO to domain. */
  private static User toDomain(TUser t) {
    return User.builder()
        .id(t.getId())
        .name(t.getName())
        .phone(t.getPhone())
        .address(t.getAddress())
        .build();
  }



  // New structured API methods

  @Override
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
  public TCreateUserResponse createUser(TCreateUserRequest request) throws TException {
    log.debug("Thrift structured createUser request received: {}", request);
    try {
      User domainUser = User.builder()
          .id(null) // Will be generated
          .name(request.getName())
          .phone(request.getPhone())
          .address(request.getAddress())
          .build();
      
      User created = userService.create(domainUser);
      log.info("User created via Thrift structured API with ID: {}", created.getId());
      
      TUser thriftUser = toThrift(created);
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
  public TGetUserResponse getUser(TGetUserRequest request) throws TException {
    log.debug("Thrift structured getUser request received for ID: {}", request.getId());
    try {
      return userService.getById(request.getId())
          .map(user -> {
            log.debug("User found via Thrift structured API: {}", user);
            TUser thriftUser = toThrift(user);
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
          .build();
      
      User updated = userService.update(domainUser);
      log.info("User updated via Thrift structured API with ID: {}", updated.getId());
      
      TUser thriftUser = toThrift(updated);
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
  public TListUsersResponse listUsers(TListUsersRequest request) throws TException {
    log.debug("Thrift structured listUsers request received - page: {}, size: {}", request.getPage(), request.getSize());
    try {
      List<User> users = userService.listPaged(request.getPage(), request.getSize());
      log.debug("Retrieved {} users from service for Thrift structured API", users.size());
      
      long total = userService.count();
      log.debug("Total user count for Thrift structured API: {}", total);
      
      int totalPages = (int) Math.ceil((double) total / (double) request.getSize());
      List<TUser> thriftUsers = users.stream()
          .map(user -> {
            log.debug("Mapping domain user to Thrift user: {}", user);
            return toThrift(user);
          })
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

  // Legacy API methods for backward compatibility

  @Override
  public String pingLegacy() throws TException {
    log.debug("Thrift legacy ping request received");
    try {
      String response = userService.ping();
      log.debug("Thrift legacy ping response: {}", response);
      return response;
    } catch (UserServiceException e) {
      log.error("Thrift legacy ping failed: {}", e.getMessage(), e);
      throw new TException("Service ping failed: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during Thrift legacy ping", e);
      throw new TException("Unexpected error during ping: " + e.getMessage(), e);
    }
  }

  @Override
  public TUser createUserLegacy(TUser user) throws TException {
    log.debug("Thrift legacy createUser request received: {}", user);
    try {
      User domainUser = toDomain(user);
      log.debug("Mapped Thrift user to domain user: {}", domainUser);
      
      User created = userService.create(domainUser);
      log.info("User created via Thrift legacy with ID: {}", created.getId());
      
      TUser thriftUser = toThrift(created);
      log.debug("Mapped domain user to Thrift user: {}", thriftUser);
      return thriftUser;
    } catch (DatabaseException e) {
      log.error("Database error during user creation: {}", e.getMessage(), e);
      throw new TException("Database error during user creation: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user creation: {}", e.getMessage(), e);
      throw new TException("Service error during user creation: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user creation: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user creation: " + e.getMessage(), e);
    }
  }

  @Override
  public TUser getUserLegacy(String id) throws TException {
    log.debug("Thrift legacy getUser request received for ID: {}", id);
    try {
      return userService.getById(id)
          .map(user -> {
            log.debug("User found via Thrift legacy: {}", user);
            TUser thriftUser = toThrift(user);
            log.debug("Mapped domain user to Thrift user: {}", thriftUser);
            return thriftUser;
          })
          .orElseGet(() -> {
            log.debug("User not found via Thrift legacy for ID: {}", id);
            return null;
          });
    } catch (DatabaseException e) {
      log.error("Database error during user retrieval: {}", e.getMessage(), e);
      throw new TException("Database error during user retrieval: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user retrieval: {}", e.getMessage(), e);
      throw new TException("Service error during user retrieval: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user retrieval: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user retrieval: " + e.getMessage(), e);
    }
  }

  @Override
  public TUser updateUserLegacy(TUser user) throws TException {
    log.debug("Thrift legacy updateUser request received: {}", user);
    try {
      User domainUser = toDomain(user);
      log.debug("Mapped Thrift user to domain user: {}", domainUser);
      
      User updated = userService.update(domainUser);
      log.info("User updated via Thrift legacy with ID: {}", updated.getId());
      
      TUser thriftUser = toThrift(updated);
      log.debug("Mapped domain user to Thrift user: {}", thriftUser);
      return thriftUser;
    } catch (DatabaseException e) {
      log.error("Database error during user update: {}", e.getMessage(), e);
      throw new TException("Database error during user update: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user update: {}", e.getMessage(), e);
      throw new TException("Service error during user update: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user update: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user update: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteUserLegacy(String id) throws TException {
    log.debug("Thrift legacy deleteUser request received for ID: {}", id);
    try {
      userService.delete(id);
      log.info("User deleted via Thrift legacy with ID: {}", id);
    } catch (DatabaseException e) {
      log.error("Database error during user deletion: {}", e.getMessage(), e);
      throw new TException("Database error during user deletion: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user deletion: {}", e.getMessage(), e);
      throw new TException("Service error during user deletion: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user deletion: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user deletion: " + e.getMessage(), e);
    }
  }

  @Override
  public TPagedUsers listUsersLegacy(int page, int size) throws TException {
    log.debug("Thrift legacy listUsers request received - page: {}, size: {}", page, size);
    try {
      List<User> users = userService.listPaged(page, size);
      log.debug("Retrieved {} users from service for Thrift legacy", users.size());
      
      long total = userService.count();
      log.debug("Total user count for Thrift legacy: {}", total);
      
      int totalPages = (int) Math.ceil((double) total / (double) size);
      List<TUser> thriftUsers = users.stream()
          .map(user -> {
            log.debug("Mapping domain user to Thrift user: {}", user);
            return toThrift(user);
          })
          .collect(Collectors.toList());
      
      TPagedUsers res = new TPagedUsers();
      res.setItems(thriftUsers);
      res.setPage(page);
      res.setSize(size);
      res.setTotal(total);
      res.setTotalPages(totalPages);
      
      log.debug("Thrift legacy listUsers response - items: {}, page: {}, size: {}, total: {}, totalPages: {}", 
                thriftUsers.size(), page, size, total, totalPages);
      return res;
    } catch (DatabaseException e) {
      log.error("Database error during user listing: {}", e.getMessage(), e);
      throw new TException("Database error during user listing: " + e.getMessage(), e);
    } catch (UserServiceException e) {
      log.error("Service error during user listing: {}", e.getMessage(), e);
      throw new TException("Service error during user listing: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error during user listing: {}", e.getMessage(), e);
      throw new TException("Unexpected error during user listing: " + e.getMessage(), e);
    }
  }
}
