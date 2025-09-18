package com.example.user.adapter.thrift;

import com.example.user.domain.User;
import com.example.user.exception.DatabaseException;
import com.example.user.exception.UserServiceException;
import com.example.user.service.port.UserServicePort;
import com.example.user.thrift.*;
 

import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for UserServiceHandler.
 * Tests all Thrift service methods, error handling, metrics collection, and data mapping.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceHandler Tests")
class UserServiceHandlerTest {

    @Mock
    private UserServicePort userService;

    

    private UserServiceHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UserServiceHandler(userService);
    }

    @Nested
    @DisplayName("Ping Tests")
    class PingTests {

        @Test
        @DisplayName("Should handle ping successfully")
        void shouldHandlePingSuccessfully() throws TException {
            // Given
            String expectedResponse = "pong";
            when(userService.ping()).thenReturn(expectedResponse);
            

            // When
            TPingResponse response = handler.ping();

            // Then
            assertThat(response.getStatus()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("Service is healthy");
            assertThat(response.getResponse()).isEqualTo(expectedResponse);
            
            verify(userService).ping();
            
        }

        @Test
        @DisplayName("Should handle ping with UserServiceException")
        void shouldHandlePingWithUserServiceException() throws TException {
            // Given
            UserServiceException exception = new UserServiceException("SERVICE_ERROR", "Service unavailable");
            when(userService.ping()).thenThrow(exception);
            

            // When
            TPingResponse response = handler.ping();

            // Then
            assertThat(response.getStatus()).isEqualTo(1);
            assertThat(response.getMessage()).isEqualTo("Service ping failed: Service unavailable");
            assertThat(response.getResponse()).isEmpty();
            
            
        }

        @Test
        @DisplayName("Should handle ping with unexpected exception")
        void shouldHandlePingWithUnexpectedException() throws TException {
            // Given
            RuntimeException exception = new RuntimeException("Unexpected error");
            when(userService.ping()).thenThrow(exception);
            

            // When
            TPingResponse response = handler.ping();

            // Then
            assertThat(response.getStatus()).isEqualTo(1);
            assertThat(response.getMessage()).isEqualTo("Unexpected error during ping: Unexpected error");
            assertThat(response.getResponse()).isEmpty();
            
            
        }
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() throws TException {
            // Given
            TCreateUserRequest request = new TCreateUserRequest()
                    .setName("John Doe")
                    .setPhone("+1-555-123-4567")
                    .setAddress("123 Main St");

            User createdUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userService.create(any(User.class))).thenReturn(createdUser);
            

            // When
            TCreateUserResponse response = handler.createUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("User created successfully");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getId()).isEqualTo("user-123");
            assertThat(response.getUser().getName()).isEqualTo("John Doe");
            assertThat(response.getUser().getPhone()).isEqualTo("+1-555-123-4567");
            assertThat(response.getUser().getAddress()).isEqualTo("123 Main St");
            
            verify(userService).create(any(User.class));
            
        }

        @Test
        @DisplayName("Should handle database error during create")
        void shouldHandleDatabaseErrorDuringCreate() throws TException {
            // Given
            TCreateUserRequest request = new TCreateUserRequest()
                    .setName("John Doe")
                    .setPhone("+1-555-123-4567")
                    .setAddress("123 Main St");

            DatabaseException exception = new DatabaseException("Database connection failed");
            when(userService.create(any(User.class))).thenThrow(exception);
            

            // When
            TCreateUserResponse response = handler.createUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(2);
            assertThat(response.getMessage()).isEqualTo("Database error during user creation: Database connection failed");
            assertThat(response.getUser()).isNull();
            
            
        }

        @Test
        @DisplayName("Should handle service error during create")
        void shouldHandleServiceErrorDuringCreate() throws TException {
            // Given
            TCreateUserRequest request = new TCreateUserRequest()
                    .setName("John Doe")
                    .setPhone("+1-555-123-4567")
                    .setAddress("123 Main St");

            UserServiceException exception = new UserServiceException("VALIDATION_ERROR", "Invalid user data");
            when(userService.create(any(User.class))).thenThrow(exception);
            

            // When
            TCreateUserResponse response = handler.createUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(1);
            assertThat(response.getMessage()).isEqualTo("Service error during user creation: Invalid user data");
            assertThat(response.getUser()).isNull();
            
            
        }

        @Test
        @DisplayName("Should handle unexpected error during create")
        void shouldHandleUnexpectedErrorDuringCreate() throws TException {
            // Given
            TCreateUserRequest request = new TCreateUserRequest()
                    .setName("John Doe")
                    .setPhone("+1-555-123-4567")
                    .setAddress("123 Main St");

            RuntimeException exception = new RuntimeException("Unexpected error");
            when(userService.create(any(User.class))).thenThrow(exception);
            

            // When
            TCreateUserResponse response = handler.createUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(100);
            assertThat(response.getMessage()).isEqualTo("Unexpected error during user creation: Unexpected error");
            assertThat(response.getUser()).isNull();
            
            
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should get user successfully when found")
        void shouldGetUserSuccessfullyWhenFound() throws TException {
            // Given
            TGetUserRequest request = new TGetUserRequest().setId("user-123");
            User foundUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userService.getById("user-123")).thenReturn(Optional.of(foundUser));
            

            // When
            TGetUserResponse response = handler.getUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("User retrieved successfully");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getId()).isEqualTo("user-123");
            assertThat(response.getUser().getName()).isEqualTo("John Doe");
            assertThat(response.getUser().getPhone()).isEqualTo("+1-555-123-4567");
            assertThat(response.getUser().getAddress()).isEqualTo("123 Main St");
            
            verify(userService).getById("user-123");
            
        }

        @Test
        @DisplayName("Should handle user not found")
        void shouldHandleUserNotFound() throws TException {
            // Given
            TGetUserRequest request = new TGetUserRequest().setId("non-existent-user");
            when(userService.getById("non-existent-user")).thenReturn(Optional.empty());
            

            // When
            TGetUserResponse response = handler.getUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(2);
            assertThat(response.getMessage()).isEqualTo("User not found");
            assertThat(response.getUser()).isNull();
            
            verify(userService).getById("non-existent-user");
        }

        @Test
        @DisplayName("Should handle database error during get user")
        void shouldHandleDatabaseErrorDuringGetUser() throws TException {
            // Given
            TGetUserRequest request = new TGetUserRequest().setId("user-123");
            DatabaseException exception = new DatabaseException("Database error");
            when(userService.getById("user-123")).thenThrow(exception);
            

            // When
            TGetUserResponse response = handler.getUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(101);
            assertThat(response.getMessage()).isEqualTo("Database error during user retrieval: Database error");
            assertThat(response.getUser()).isNull();
            
            
        }

        @Test
        @DisplayName("Should handle unexpected error during get user")
        void shouldHandleUnexpectedErrorDuringGetUser() throws TException {
            // Given
            TGetUserRequest request = new TGetUserRequest().setId("user-123");
            RuntimeException exception = new RuntimeException("Unexpected error");
            when(userService.getById("user-123")).thenThrow(exception);
            

            // When
            TGetUserResponse response = handler.getUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(100);
            assertThat(response.getMessage()).isEqualTo("Unexpected error during user retrieval: Unexpected error");
            assertThat(response.getUser()).isNull();
            
            
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully when user exists")
        void shouldUpdateUserSuccessfullyWhenUserExists() throws TException {
            // Given
            TUpdateUserRequest request = new TUpdateUserRequest()
                    .setId("user-123")
                    .setName("John Updated")
                    .setPhone("+1-555-999-8888")
                    .setAddress("456 New St");

            User existingUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            User updatedUser = User.builder()
                    .id("user-123")
                    .name("John Updated")
                    .phone("+1-555-999-8888")
                    .address("456 New St")
                    .build();

            when(userService.getById("user-123")).thenReturn(Optional.of(existingUser));
            when(userService.update(any(User.class))).thenReturn(updatedUser);
            

            // When
            TUpdateUserResponse response = handler.updateUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("User updated successfully");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getId()).isEqualTo("user-123");
            assertThat(response.getUser().getName()).isEqualTo("John Updated");
            assertThat(response.getUser().getPhone()).isEqualTo("+1-555-999-8888");
            assertThat(response.getUser().getAddress()).isEqualTo("456 New St");
            
            verify(userService).getById("user-123");
            verify(userService).update(any(User.class));
            
        }

        @Test
        @DisplayName("Should handle user not found for update")
        void shouldHandleUserNotFoundForUpdate() throws TException {
            // Given
            TUpdateUserRequest request = new TUpdateUserRequest()
                    .setId("non-existent-user")
                    .setName("John Updated")
                    .setPhone("+1-555-999-8888")
                    .setAddress("456 New St");

            when(userService.getById("non-existent-user")).thenReturn(Optional.empty());
            

            // When
            TUpdateUserResponse response = handler.updateUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(1);
            assertThat(response.getMessage()).isEqualTo("User not found");
            assertThat(response.getUser()).isNull();
            
            verify(userService).getById("non-existent-user");
            verify(userService, never()).update(any(User.class));
        }

        @Test
        @DisplayName("Should handle database error during update")
        void shouldHandleDatabaseErrorDuringUpdate() throws TException {
            // Given
            TUpdateUserRequest request = new TUpdateUserRequest()
                    .setId("user-123")
                    .setName("John Updated")
                    .setPhone("+1-555-999-8888")
                    .setAddress("456 New St");

            User existingUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userService.getById("user-123")).thenReturn(Optional.of(existingUser));
            DatabaseException exception = new DatabaseException("Database error");
            when(userService.update(any(User.class))).thenThrow(exception);
            

            // When
            TUpdateUserResponse response = handler.updateUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(3);
            assertThat(response.getMessage()).isEqualTo("Database error during user update: Database error");
            assertThat(response.getUser()).isNull();
            
            
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully when user exists")
        void shouldDeleteUserSuccessfullyWhenUserExists() throws TException {
            // Given
            TDeleteUserRequest request = new TDeleteUserRequest().setId("user-123");
            User existingUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userService.getById("user-123")).thenReturn(Optional.of(existingUser));
            

            // When
            TDeleteUserResponse response = handler.deleteUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("User deleted successfully");
            
            verify(userService).getById("user-123");
            verify(userService).delete("user-123");
            
        }

        @Test
        @DisplayName("Should handle user not found for delete")
        void shouldHandleUserNotFoundForDelete() throws TException {
            // Given
            TDeleteUserRequest request = new TDeleteUserRequest().setId("non-existent-user");
            when(userService.getById("non-existent-user")).thenReturn(Optional.empty());
            

            // When
            TDeleteUserResponse response = handler.deleteUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(1);
            assertThat(response.getMessage()).isEqualTo("User not found");
            
            verify(userService).getById("non-existent-user");
            verify(userService, never()).delete(anyString());
        }

        @Test
        @DisplayName("Should handle database error during delete")
        void shouldHandleDatabaseErrorDuringDelete() throws TException {
            // Given
            TDeleteUserRequest request = new TDeleteUserRequest().setId("user-123");
            User existingUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userService.getById("user-123")).thenReturn(Optional.of(existingUser));
            DatabaseException exception = new DatabaseException("Database error");
            doThrow(exception).when(userService).delete("user-123");

            // When
            TDeleteUserResponse response = handler.deleteUser(request);

            // Then
            assertThat(response.getStatus()).isEqualTo(2);
            assertThat(response.getMessage()).isEqualTo("Database error during user deletion: Database error");
            
            
        }
    }

    @Nested
    @DisplayName("List Users Tests")
    class ListUsersTests {

        

        
    }

    

    @Nested
    @DisplayName("Data Mapping Tests")
    class DataMappingTests {

        @Test
        @DisplayName("Should map domain user to thrift user correctly")
        void shouldMapDomainUserToThriftUserCorrectly() throws TException {
            // Given
            User domainUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(userService.create(any(User.class))).thenReturn(domainUser);
            

            TCreateUserRequest request = new TCreateUserRequest()
                    .setName("John Doe")
                    .setPhone("+1-555-123-4567")
                    .setAddress("123 Main St");

            // When
            TCreateUserResponse response = handler.createUser(request);

            // Then
            TUser thriftUser = response.getUser();
            assertThat(thriftUser.getId()).isEqualTo(domainUser.getId());
            assertThat(thriftUser.getName()).isEqualTo(domainUser.getName());
            assertThat(thriftUser.getPhone()).isEqualTo(domainUser.getPhone());
            assertThat(thriftUser.getAddress()).isEqualTo(domainUser.getAddress());
        }

        

        @Test
        @DisplayName("Should handle null fields in mapping")
        void shouldHandleNullFieldsInMapping() throws TException {
            // Given
            User domainUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address(null) // null address
                    .build();

            when(userService.create(any(User.class))).thenReturn(domainUser);

            TCreateUserRequest request = new TCreateUserRequest()
                    .setName("John Doe")
                    .setPhone("+1-555-123-4567")
                    .setAddress(null);

            // When
            TCreateUserResponse response = handler.createUser(request);

            // Then
            TUser thriftUser = response.getUser();
            assertThat(thriftUser.getId()).isEqualTo(domainUser.getId());
            assertThat(thriftUser.getName()).isEqualTo(domainUser.getName());
            assertThat(thriftUser.getPhone()).isEqualTo(domainUser.getPhone());
            assertThat(thriftUser.getAddress()).isNull();
        }
    }
}
