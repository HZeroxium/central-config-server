package com.example.rest.exception;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ErrorCode utility class.
 * Tests error code constants and immutability.
 */
@DisplayName("ErrorCode Tests")
class ErrorCodeTest {

    @Nested
    @DisplayName("Error Code Constants Tests")
    class ErrorCodeConstantsTests {

        @Test
        @DisplayName("Should have correct user related error codes")
        void shouldHaveCorrectUserRelatedErrorCodes() {
            assertThat(ErrorCode.USER_NOT_FOUND).isEqualTo("USER_NOT_FOUND");
            assertThat(ErrorCode.USER_ALREADY_EXISTS).isEqualTo("USER_ALREADY_EXISTS");
            assertThat(ErrorCode.USER_VALIDATION_FAILED).isEqualTo("USER_VALIDATION_FAILED");
            assertThat(ErrorCode.USER_CREATION_FAILED).isEqualTo("USER_CREATION_FAILED");
            assertThat(ErrorCode.USER_UPDATE_FAILED).isEqualTo("USER_UPDATE_FAILED");
            assertThat(ErrorCode.USER_DELETION_FAILED).isEqualTo("USER_DELETION_FAILED");
        }

        @Test
        @DisplayName("Should have correct Thrift service error codes")
        void shouldHaveCorrectThriftServiceErrorCodes() {
            assertThat(ErrorCode.THRIFT_SERVICE_ERROR).isEqualTo("THRIFT_SERVICE_ERROR");
            assertThat(ErrorCode.THRIFT_SERVICE_UNAVAILABLE).isEqualTo("THRIFT_SERVICE_UNAVAILABLE");
            assertThat(ErrorCode.THRIFT_SERVICE_TIMEOUT).isEqualTo("THRIFT_SERVICE_TIMEOUT");
            assertThat(ErrorCode.THRIFT_SERVICE_CONNECTION_FAILED).isEqualTo("THRIFT_SERVICE_CONNECTION_FAILED");
        }

        @Test
        @DisplayName("Should have correct database error codes")
        void shouldHaveCorrectDatabaseErrorCodes() {
            assertThat(ErrorCode.DATABASE_ERROR).isEqualTo("DATABASE_ERROR");
            assertThat(ErrorCode.DATABASE_CONNECTION_FAILED).isEqualTo("DATABASE_CONNECTION_FAILED");
            assertThat(ErrorCode.DATABASE_CONSTRAINT_VIOLATION).isEqualTo("DATABASE_CONSTRAINT_VIOLATION");
            assertThat(ErrorCode.DATABASE_TIMEOUT).isEqualTo("DATABASE_TIMEOUT");
        }

        @Test
        @DisplayName("Should have correct validation error codes")
        void shouldHaveCorrectValidationErrorCodes() {
            assertThat(ErrorCode.VALIDATION_ERROR).isEqualTo("VALIDATION_ERROR");
            assertThat(ErrorCode.INVALID_INPUT).isEqualTo("INVALID_INPUT");
            assertThat(ErrorCode.MISSING_REQUIRED_FIELD).isEqualTo("MISSING_REQUIRED_FIELD");
            assertThat(ErrorCode.INVALID_FORMAT).isEqualTo("INVALID_FORMAT");
        }

        @Test
        @DisplayName("Should have correct system error codes")
        void shouldHaveCorrectSystemErrorCodes() {
            assertThat(ErrorCode.INTERNAL_SERVER_ERROR).isEqualTo("INTERNAL_SERVER_ERROR");
            assertThat(ErrorCode.SERVICE_UNAVAILABLE).isEqualTo("SERVICE_UNAVAILABLE");
            assertThat(ErrorCode.RATE_LIMIT_EXCEEDED).isEqualTo("RATE_LIMIT_EXCEEDED");
        }
    }

    @Nested
    @DisplayName("Error Code Properties Tests")
    class ErrorCodePropertiesTests {

