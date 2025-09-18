package com.example.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("User Domain Model Tests")
public class UserTest {
    private Validator validator;
    private static final String VALID_PHONE = "+84-123-456-789";
    private static final String VALID_NAME = "Nguyen Gia Huy";
    private static final String VALID_ADDRESS = "123 Street, District 10";
    private static final String VALID_USERID = "validUserId";

    @BeforeEach
    void setup() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create user with all fields using builder")
        void shouldCreateUserWithAllFields() {
            // ARRANGE - Prepare test data

            String id = "userId";
            String name = "userName";
            String phone = "+84-123-456-789";
            String address = "123 Nguyen Thuong Hien, District 10";

            // ACT - Perform actions needed to test
            User user = User.builder().id(id).name(name).phone(phone).address(address).status(User.UserStatus.ACTIVE)
                    .role(User.UserRole.USER).createdAt(LocalDateTime.now()).createdBy("admin").version(1)
                    .deleted(false).build();

            // ASSERT
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.getPhone()).isEqualTo(phone);
            assertThat(user.getAddress()).isEqualTo(address);
            assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
            assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
        }

        @Test
        @DisplayName("Should create user with minimal required fields")
        void shouldCreateUserWithMinimalFields() {
            // ARRANGE

            String name = "userName";

            // ACT

            User user = User.builder().name(name).build();

            // ASSERT

            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.getPhone()).isNull();
            assertThat(user.getAddress()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid data")
        void shouldPassValidationWithValidData() {

            User user = User.builder().name("valid name").address("Valid Address").phone("+1-123-456-89").build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);

            assertThat(violations).isEmpty();
            assertThat(violations.size()).isEqualTo(0);

        }


        @ParameterizedTest
        @DisplayName("Should fail validation when name is blank")
        @ValueSource(strings = {"", "    ", "\t", "\n", "\t\n"})
        void shouldFailValidationWhenNameIsBlank(String invalidName) {

            User user = User.builder().name(invalidName).phone(VALID_PHONE).build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("not be blank");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields are the same")
        void shouldBeEqualWhenAllFieldsAreSame() {
            User user1 = User.builder().id(VALID_USERID).name(VALID_NAME).phone(VALID_PHONE).address(VALID_ADDRESS).build();
            
            User user2 = User.builder().id(VALID_USERID).name(VALID_NAME).phone(VALID_PHONE).address(VALID_ADDRESS).build();
        
            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        
        }

        @Test
        @DisplayName("Should not be equal when id is different")
        void shouldNotBeEqualWhenIdIsDifferent() {
            User user1 = User.builder().id(VALID_USERID).name(VALID_NAME).phone(VALID_PHONE).address(VALID_ADDRESS).build();
            
            User user2 = User.builder().id("user-456").name(VALID_NAME).phone(VALID_PHONE).address(VALID_ADDRESS).build();
        
            assertThat(user1).isNotEqualTo(user2);
            assertThat(user1.hashCode()).isNotEqualTo(user2.hashCode());
        }

    }

}
