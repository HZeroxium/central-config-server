package com.example.user.domain;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for the User domain model.
 * Tests validation, builder pattern, equals/hashCode, and property-based testing.
 */
@DisplayName("User Domain Model Tests")
class UserTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create user with all fields using builder")
        void shouldCreateUserWithAllFields() {
            // Given
            String id = "user-123";
            String name = "John Doe";
            String phone = "+1-555-123-4567";
            String address = "123 Main St, City, State";

            // When
            User user = User.builder()
                    .id(id)
                    .name(name)
                    .phone(phone)
                    .address(address)
                    .build();

            // Then
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.getPhone()).isEqualTo(phone);
            assertThat(user.getAddress()).isEqualTo(address);
        }

        @Test
        @DisplayName("Should create user with minimal required fields")
        void shouldCreateUserWithMinimalFields() {
            // Given
            String name = "Jane Doe";
            String phone = "+1-555-987-6543";

            // When
            User user = User.builder()
                    .name(name)
                    .phone(phone)
                    .build();

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.getPhone()).isEqualTo(phone);
            assertThat(user.getAddress()).isNull();
        }

        @Test
        @DisplayName("Should create user with no-args constructor")
        void shouldCreateUserWithNoArgsConstructor() {
            // When
            User user = new User();

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isNull();
            assertThat(user.getPhone()).isNull();
            assertThat(user.getAddress()).isNull();
        }

        @Test
        @DisplayName("Should create user with all-args constructor")
        void shouldCreateUserWithAllArgsConstructor() {
            // Given
            String id = "user-456";
            String name = "Bob Smith";
            String phone = "+1-555-111-2222";
            String address = "456 Oak Ave, Town, State";

            // When
            User user = User.builder()
                    .id(id)
                    .name(name)
                    .phone(phone)
                    .address(address)
                    .status(User.UserStatus.ACTIVE)
                    .role(User.UserRole.USER)
                    .createdBy("admin")
                    .updatedBy("admin")
                    .version(1)
                    .deleted(false)
                    .build();

            // Then
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.getPhone()).isEqualTo(phone);
            assertThat(user.getAddress()).isEqualTo(address);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid data")
        void shouldPassValidationWithValidData() {
            // Given
            User user = User.builder()
                    .name("Valid User")
                    .phone("+1-555-123-4567")
                    .address("Valid Address")
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @DisplayName("Should fail validation when name is blank")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        void shouldFailValidationWhenNameIsBlank(String invalidName) {
            // Given
            User user = User.builder()
                    .name(invalidName)
                    .phone("+1-555-123-4567")
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("not be blank");
        }

        @Test
        @DisplayName("Should fail validation when name is null")
        void shouldFailValidationWhenNameIsNull() {
            // Given
            User user = User.builder()
                    .phone("+1-555-123-4567")
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("not be blank");
        }

        @Test
        @DisplayName("Should fail validation when name exceeds maximum length")
        void shouldFailValidationWhenNameExceedsMaxLength() {
            // Given - 101 characters name
            String longName = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            
            User user = User.builder()
                    .name(longName)
                    .phone("+1-555-123-4567")
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("size must be between 0 and 100");
        }
        
        @Test
        @DisplayName("Should fail validation when name is very long descriptive text")
        void shouldFailValidationWhenNameIsVeryLongDescriptiveText() {
            // Given
            String longName = "Very Long Name That Exceeds The Maximum Allowed Length Of One Hundred Characters And Should Fail Validation";
            
            User user = User.builder()
                    .name(longName)
                    .phone("+1-555-123-4567")
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("size must be between 0 and 100");
        }

        @ParameterizedTest
        @DisplayName("Should fail validation when phone is blank")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        void shouldFailValidationWhenPhoneIsBlank(String invalidPhone) {
            // Given
            User user = User.builder()
                    .name("Valid Name")
                    .phone(invalidPhone)
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("not be blank");
        }

        @Test
        @DisplayName("Should fail validation when phone is null")
        void shouldFailValidationWhenPhoneIsNull() {
            // Given
            User user = User.builder()
                    .name("Valid Name")
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("not be blank");
        }

        @Test
        @DisplayName("Should fail validation when phone exceeds maximum length")
        void shouldFailValidationWhenPhoneExceedsMaxLength() {
            // Given - 33 characters phone
            String longPhone = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            
            User user = User.builder()
                    .name("Valid Name")
                    .phone(longPhone)
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("size must be between 0 and 32");
        }
        
        @Test
        @DisplayName("Should fail validation when phone is extra long descriptive")
        void shouldFailValidationWhenPhoneIsExtraLongDescriptive() {
            // Given
            String longPhone = "+1-555-123-4567-EXTRA-LONG-PHONE-NUMBER-THAT-EXCEEDS-MAX-LENGTH";
            
            User user = User.builder()
                    .name("Valid Name")
                    .phone(longPhone)
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("size must be between 0 and 32");
        }

        @ParameterizedTest
        @DisplayName("Should pass validation when address is within maximum length")
        @ValueSource(strings = {
                "Short Address",
                "Medium length address that should be valid"
        })
        void shouldPassValidationWhenAddressIsWithinMaxLength(String address) {
            // Given
            User user = User.builder()
                    .name("Valid Name")
                    .phone("+1-555-123-4567")
                    .address(address)
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).isEmpty();
        }
        
        @Test
        @DisplayName("Should pass validation when address is exactly maximum length")
        void shouldPassValidationWhenAddressIsExactlyMaxLength() {
            // Given - exactly 255 characters
            String maxLengthAddress = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            
            User user = User.builder()
                    .name("Valid Name")
                    .phone("+1-555-123-4567")
                    .address(maxLengthAddress)
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).isEmpty();
        }
        
        @Test
        @DisplayName("Should pass validation when address is null")
        void shouldPassValidationWhenAddressIsNull() {
            // Given
            User user = User.builder()
                    .name("Valid Name")
                    .phone("+1-555-123-4567")
                    .address(null)
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when address exceeds maximum length")
        void shouldFailValidationWhenAddressExceedsMaxLength() {
            // Given
            String longAddress = "A".repeat(256); // 256 characters
            User user = User.builder()
                    .name("Valid Name")
                    .phone("+1-555-123-4567")
                    .address(longAddress)
                    .build();

            // When
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("size must be between 0 and 255");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields are the same")
        void shouldBeEqualWhenAllFieldsAreSame() {
            // Given
            User user1 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            User user2 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            // When & Then
            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when id is different")
        void shouldNotBeEqualWhenIdIsDifferent() {
            // Given
            User user1 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            User user2 = User.builder()
                    .id("user-456")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            // When & Then
            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("Should not be equal when name is different")
        void shouldNotBeEqualWhenNameIsDifferent() {
            // Given
            User user1 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            User user2 = User.builder()
                    .id("user-123")
                    .name("Jane Doe")
                    .phone("+1-555-123-4567")
                    .build();

            // When & Then
            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("Should not be equal when phone is different")
        void shouldNotBeEqualWhenPhoneIsDifferent() {
            // Given
            User user1 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            User user2 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-987-6543")
                    .build();

            // When & Then
            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("Should not be equal when address is different")
        void shouldNotBeEqualWhenAddressIsDifferent() {
            // Given
            User user1 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            User user2 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("456 Oak Ave")
                    .build();

            // When & Then
            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("Should be equal when both have null address")
        void shouldBeEqualWhenBothHaveNullAddress() {
            // Given
            User user1 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address(null)
                    .build();

            User user2 = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address(null)
                    .build();

            // When & Then
            assertThat(user1).isEqualTo(user2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            User user = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            // When & Then
            assertThat(user).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different class")
        void shouldNotBeEqualToDifferentClass() {
            // Given
            User user = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            String otherObject = "not a user";

            // When & Then
            assertThat(user).isNotEqualTo(otherObject);
        }
    }


    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle null id")
        void shouldHandleNullId() {
            // Given & When
            User user = User.builder()
                    .id(null)
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should handle empty string id")
        void shouldHandleEmptyStringId() {
            // Given & When
            User user = User.builder()
                    .id("")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            // Then
            assertThat(user.getId()).isEmpty();
            assertThat(user.getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should handle maximum length name")
        void shouldHandleMaximumLengthName() {
            // Given
            String maxLengthName = "A".repeat(100);

            // When
            User user = User.builder()
                    .name(maxLengthName)
                    .phone("+1-555-123-4567")
                    .build();

            // Then
            assertThat(user.getName()).isEqualTo(maxLengthName);
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle maximum length phone")
        void shouldHandleMaximumLengthPhone() {
            // Given
            String maxLengthPhone = "A".repeat(32);

            // When
            User user = User.builder()
                    .name("John Doe")
                    .phone(maxLengthPhone)
                    .build();

            // Then
            assertThat(user.getPhone()).isEqualTo(maxLengthPhone);
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle maximum length address")
        void shouldHandleMaximumLengthAddress() {
            // Given
            String maxLengthAddress = "A".repeat(255);

            // When
            User user = User.builder()
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address(maxLengthAddress)
                    .build();

            // Then
            assertThat(user.getAddress()).isEqualTo(maxLengthAddress);
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).isEmpty();
        }
    }
}