        @Test
        @DisplayName("Should have non-null error codes")
        void shouldHaveNonNullErrorCodes() {
            assertThat(ErrorCode.USER_NOT_FOUND).isNotNull();
            assertThat(ErrorCode.USER_ALREADY_EXISTS).isNotNull();
            assertThat(ErrorCode.USER_VALIDATION_FAILED).isNotNull();
            assertThat(ErrorCode.USER_CREATION_FAILED).isNotNull();
            assertThat(ErrorCode.USER_UPDATE_FAILED).isNotNull();
            assertThat(ErrorCode.USER_DELETION_FAILED).isNotNull();
            assertThat(ErrorCode.THRIFT_SERVICE_ERROR).isNotNull();
            assertThat(ErrorCode.THRIFT_SERVICE_UNAVAILABLE).isNotNull();
            assertThat(ErrorCode.THRIFT_SERVICE_TIMEOUT).isNotNull();
            assertThat(ErrorCode.THRIFT_SERVICE_CONNECTION_FAILED).isNotNull();
            assertThat(ErrorCode.DATABASE_ERROR).isNotNull();
            assertThat(ErrorCode.DATABASE_CONNECTION_FAILED).isNotNull();
            assertThat(ErrorCode.DATABASE_CONSTRAINT_VIOLATION).isNotNull();
            assertThat(ErrorCode.DATABASE_TIMEOUT).isNotNull();
            assertThat(ErrorCode.VALIDATION_ERROR).isNotNull();
            assertThat(ErrorCode.INVALID_INPUT).isNotNull();
            assertThat(ErrorCode.MISSING_REQUIRED_FIELD).isNotNull();
            assertThat(ErrorCode.INVALID_FORMAT).isNotNull();
            assertThat(ErrorCode.INTERNAL_SERVER_ERROR).isNotNull();
            assertThat(ErrorCode.SERVICE_UNAVAILABLE).isNotNull();
            assertThat(ErrorCode.RATE_LIMIT_EXCEEDED).isNotNull();
        }

        @Test
        @DisplayName("Should have non-empty error codes")
        void shouldHaveNonEmptyErrorCodes() {
            assertThat(ErrorCode.USER_NOT_FOUND).isNotEmpty();
            assertThat(ErrorCode.USER_ALREADY_EXISTS).isNotEmpty();
            assertThat(ErrorCode.USER_VALIDATION_FAILED).isNotEmpty();
            assertThat(ErrorCode.USER_CREATION_FAILED).isNotEmpty();
            assertThat(ErrorCode.USER_UPDATE_FAILED).isNotEmpty();
            assertThat(ErrorCode.USER_DELETION_FAILED).isNotEmpty();
            assertThat(ErrorCode.THRIFT_SERVICE_ERROR).isNotEmpty();
            assertThat(ErrorCode.THRIFT_SERVICE_UNAVAILABLE).isNotEmpty();
            assertThat(ErrorCode.THRIFT_SERVICE_TIMEOUT).isNotEmpty();
            assertThat(ErrorCode.THRIFT_SERVICE_CONNECTION_FAILED).isNotEmpty();
            assertThat(ErrorCode.DATABASE_ERROR).isNotEmpty();
            assertThat(ErrorCode.DATABASE_CONNECTION_FAILED).isNotEmpty();
            assertThat(ErrorCode.DATABASE_CONSTRAINT_VIOLATION).isNotEmpty();
            assertThat(ErrorCode.DATABASE_TIMEOUT).isNotEmpty();
            assertThat(ErrorCode.VALIDATION_ERROR).isNotEmpty();
            assertThat(ErrorCode.INVALID_INPUT).isNotEmpty();
            assertThat(ErrorCode.MISSING_REQUIRED_FIELD).isNotEmpty();
            assertThat(ErrorCode.INVALID_FORMAT).isNotEmpty();
            assertThat(ErrorCode.INTERNAL_SERVER_ERROR).isNotEmpty();
            assertThat(ErrorCode.SERVICE_UNAVAILABLE).isNotEmpty();
            assertThat(ErrorCode.RATE_LIMIT_EXCEEDED).isNotEmpty();
        }

