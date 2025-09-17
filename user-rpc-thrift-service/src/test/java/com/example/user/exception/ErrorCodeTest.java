package com.example.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the ErrorCode utility class.
 * Tests that all error codes are properly defined and the class is properly designed.
 */
@DisplayName("ErrorCode Tests")
class ErrorCodeTest {

    @Test
    @DisplayName("Should be a utility class with private constructor")
    void shouldBeUtilityClassWithPrivateConstructor() throws Exception {
        // Given
        Constructor<ErrorCode> constructor = ErrorCode.class.getDeclaredConstructor();

        // When & Then
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(ErrorCode.class.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("Should have all user-related error codes defined")
    void shouldHaveAllUserRelatedErrorCodesDefined() {
        // When & Then
        assertThat(ErrorCode.USER_NOT_FOUND).isEqualTo("USER_NOT_FOUND");
        assertThat(ErrorCode.USER_ALREADY_EXISTS).isEqualTo("USER_ALREADY_EXISTS");
        assertThat(ErrorCode.USER_VALIDATION_FAILED).isEqualTo("USER_VALIDATION_FAILED");
        assertThat(ErrorCode.USER_CREATION_FAILED).isEqualTo("USER_CREATION_FAILED");
        assertThat(ErrorCode.USER_UPDATE_FAILED).isEqualTo("USER_UPDATE_FAILED");
        assertThat(ErrorCode.USER_DELETION_FAILED).isEqualTo("USER_DELETION_FAILED");
    }

    @Test
    @DisplayName("Should have all database-related error codes defined")
    void shouldHaveAllDatabaseRelatedErrorCodesDefined() {
        // When & Then
        assertThat(ErrorCode.DATABASE_ERROR).isEqualTo("DATABASE_ERROR");
        assertThat(ErrorCode.DATABASE_CONNECTION_FAILED).isEqualTo("DATABASE_CONNECTION_FAILED");
        assertThat(ErrorCode.DATABASE_CONSTRAINT_VIOLATION).isEqualTo("DATABASE_CONSTRAINT_VIOLATION");
        assertThat(ErrorCode.DATABASE_TIMEOUT).isEqualTo("DATABASE_TIMEOUT");
    }

    @Test
    @DisplayName("Should have all validation-related error codes defined")
    void shouldHaveAllValidationRelatedErrorCodesDefined() {
        // When & Then
        assertThat(ErrorCode.VALIDATION_ERROR).isEqualTo("VALIDATION_ERROR");
        assertThat(ErrorCode.INVALID_INPUT).isEqualTo("INVALID_INPUT");
        assertThat(ErrorCode.MISSING_REQUIRED_FIELD).isEqualTo("MISSING_REQUIRED_FIELD");
        assertThat(ErrorCode.INVALID_FORMAT).isEqualTo("INVALID_FORMAT");
    }

    @Test
    @DisplayName("Should have all system-related error codes defined")
    void shouldHaveAllSystemRelatedErrorCodesDefined() {
        // When & Then
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(ErrorCode.SERVICE_UNAVAILABLE).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(ErrorCode.RATE_LIMIT_EXCEEDED).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Should have unique error codes")
    void shouldHaveUniqueErrorCodes() {
        // Given
        String[] allErrorCodes = {
                ErrorCode.USER_NOT_FOUND,
                ErrorCode.USER_ALREADY_EXISTS,
                ErrorCode.USER_VALIDATION_FAILED,
                ErrorCode.USER_CREATION_FAILED,
                ErrorCode.USER_UPDATE_FAILED,
                ErrorCode.USER_DELETION_FAILED,
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

        // When & Then
        assertThat(allErrorCodes).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Should have error codes in uppercase with underscores")
    void shouldHaveErrorCodesInUppercaseWithUnderscores() {
        // Given
        String[] allErrorCodes = {
                ErrorCode.USER_NOT_FOUND,
                ErrorCode.USER_ALREADY_EXISTS,
                ErrorCode.USER_VALIDATION_FAILED,
                ErrorCode.USER_CREATION_FAILED,
                ErrorCode.USER_UPDATE_FAILED,
                ErrorCode.USER_DELETION_FAILED,
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

        // When & Then
        for (String errorCode : allErrorCodes) {
            assertThat(errorCode).matches("^[A-Z_]+$");
        }
    }

    @Test
    @DisplayName("Should have descriptive error code names")
    void shouldHaveDescriptiveErrorCodeNames() {
        // When & Then
        assertThat(ErrorCode.USER_NOT_FOUND).contains("USER").contains("NOT_FOUND");
        assertThat(ErrorCode.USER_ALREADY_EXISTS).contains("USER").contains("ALREADY_EXISTS");
        assertThat(ErrorCode.DATABASE_ERROR).contains("DATABASE").contains("ERROR");
        assertThat(ErrorCode.VALIDATION_ERROR).contains("VALIDATION").contains("ERROR");
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR).contains("INTERNAL").contains("SERVER").contains("ERROR");
    }
}
