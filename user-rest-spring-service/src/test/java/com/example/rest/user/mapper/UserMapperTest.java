package com.example.rest.user.mapper;

import com.example.common.domain.User;
import com.example.rest.user.dto.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for UserMapper utility class.
 * Tests all mapping methods between DTOs and domain models.
 */
@DisplayName("UserMapper Tests")
class UserMapperTest {

    @Nested
    @DisplayName("CreateUserRequest to Domain Mapping Tests")
    class CreateUserRequestToDomainMappingTests {

        @Test
        @DisplayName("Should map CreateUserRequest to domain User with all fields")
        void shouldMapCreateUserRequestToDomainUserWithAllFields() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("John Doe");
            request.setPhone("+1-555-123-4567");
            request.setAddress("123 Main St, City, State");

            // When
            User user = UserMapper.toDomainFromCreateRequest(request);

            // Then
            assertThat(user.getId()).isNull(); // Should be null for create requests
            assertThat(user.getName()).isEqualTo(request.getName());
            assertThat(user.getPhone()).isEqualTo(request.getPhone());
            assertThat(user.getAddress()).isEqualTo(request.getAddress());
            assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
            assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
            assertThat(user.getCreatedBy()).isEqualTo("admin");
            assertThat(user.getUpdatedBy()).isEqualTo("admin");
            assertThat(user.getVersion()).isEqualTo(1);
            assertThat(user.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("Should map CreateUserRequest to domain User with minimal fields")
        void shouldMapCreateUserRequestToDomainUserWithMinimalFields() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("Jane Doe");
            request.setPhone("+1-555-987-6543");
            // address is null

            // When
            User user = UserMapper.toDomainFromCreateRequest(request);

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEqualTo(request.getName());
            assertThat(user.getPhone()).isEqualTo(request.getPhone());
            assertThat(user.getAddress()).isNull();
            assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
            assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
            assertThat(user.getCreatedBy()).isEqualTo("admin");
            assertThat(user.getUpdatedBy()).isEqualTo("admin");
            assertThat(user.getVersion()).isEqualTo(1);
            assertThat(user.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("Should map CreateUserRequest to domain User with null fields")
        void shouldMapCreateUserRequestToDomainUserWithNullFields() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName(null);
            request.setPhone(null);
            request.setAddress(null);

            // When
            User user = UserMapper.toDomainFromCreateRequest(request);

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isNull();
            assertThat(user.getPhone()).isNull();
            assertThat(user.getAddress()).isNull();
        }

        @Test
        @DisplayName("Should map CreateUserRequest to domain User with empty strings")
        void shouldMapCreateUserRequestToDomainUserWithEmptyStrings() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("");
            request.setPhone("");
            request.setAddress("");

            // When
            User user = UserMapper.toDomainFromCreateRequest(request);

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEmpty();
            assertThat(user.getPhone()).isEmpty();
            assertThat(user.getAddress()).isEmpty();
        }

        @Test
        @DisplayName("Should map CreateUserRequest to domain User with maximum length fields")
        void shouldMapCreateUserRequestToDomainUserWithMaximumLengthFields() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("A".repeat(100)); // Maximum length
            request.setPhone("A".repeat(32)); // Maximum length
            request.setAddress("A".repeat(255)); // Maximum length

