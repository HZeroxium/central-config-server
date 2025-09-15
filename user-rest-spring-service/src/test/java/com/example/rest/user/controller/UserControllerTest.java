package com.example.rest.user.controller;

import com.example.rest.user.domain.User;
import com.example.rest.user.dto.*;
import com.example.rest.user.mapper.UserMapper;
import com.example.rest.user.service.UserService;
import com.example.rest.metrics.ApplicationMetrics;
import com.example.rest.exception.ThriftServiceException;
import com.example.rest.exception.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for UserController class.
 * Tests all REST endpoints with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ApplicationMetrics metrics;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        meterRegistry = new SimpleMeterRegistry();
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        
        // Use real metrics for more accurate testing
        when(metrics.startRestApiTimer()).thenReturn(Timer.start(meterRegistry));
        // Only mock startThriftClientTimer when needed in specific tests
    }

    @Nested
    @DisplayName("Ping Endpoint Tests")
    class PingEndpointTests {

        @Test
        @DisplayName("Should ping successfully")
        void shouldPingSuccessfully() throws Exception {
            // Given
            String expectedResponse = "pong";
            when(userService.ping()).thenReturn(expectedResponse);

            // When & Then
            mockMvc.perform(get("/users/ping"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedResponse));

            verify(userService).ping();
            verify(metrics).incrementRestApiRequests("/users/ping", "GET");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/ping"), eq("GET"));
        }

        @Test
        @DisplayName("Should handle ping failure")
        void shouldHandlePingFailure() throws Exception {
            // Given
            when(userService.ping()).thenThrow(new ThriftServiceException("Thrift service unavailable", "ping", new RuntimeException()));

            // When & Then - Controller re-throws exceptions, so we expect 500
            mockMvc.perform(get("/users/ping"))
                    .andExpect(status().isInternalServerError());

            verify(userService).ping();
            verify(metrics).incrementRestApiRequests("/users/ping", "GET");
            verify(metrics).incrementRestApiErrors("/users/ping", "GET", "ThriftServiceException");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/ping"), eq("GET"));
        }
    }

    @Nested
    @DisplayName("Create User Endpoint Tests")
    class CreateUserEndpointTests {

        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() throws Exception {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("John Doe");
            request.setPhone("+1-555-123-4567");
            request.setAddress("123 Main St");

            User domainUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userService.create(any(User.class))).thenReturn(domainUser);

            // When & Then
            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(StatusCode.SUCCESS))
                    .andExpect(jsonPath("$.message").value("User created successfully"))
                    .andExpect(jsonPath("$.user.id").value("user-123"))
                    .andExpect(jsonPath("$.user.name").value("John Doe"))
                    .andExpect(jsonPath("$.user.phone").value("+1-555-123-4567"))
                    .andExpect(jsonPath("$.user.address").value("123 Main St"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.correlationId").exists());

            verify(userService).create(any(User.class));
            verify(metrics).incrementRestApiRequests("/users", "POST");
            verify(metrics).incrementUserOperations("create");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users"), eq("POST"));
        }

        @Test
        @DisplayName("Should handle create user failure")
        void shouldHandleCreateUserFailure() throws Exception {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("John Doe");
            request.setPhone("+1-555-123-4567");

            when(userService.create(any(User.class))).thenThrow(new ThriftServiceException("Create failed", "create", new RuntimeException()));

            // When & Then - Controller re-throws exceptions, so we expect 500
            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());

            verify(userService).create(any(User.class));
            verify(metrics).incrementRestApiRequests("/users", "POST");
            verify(metrics).incrementUserOperations("create");
            verify(metrics).incrementRestApiErrors("/users", "POST", "ThriftServiceException");
            verify(metrics).incrementUserOperationsErrors("create", "ThriftServiceException");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users"), eq("POST"));
        }

        @Test
        @DisplayName("Should handle validation errors")
        void shouldHandleValidationErrors() throws Exception {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            // Missing required fields

            // When & Then
            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get User Endpoint Tests")
    class GetUserEndpointTests {

        @Test
        @DisplayName("Should get user successfully")
        void shouldGetUserSuccessfully() throws Exception {
            // Given
            String userId = "user-123";
            User domainUser = User.builder()
                    .id(userId)
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userService.getById(userId)).thenReturn(Optional.of(domainUser));

            // When & Then
            mockMvc.perform(get("/users/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(StatusCode.SUCCESS))
                    .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                    .andExpect(jsonPath("$.user.id").value(userId))
                    .andExpect(jsonPath("$.user.name").value("John Doe"))
                    .andExpect(jsonPath("$.user.phone").value("+1-555-123-4567"))
                    .andExpect(jsonPath("$.user.address").value("123 Main St"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.correlationId").exists());

            verify(userService).getById(userId);
            verify(metrics).incrementRestApiRequests("/users/{id}", "GET");
            verify(metrics).incrementUserOperations("get");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/{id}"), eq("GET"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            String userId = "non-existent-user";
            when(userService.getById(userId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/users/{id}", userId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(StatusCode.USER_NOT_FOUND))
                    .andExpect(jsonPath("$.message").value("User not found"))
                    .andExpect(jsonPath("$.user").isEmpty())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.correlationId").exists());

            verify(userService).getById(userId);
            verify(metrics).incrementRestApiRequests("/users/{id}", "GET");
            verify(metrics).incrementUserOperations("get");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/{id}"), eq("GET"));
        }

        @Test
        @DisplayName("Should handle get user failure")
        void shouldHandleGetUserFailure() throws Exception {
            // Given
            String userId = "user-123";
            when(userService.getById(userId)).thenThrow(new ThriftServiceException("Get failed", "getById", new RuntimeException()));

            // When & Then - Controller re-throws exceptions, so we expect 500
            mockMvc.perform(get("/users/{id}", userId))
                    .andExpect(status().isInternalServerError());

            verify(userService).getById(userId);
            verify(metrics).incrementRestApiRequests("/users/{id}", "GET");
            verify(metrics).incrementUserOperations("get");
            verify(metrics).incrementRestApiErrors("/users/{id}", "GET", "ThriftServiceException");
            verify(metrics).incrementUserOperationsErrors("get", "ThriftServiceException");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/{id}"), eq("GET"));
        }
    }

    @Nested
    @DisplayName("Update User Endpoint Tests")
    class UpdateUserEndpointTests {

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() throws Exception {
            // Given
            String userId = "user-123";
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("John Doe Updated");
            request.setPhone("+1-555-123-4567");
            request.setAddress("456 Oak Ave");

            User existingUser = User.builder()
                    .id(userId)
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            User updatedUser = User.builder()
                    .id(userId)
                    .name("John Doe Updated")
                    .phone("+1-555-123-4567")
                    .address("456 Oak Ave")
                    .build();

            when(userService.getById(userId)).thenReturn(Optional.of(existingUser));
            when(userService.update(any(User.class))).thenReturn(updatedUser);

            // When & Then
            mockMvc.perform(put("/users/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(StatusCode.SUCCESS))
                    .andExpect(jsonPath("$.message").value("User updated successfully"))
                    .andExpect(jsonPath("$.user.id").value(userId))
                    .andExpect(jsonPath("$.user.name").value("John Doe Updated"))
                    .andExpect(jsonPath("$.user.phone").value("+1-555-123-4567"))
                    .andExpect(jsonPath("$.user.address").value("456 Oak Ave"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.correlationId").exists());

            verify(userService).getById(userId);
            verify(userService).update(any(User.class));
            verify(metrics).incrementRestApiRequests("/users/{id}", "PUT");
            verify(metrics).incrementUserOperations("update");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/{id}"), eq("PUT"));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent user")
        void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
            // Given
            String userId = "non-existent-user";
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("John Doe Updated");
            request.setPhone("+1-555-123-4567");

            when(userService.getById(userId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(put("/users/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(StatusCode.USER_NOT_FOUND))
                    .andExpect(jsonPath("$.message").value("User not found"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.correlationId").exists());

            verify(userService).getById(userId);
            verify(userService, never()).update(any(User.class));
            verify(metrics).incrementRestApiRequests("/users/{id}", "PUT");
            verify(metrics).incrementUserOperations("update");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/{id}"), eq("PUT"));
        }

        @Test
        @DisplayName("Should handle update user failure")
        void shouldHandleUpdateUserFailure() throws Exception {
            // Given
            String userId = "user-123";
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("John Doe Updated");
            request.setPhone("+1-555-123-4567");

            User existingUser = User.builder()
                    .id(userId)
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            when(userService.getById(userId)).thenReturn(Optional.of(existingUser));
            when(userService.update(any(User.class))).thenThrow(new ThriftServiceException("Update failed", "update", new RuntimeException()));

            // When & Then - Controller re-throws exceptions, so we expect 500
            mockMvc.perform(put("/users/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());

            verify(userService).getById(userId);
            verify(userService).update(any(User.class));
            verify(metrics).incrementRestApiRequests("/users/{id}", "PUT");
            verify(metrics).incrementUserOperations("update");
            verify(metrics).incrementRestApiErrors("/users/{id}", "PUT", "ThriftServiceException");
            verify(metrics).incrementUserOperationsErrors("update", "ThriftServiceException");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/{id}"), eq("PUT"));
        }
    }

    @Nested
    @DisplayName("Delete User Endpoint Tests")
    class DeleteUserEndpointTests {

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() throws Exception {
            // Given
            String userId = "user-123";
            User existingUser = User.builder()
                    .id(userId)
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            when(userService.getById(userId)).thenReturn(Optional.of(existingUser));
            doNothing().when(userService).delete(userId);

            // When & Then
            mockMvc.perform(delete("/users/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(StatusCode.SUCCESS))
                    .andExpect(jsonPath("$.message").value("User deleted successfully"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.correlationId").exists());

            verify(userService).getById(userId);
            verify(userService).delete(userId);
            verify(metrics).incrementRestApiRequests("/users/{id}", "DELETE");
            verify(metrics).incrementUserOperations("delete");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/{id}"), eq("DELETE"));
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent user")
        void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
            // Given
            String userId = "non-existent-user";
            when(userService.getById(userId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(delete("/users/{id}", userId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(StatusCode.USER_NOT_FOUND))
                    .andExpect(jsonPath("$.message").value("User not found"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.correlationId").exists());

            verify(userService).getById(userId);
            verify(userService, never()).delete(anyString());
            verify(metrics).incrementRestApiRequests("/users/{id}", "DELETE");
            verify(metrics).incrementUserOperations("delete");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/{id}"), eq("DELETE"));
        }

        @Test
        @DisplayName("Should handle delete user failure")
        void shouldHandleDeleteUserFailure() throws Exception {
            // Given
            String userId = "user-123";
            User existingUser = User.builder()
                    .id(userId)
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            when(userService.getById(userId)).thenReturn(Optional.of(existingUser));
            doThrow(new ThriftServiceException("Delete failed", "delete", new RuntimeException()))
                    .when(userService).delete(userId);

            // When & Then - Controller re-throws exceptions, so we expect 500
            mockMvc.perform(delete("/users/{id}", userId))
                    .andExpect(status().isInternalServerError());

            verify(userService).getById(userId);
            verify(userService).delete(userId);
            verify(metrics).incrementRestApiRequests("/users/{id}", "DELETE");
            verify(metrics).incrementUserOperations("delete");
            verify(metrics).incrementRestApiErrors("/users/{id}", "DELETE", "ThriftServiceException");
            verify(metrics).incrementUserOperationsErrors("delete", "ThriftServiceException");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users/{id}"), eq("DELETE"));
        }
    }

    @Nested
    @DisplayName("List Users Endpoint Tests")
    class ListUsersEndpointTests {

        @Test
        @DisplayName("Should list users successfully with default pagination")
        void shouldListUsersSuccessfullyWithDefaultPagination() throws Exception {
            // Given
            List<User> users = Arrays.asList(
                    User.builder().id("user-1").name("John Doe").phone("+1-555-123-4567").build(),
                    User.builder().id("user-2").name("Jane Smith").phone("+1-555-987-6543").build()
            );

            when(userService.listPaged(0, 20)).thenReturn(users);
            when(userService.count()).thenReturn(2L);

            // When & Then
            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(StatusCode.SUCCESS))
                    .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items.length()").value(2))
                    .andExpect(jsonPath("$.items[0].id").value("user-1"))
                    .andExpect(jsonPath("$.items[0].name").value("John Doe"))
                    .andExpect(jsonPath("$.items[1].id").value("user-2"))
                    .andExpect(jsonPath("$.items[1].name").value("Jane Smith"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.total").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.correlationId").exists());

            verify(userService).listPaged(0, 20);
            verify(userService).count();
            verify(metrics).incrementRestApiRequests("/users", "GET");
            verify(metrics).incrementUserOperations("list");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users"), eq("GET"));
        }

        @Test
        @DisplayName("Should list users successfully with custom pagination")
        void shouldListUsersSuccessfullyWithCustomPagination() throws Exception {
            // Given
            int page = 1;
            int size = 5;
            List<User> users = Arrays.asList(
                    User.builder().id("user-6").name("User 6").phone("+1-555-111-1111").build()
            );

            when(userService.listPaged(page, size)).thenReturn(users);
            when(userService.count()).thenReturn(10L);

            // When & Then
            mockMvc.perform(get("/users")
                    .param("page", String.valueOf(page))
                    .param("size", String.valueOf(size)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(StatusCode.SUCCESS))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.page").value(page))
                    .andExpect(jsonPath("$.size").value(size))
                    .andExpect(jsonPath("$.total").value(10))
                    .andExpect(jsonPath("$.totalPages").value(2));

            verify(userService).listPaged(page, size);
            verify(userService).count();
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsersExist() throws Exception {
            // Given
            when(userService.listPaged(0, 20)).thenReturn(Arrays.asList());
            when(userService.count()).thenReturn(0L);

            // When & Then
            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(StatusCode.SUCCESS))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items.length()").value(0))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));

            verify(userService).listPaged(0, 20);
            verify(userService).count();
        }

        @Test
        @DisplayName("Should handle list users failure")
        void shouldHandleListUsersFailure() throws Exception {
            // Given
            when(userService.listPaged(0, 20)).thenThrow(new ThriftServiceException("List failed", "listPaged", new RuntimeException()));

            // When & Then - Controller re-throws exceptions, so we expect 500
            mockMvc.perform(get("/users"))
                    .andExpect(status().isInternalServerError());

            verify(userService).listPaged(0, 20);
            verify(metrics).incrementRestApiRequests("/users", "GET");
            verify(metrics).incrementUserOperations("list");
            verify(metrics).incrementRestApiErrors("/users", "GET", "ThriftServiceException");
            verify(metrics).incrementUserOperationsErrors("list", "ThriftServiceException");
            verify(metrics).recordRestApiDuration(any(Timer.Sample.class), eq("/users"), eq("GET"));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle very large page numbers")
        void shouldHandleVeryLargePageNumbers() throws Exception {
            // Given
            int largePage = Integer.MAX_VALUE;
            int size = 10;
            when(userService.listPaged(largePage, size)).thenReturn(Arrays.asList());
            when(userService.count()).thenReturn(0L);

            // When & Then
            mockMvc.perform(get("/users")
                    .param("page", String.valueOf(largePage))
                    .param("size", String.valueOf(size)))
                    .andExpect(status().isOk());

            verify(userService).listPaged(largePage, size);
        }

        @Test
        @DisplayName("Should handle very large size values")
        void shouldHandleVeryLargeSizeValues() throws Exception {
            // Given
            int page = 0;
            int largeSize = Integer.MAX_VALUE;
            when(userService.listPaged(page, largeSize)).thenReturn(Arrays.asList());
            when(userService.count()).thenReturn(0L);

            // When & Then
            mockMvc.perform(get("/users")
                    .param("page", String.valueOf(page))
                    .param("size", String.valueOf(largeSize)))
                    .andExpect(status().isOk());

            verify(userService).listPaged(page, largeSize);
        }

        @Test
        @DisplayName("Should handle negative page numbers")
        void shouldHandleNegativePageNumbers() throws Exception {
            // Given
            int negativePage = -1;
            int size = 10;
            when(userService.listPaged(negativePage, size)).thenReturn(Arrays.asList());
            when(userService.count()).thenReturn(0L);

            // When & Then
            mockMvc.perform(get("/users")
                    .param("page", String.valueOf(negativePage))
                    .param("size", String.valueOf(size)))
                    .andExpect(status().isOk());

            verify(userService).listPaged(negativePage, size);
        }

        @Test
        @DisplayName("Should handle zero size values")
        void shouldHandleZeroSizeValues() throws Exception {
            // Given
            int page = 0;
            int zeroSize = 0;
            when(userService.listPaged(page, zeroSize)).thenReturn(Arrays.asList());
            when(userService.count()).thenReturn(0L);

            // When & Then
            mockMvc.perform(get("/users")
                    .param("page", String.valueOf(page))
                    .param("size", String.valueOf(zeroSize)))
                    .andExpect(status().isOk());

            verify(userService).listPaged(page, zeroSize);
        }
    }
}