        @Test
        @DisplayName("Should have uppercase error codes")
        void shouldHaveUppercaseErrorCodes() {
            assertThat(ErrorCode.USER_NOT_FOUND).isUpperCase();
            assertThat(ErrorCode.USER_ALREADY_EXISTS).isUpperCase();
            assertThat(ErrorCode.USER_VALIDATION_FAILED).isUpperCase();
            assertThat(ErrorCode.USER_CREATION_FAILED).isUpperCase();
            assertThat(ErrorCode.USER_UPDATE_FAILED).isUpperCase();
            assertThat(ErrorCode.USER_DELETION_FAILED).isUpperCase();
            assertThat(ErrorCode.THRIFT_SERVICE_ERROR).isUpperCase();
            assertThat(ErrorCode.THRIFT_SERVICE_UNAVAILABLE).isUpperCase();
            assertThat(ErrorCode.THRIFT_SERVICE_TIMEOUT).isUpperCase();
            assertThat(ErrorCode.THRIFT_SERVICE_CONNECTION_FAILED).isUpperCase();
            assertThat(ErrorCode.DATABASE_ERROR).isUpperCase();
            assertThat(ErrorCode.DATABASE_CONNECTION_FAILED).isUpperCase();
            assertThat(ErrorCode.DATABASE_CONSTRAINT_VIOLATION).isUpperCase();
            assertThat(ErrorCode.DATABASE_TIMEOUT).isUpperCase();
            assertThat(ErrorCode.VALIDATION_ERROR).isUpperCase();
            assertThat(ErrorCode.INVALID_INPUT).isUpperCase();
            assertThat(ErrorCode.MISSING_REQUIRED_FIELD).isUpperCase();
            assertThat(ErrorCode.INVALID_FORMAT).isUpperCase();
            assertThat(ErrorCode.INTERNAL_SERVER_ERROR).isUpperCase();
            assertThat(ErrorCode.SERVICE_UNAVAILABLE).isUpperCase();
            assertThat(ErrorCode.RATE_LIMIT_EXCEEDED).isUpperCase();
        }

        @Test
        @DisplayName("Should have underscore separated error codes")
        void shouldHaveUnderscoreSeparatedErrorCodes() {
            assertThat(ErrorCode.USER_NOT_FOUND).contains("_");
            assertThat(ErrorCode.USER_ALREADY_EXISTS).contains("_");
            assertThat(ErrorCode.USER_VALIDATION_FAILED).contains("_");
            assertThat(ErrorCode.USER_CREATION_FAILED).contains("_");
            assertThat(ErrorCode.USER_UPDATE_FAILED).contains("_");
            assertThat(ErrorCode.USER_DELETION_FAILED).contains("_");
            assertThat(ErrorCode.THRIFT_SERVICE_ERROR).contains("_");
            assertThat(ErrorCode.THRIFT_SERVICE_UNAVAILABLE).contains("_");
            assertThat(ErrorCode.THRIFT_SERVICE_TIMEOUT).contains("_");
            assertThat(ErrorCode.THRIFT_SERVICE_CONNECTION_FAILED).contains("_");
            assertThat(ErrorCode.DATABASE_ERROR).contains("_");
            assertThat(ErrorCode.DATABASE_CONNECTION_FAILED).contains("_");
            assertThat(ErrorCode.DATABASE_CONSTRAINT_VIOLATION).contains("_");
            assertThat(ErrorCode.DATABASE_TIMEOUT).contains("_");
            assertThat(ErrorCode.VALIDATION_ERROR).contains("_");
            assertThat(ErrorCode.INVALID_INPUT).contains("_");
            assertThat(ErrorCode.MISSING_REQUIRED_FIELD).contains("_");
            assertThat(ErrorCode.INVALID_FORMAT).contains("_");
            assertThat(ErrorCode.INTERNAL_SERVER_ERROR).contains("_");
            assertThat(ErrorCode.SERVICE_UNAVAILABLE).contains("_");
            assertThat(ErrorCode.RATE_LIMIT_EXCEEDED).contains("_");
        }
    }

    @Nested
    @DisplayName("Error Code Uniqueness Tests")
    class ErrorCodeUniquenessTests {

        @Test
        @DisplayName("Should have unique error codes")
        void shouldHaveUniqueErrorCodes() {
            String[] allErrorCodes = {
                ErrorCode.USER_NOT_FOUND,
                ErrorCode.USER_ALREADY_EXISTS,
                ErrorCode.USER_VALIDATION_FAILED,
                ErrorCode.USER_CREATION_FAILED,
                ErrorCode.USER_UPDATE_FAILED,
                ErrorCode.USER_DELETION_FAILED,
                ErrorCode.THRIFT_SERVICE_ERROR,
                ErrorCode.THRIFT_SERVICE_UNAVAILABLE,
                ErrorCode.THRIFT_SERVICE_TIMEOUT,
                ErrorCode.THRIFT_SERVICE_CONNECTION_FAILED,
                ErrorCode.DATABASE_ERROR,
                ErrorCode.DATABASE_CONNECTION_FAILED,
                ErrorCode.DATABASE_CONSTRAINT_VIOLATION,
                ErrorCode.DATABASE_TIMEOUT,
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.INVALID_INPUT,
                ErrorCode.MISSING_REQUIRED_FIELD,
                ErrorCode.INVALID_FORMAT,
                ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.SERVICE_UNAVAILABLE,
                ErrorCode.RATE_LIMIT_EXCEEDED
            };

            // Check that all error codes are unique
            for (int i = 0; i < allErrorCodes.length; i++) {
                for (int j = i + 1; j < allErrorCodes.length; j++) {
                    assertThat(allErrorCodes[i])
                        .withFailMessage("Error codes should be unique: %s and %s are the same", 
                                       allErrorCodes[i], allErrorCodes[j])
                        .isNotEqualTo(allErrorCodes[j]);
                }
            }
        }
    }