            // When
            User user = UserMapper.toDomainFromCreateRequest(request);

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEqualTo(request.getName());
            assertThat(user.getPhone()).isEqualTo(request.getPhone());
            assertThat(user.getAddress()).isEqualTo(request.getAddress());
        }
    }

    @Nested
    @DisplayName("UpdateUserRequest to Domain Mapping Tests")
    class UpdateUserRequestToDomainMappingTests {

        @Test
        @DisplayName("Should map UpdateUserRequest to domain User with all fields")
        void shouldMapUpdateUserRequestToDomainUserWithAllFields() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("John Doe Updated");
            request.setPhone("+1-555-123-4567");
            request.setAddress("456 Oak Ave, Town, State");

            // When
            User user = UserMapper.toDomainFromUpdateRequest(request, "test-id");

            // Then
            assertThat(user.getId()).isEqualTo("test-id");
            assertThat(user.getName()).isEqualTo(request.getName());
            assertThat(user.getPhone()).isEqualTo(request.getPhone());
            assertThat(user.getAddress()).isEqualTo(request.getAddress());
            assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
            assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
            assertThat(user.getUpdatedBy()).isEqualTo("admin");
            assertThat(user.getVersion()).isEqualTo(1);
            assertThat(user.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("Should map UpdateUserRequest to domain User with minimal fields")
        void shouldMapUpdateUserRequestToDomainUserWithMinimalFields() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("Jane Doe Updated");
            request.setPhone("+1-555-987-6543");
            // address is null

            // When
            User user = UserMapper.toDomainFromUpdateRequest(request, "test-id");

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEqualTo(request.getName());
            assertThat(user.getPhone()).isEqualTo(request.getPhone());
            assertThat(user.getAddress()).isNull();
        }

        @Test
        @DisplayName("Should map UpdateUserRequest to domain User with null fields")
        void shouldMapUpdateUserRequestToDomainUserWithNullFields() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName(null);
            request.setPhone(null);
            request.setAddress(null);

            // When
            User user = UserMapper.toDomainFromUpdateRequest(request, "test-id");

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isNull();
            assertThat(user.getPhone()).isNull();
            assertThat(user.getAddress()).isNull();
        }

        @Test
        @DisplayName("Should map UpdateUserRequest to domain User with empty strings")
        void shouldMapUpdateUserRequestToDomainUserWithEmptyStrings() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("");
            request.setPhone("");
            request.setAddress("");

            // When
            User user = UserMapper.toDomainFromUpdateRequest(request, "test-id");

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEmpty();
            assertThat(user.getPhone()).isEmpty();
            assertThat(user.getAddress()).isEmpty();
        }

        @Test
        @DisplayName("Should map UpdateUserRequest to domain User with maximum length fields")
        void shouldMapUpdateUserRequestToDomainUserWithMaximumLengthFields() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("A".repeat(100)); // Maximum length
            request.setPhone("A".repeat(32)); // Maximum length
            request.setAddress("A".repeat(255)); // Maximum length

            // When
            User user = UserMapper.toDomainFromUpdateRequest(request, "test-id");

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEqualTo(request.getName());
            assertThat(user.getPhone()).isEqualTo(request.getPhone());
            assertThat(user.getAddress()).isEqualTo(request.getAddress());
        }
    }

    @Nested
    @DisplayName("Domain to UserResponse Mapping Tests")
    class DomainToUserResponseMappingTests {

        @Test
        @DisplayName("Should map domain User to UserResponse with all fields")
        void shouldMapDomainUserToUserResponseWithAllFields() {
            // Given
            User user = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St, City, State")
                    .status(User.UserStatus.ACTIVE)
                    .role(User.UserRole.USER)
                    .createdAt(java.time.LocalDateTime.now())
                    .createdBy("admin")
                    .updatedAt(java.time.LocalDateTime.now())
                    .updatedBy("admin")
                    .version(1)
                    .deleted(false)
                    .build();

            // When
            UserResponse response = UserMapper.toResponse(user);

            // Then
            assertThat(response.getId()).isEqualTo(user.getId());
            assertThat(response.getName()).isEqualTo(user.getName());
            assertThat(response.getPhone()).isEqualTo(user.getPhone());
            assertThat(response.getAddress()).isEqualTo(user.getAddress());
            assertThat(response.getStatus()).isEqualTo(user.getStatus());
            assertThat(response.getRole()).isEqualTo(user.getRole());
            assertThat(response.getCreatedAt()).isEqualTo(user.getCreatedAt());
            assertThat(response.getCreatedBy()).isEqualTo(user.getCreatedBy());
            assertThat(response.getUpdatedAt()).isEqualTo(user.getUpdatedAt());
            assertThat(response.getUpdatedBy()).isEqualTo(user.getUpdatedBy());
            assertThat(response.getVersion()).isEqualTo(user.getVersion());
        }

        @Test
        @DisplayName("Should map domain User to UserResponse with minimal fields")
        void shouldMapDomainUserToUserResponseWithMinimalFields() {
            // Given
            User user = User.builder()
                    .id("user-456")
                    .name("Jane Doe")
                    .phone("+1-555-987-6543")
                    .address(null)
                    .build();

            // When
            UserResponse response = UserMapper.toResponse(user);

            // Then
            assertThat(response.getId()).isEqualTo(user.getId());
            assertThat(response.getName()).isEqualTo(user.getName());
            assertThat(response.getPhone()).isEqualTo(user.getPhone());
            assertThat(response.getAddress()).isNull();
        }

        @Test
        @DisplayName("Should map domain User to UserResponse with null fields")
        void shouldMapDomainUserToUserResponseWithNullFields() {
            // Given
            User user = User.builder()
                    .id(null)
                    .name(null)
                    .phone(null)
                    .address(null)
                    .build();

            // When
            UserResponse response = UserMapper.toResponse(user);

            // Then
            assertThat(response.getId()).isNull();
            assertThat(response.getName()).isNull();
            assertThat(response.getPhone()).isNull();
            assertThat(response.getAddress()).isNull();
        }

        @Test
        @DisplayName("Should map domain User to UserResponse with empty strings")
        void shouldMapDomainUserToUserResponseWithEmptyStrings() {
            // Given
            User user = User.builder()
                    .id("")
                    .name("")
                    .phone("")
                    .address("")
                    .build();

            // When
            UserResponse response = UserMapper.toResponse(user);

            // Then
            assertThat(response.getId()).isEmpty();
            assertThat(response.getName()).isEmpty();
            assertThat(response.getPhone()).isEmpty();
            assertThat(response.getAddress()).isEmpty();
        }

        @Test
        @DisplayName("Should map domain User to UserResponse with maximum length fields")
        void shouldMapDomainUserToUserResponseWithMaximumLengthFields() {
            // Given
            User user = User.builder()
                    .id("user-max-length")
                    .name("A".repeat(100)) // Maximum length
                    .phone("A".repeat(32)) // Maximum length
                    .address("A".repeat(255)) // Maximum length
                    .build();

            // When
            UserResponse response = UserMapper.toResponse(user);

            // Then
            assertThat(response.getId()).isEqualTo(user.getId());
            assertThat(response.getName()).isEqualTo(user.getName());
            assertThat(response.getPhone()).isEqualTo(user.getPhone());
            assertThat(response.getAddress()).isEqualTo(user.getAddress());
        }
    }

    @Nested
    @DisplayName("Round Trip Mapping Tests")
    class RoundTripMappingTests {

        @Test
        @DisplayName("Should maintain data integrity through create request round trip")
        void shouldMaintainDataIntegrityThroughCreateRequestRoundTrip() {
            // Given
            CreateUserRequest originalRequest = new CreateUserRequest();
            originalRequest.setName("John Doe");
            originalRequest.setPhone("+1-555-123-4567");
            originalRequest.setAddress("123 Main St, City, State");

            // When
            User domainUser = UserMapper.toDomainFromCreateRequest(originalRequest);
            UserResponse response = UserMapper.toResponse(domainUser);

            // Then
            assertThat(response.getName()).isEqualTo(originalRequest.getName());
            assertThat(response.getPhone()).isEqualTo(originalRequest.getPhone());
            assertThat(response.getAddress()).isEqualTo(originalRequest.getAddress());
        }

        @Test
        @DisplayName("Should maintain data integrity through update request round trip")
        void shouldMaintainDataIntegrityThroughUpdateRequestRoundTrip() {
            // Given
            UpdateUserRequest originalRequest = new UpdateUserRequest();
            originalRequest.setName("Jane Doe Updated");
            originalRequest.setPhone("+1-555-987-6543");
            originalRequest.setAddress("456 Oak Ave, Town, State");

            // When
            User domainUser = UserMapper.toDomainFromUpdateRequest(originalRequest);
            UserResponse response = UserMapper.toResponse(domainUser);

            // Then
            assertThat(response.getName()).isEqualTo(originalRequest.getName());
            assertThat(response.getPhone()).isEqualTo(originalRequest.getPhone());
            assertThat(response.getAddress()).isEqualTo(originalRequest.getAddress());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle special characters in mapping")
        void shouldHandleSpecialCharactersInMapping() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("José María O'Connor-Smith");
            request.setPhone("+1-555-123-4567 ext. 123");
            request.setAddress("123 Main St., Apt. #4B, New York, NY 10001");

            // When
            User user = UserMapper.toDomainFromCreateRequest(request);
            UserResponse response = UserMapper.toResponse(user);

            // Then
            assertThat(response.getName()).isEqualTo(request.getName());
            assertThat(response.getPhone()).isEqualTo(request.getPhone());
            assertThat(response.getAddress()).isEqualTo(request.getAddress());
        }

        @Test
        @DisplayName("Should handle unicode characters in mapping")
        void shouldHandleUnicodeCharactersInMapping() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("张三");
            request.setPhone("+86-138-0013-8000");
            request.setAddress("北京市朝阳区");

            // When
            User user = UserMapper.toDomainFromCreateRequest(request);
            UserResponse response = UserMapper.toResponse(user);

            // Then
            assertThat(response.getName()).isEqualTo(request.getName());
            assertThat(response.getPhone()).isEqualTo(request.getPhone());
            assertThat(response.getAddress()).isEqualTo(request.getAddress());
        }

        @Test
        @DisplayName("Should handle very long strings in mapping")
        void shouldHandleVeryLongStringsInMapping() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("A".repeat(100)); // Maximum allowed
            request.setPhone("A".repeat(32)); // Maximum allowed
            request.setAddress("A".repeat(255)); // Maximum allowed

            // When
            User user = UserMapper.toDomainFromCreateRequest(request);
            UserResponse response = UserMapper.toResponse(user);

            // Then
            assertThat(response.getName()).isEqualTo(request.getName());
            assertThat(response.getPhone()).isEqualTo(request.getPhone());
            assertThat(response.getAddress()).isEqualTo(request.getAddress());
        }

        @Test
        @DisplayName("Should handle whitespace-only strings in mapping")
        void shouldHandleWhitespaceOnlyStringsInMapping() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setName("   ");
            request.setPhone("\t\t\t");
            request.setAddress("\n\n\n");

            // When
            User user = UserMapper.toDomainFromCreateRequest(request);
            UserResponse response = UserMapper.toResponse(user);

            // Then
            assertThat(response.getName()).isEqualTo(request.getName());
            assertThat(response.getPhone()).isEqualTo(request.getPhone());
            assertThat(response.getAddress()).isEqualTo(request.getAddress());
        }
    }

    @Nested
    @DisplayName("Null Safety Tests")
    class NullSafetyTests {

        @Test
        @DisplayName("Should handle null CreateUserRequest gracefully")
        void shouldHandleNullCreateUserRequestGracefully() {
            // Given
            CreateUserRequest request = null;

            // When & Then
            assertThatThrownBy(() -> UserMapper.toDomainFromCreateRequest(request))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null UpdateUserRequest gracefully")
        void shouldHandleNullUpdateUserRequestGracefully() {
            // Given
            UpdateUserRequest request = null;

            // When & Then
            assertThatThrownBy(() -> UserMapper.toDomainFromUpdateRequest(request))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null User gracefully")
        void shouldHandleNullUserGracefully() {
            // Given
            User user = null;

            // When & Then
            assertThatThrownBy(() -> UserMapper.toResponse(user))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
