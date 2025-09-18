package com.example.rest.user.controller;

import com.example.rest.user.domain.User;
import com.example.rest.user.dto.*;
import com.example.rest.user.service.UserService;
import com.example.rest.exception.ThriftServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
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


    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        
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
       
        }
    }

    
}