    @Nested
    @DisplayName("Error Code Consistency Tests")
    class ErrorCodeConsistencyTests {

        @Test
        @DisplayName("Should have consistent naming pattern")
        void shouldHaveConsistentNamingPattern() {
            // All error codes should follow the pattern: CATEGORY_SPECIFIC_ERROR
            assertThat(ErrorCode.USER_NOT_FOUND).matches("^[A-Z_]+$");
            assertThat(ErrorCode.USER_ALREADY_EXISTS).matches("^[A-Z_]+$");
            assertThat(ErrorCode.USER_VALIDATION_FAILED).matches("^[A-Z_]+$");
            assertThat(ErrorCode.USER_CREATION_FAILED).matches("^[A-Z_]+$");
            assertThat(ErrorCode.USER_UPDATE_FAILED).matches("^[A-Z_]+$");
            assertThat(ErrorCode.USER_DELETION_FAILED).matches("^[A-Z_]+$");
            assertThat(ErrorCode.THRIFT_SERVICE_ERROR).matches("^[A-Z_]+$");
            assertThat(ErrorCode.THRIFT_SERVICE_UNAVAILABLE).matches("^[A-Z_]+$");
            assertThat(ErrorCode.THRIFT_SERVICE_TIMEOUT).matches("^[A-Z_]+$");
            assertThat(ErrorCode.THRIFT_SERVICE_CONNECTION_FAILED).matches("^[A-Z_]+$");
            assertThat(ErrorCode.DATABASE_ERROR).matches("^[A-Z_]+$");
            assertThat(ErrorCode.DATABASE_CONNECTION_FAILED).matches("^[A-Z_]+$");
            assertThat(ErrorCode.DATABASE_CONSTRAINT_VIOLATION).matches("^[A-Z_]+$");
            assertThat(ErrorCode.DATABASE_TIMEOUT).matches("^[A-Z_]+$");
            assertThat(ErrorCode.VALIDATION_ERROR).matches("^[A-Z_]+$");
            assertThat(ErrorCode.INVALID_INPUT).matches("^[A-Z_]+$");
            assertThat(ErrorCode.MISSING_REQUIRED_FIELD).matches("^[A-Z_]+$");
            assertThat(ErrorCode.INVALID_FORMAT).matches("^[A-Z_]+$");
            assertThat(ErrorCode.INTERNAL_SERVER_ERROR).matches("^[A-Z_]+$");
            assertThat(ErrorCode.SERVICE_UNAVAILABLE).matches("^[A-Z_]+$");
            assertThat(ErrorCode.RATE_LIMIT_EXCEEDED).matches("^[A-Z_]+$");
        }

        @Test
        @DisplayName("Should have meaningful error code names")
        void shouldHaveMeaningfulErrorCodeNames() {
            // Test that error codes are descriptive and meaningful
            assertThat(ErrorCode.USER_NOT_FOUND).contains("USER").contains("NOT_FOUND");
            assertThat(ErrorCode.USER_ALREADY_EXISTS).contains("USER").contains("ALREADY_EXISTS");
            assertThat(ErrorCode.USER_VALIDATION_FAILED).contains("USER").contains("VALIDATION");
            assertThat(ErrorCode.THRIFT_SERVICE_ERROR).contains("THRIFT").contains("SERVICE");
            assertThat(ErrorCode.THRIFT_SERVICE_UNAVAILABLE).contains("THRIFT").contains("UNAVAILABLE");
            assertThat(ErrorCode.DATABASE_ERROR).contains("DATABASE");
            assertThat(ErrorCode.DATABASE_CONNECTION_FAILED).contains("DATABASE").contains("CONNECTION");
            assertThat(ErrorCode.VALIDATION_ERROR).contains("VALIDATION");
            assertThat(ErrorCode.INVALID_INPUT).contains("INVALID").contains("INPUT");
            assertThat(ErrorCode.INTERNAL_SERVER_ERROR).contains("INTERNAL").contains("SERVER");
            assertThat(ErrorCode.SERVICE_UNAVAILABLE).contains("SERVICE").contains("UNAVAILABLE");
        }
    }
}
