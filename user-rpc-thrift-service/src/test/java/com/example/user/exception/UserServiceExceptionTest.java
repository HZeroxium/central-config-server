package com.example.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive unit tests for UserServiceException and its hierarchy.
 * Tests all constructors, error codes, context handling, and exception chaining.
 */
@DisplayName("UserServiceException Tests")
class UserServiceExceptionTest {

    @Nested
    @DisplayName("UserServiceException Base Class Tests")
    class UserServiceExceptionBaseTests {

        @Test
        @DisplayName("Should create exception with error code and message")
        void shouldCreateExceptionWithErrorCodeAndMessage() {
            // Given
            String errorCode = "TEST_ERROR";
            String message = "Test error message";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isNull();
            assertThat(exception.getContext()).isNull();
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create exception with error code, message, and cause")
        void shouldCreateExceptionWithErrorCodeMessageAndCause() {
            // Given
            String errorCode = "TEST_ERROR";
            String message = "Test error message";
            RuntimeException cause = new RuntimeException("Root cause");

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, cause);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isNull();
            assertThat(exception.getContext()).isNull();
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should create exception with error code, message, and message args")
        void shouldCreateExceptionWithErrorCodeMessageAndMessageArgs() {
            // Given
            String errorCode = "TEST_ERROR";
            String message = "Test error message";
            Object[] messageArgs = {"arg1", "arg2"};

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, messageArgs);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isEqualTo(messageArgs);
            assertThat(exception.getContext()).isNull();
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create exception with error code, message, message args, and context")
        void shouldCreateExceptionWithErrorCodeMessageMessageArgsAndContext() {
            // Given
            String errorCode = "TEST_ERROR";
            String message = "Test error message";
            Object[] messageArgs = {"arg1", "arg2"};
            String context = "test context";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, messageArgs, context);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isEqualTo(messageArgs);
            assertThat(exception.getContext()).isEqualTo(context);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create exception with all parameters")
        void shouldCreateExceptionWithAllParameters() {
            // Given
            String errorCode = "TEST_ERROR";
            String message = "Test error message";
            Object[] messageArgs = {"arg1", "arg2"};
            String context = "test context";
            RuntimeException cause = new RuntimeException("Root cause");

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, messageArgs, context, cause);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isEqualTo(messageArgs);
            assertThat(exception.getContext()).isEqualTo(context);
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @ParameterizedTest
        @DisplayName("Should handle null error code")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        void shouldHandleNullOrBlankErrorCode(String errorCode) {
            // Given
            String message = "Test error message";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("Should handle null message")
        void shouldHandleNullMessage() {
            // Given
            String errorCode = "TEST_ERROR";
            String message = null;

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isNull();
        }

        @Test
        @DisplayName("Should handle null message args")
        void shouldHandleNullMessageArgs() {
            // Given
            String errorCode = "TEST_ERROR";
            String message = "Test error message";
            Object[] messageArgs = null;

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, messageArgs);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isNull();
        }

        @Test
        @DisplayName("Should handle null context")
        void shouldHandleNullContext() {
            // Given
            String errorCode = "TEST_ERROR";
            String message = "Test error message";
            Object[] messageArgs = {"arg1"};
            String context = null;

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, messageArgs, context);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isEqualTo(messageArgs);
            assertThat(exception.getContext()).isNull();
        }
    }

    @Nested
    @DisplayName("DatabaseException Tests")
    class DatabaseExceptionTests {

        @Test
        @DisplayName("Should create database exception with message")
        void shouldCreateDatabaseExceptionWithMessage() {
            // Given
            String message = "Database connection failed";

            // When
            DatabaseException exception = new DatabaseException(message);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("DATABASE_ERROR");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isNull();
            assertThat(exception.getContext()).isNull();
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create database exception with message and cause")
        void shouldCreateDatabaseExceptionWithMessageAndCause() {
            // Given
            String message = "Database connection failed";
            RuntimeException cause = new RuntimeException("Connection timeout");

            // When
            DatabaseException exception = new DatabaseException(message, cause);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("DATABASE_ERROR");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isNull();
            assertThat(exception.getContext()).isNull();
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should create database exception with message and context")
        void shouldCreateDatabaseExceptionWithMessageAndContext() {
            // Given
            String message = "Database operation failed";
            String context = "save operation";

            // When
            DatabaseException exception = new DatabaseException(message, context);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("DATABASE_ERROR");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).containsExactly(context);
            assertThat(exception.getContext()).isEqualTo(context);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create database exception with message, context, and cause")
        void shouldCreateDatabaseExceptionWithMessageContextAndCause() {
            // Given
            String message = "Database operation failed";
            String context = "save operation";
            RuntimeException cause = new RuntimeException("Constraint violation");

            // When
            DatabaseException exception = new DatabaseException(message, context, cause);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("DATABASE_ERROR");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).containsExactly(context);
            assertThat(exception.getContext()).isEqualTo(context);
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should be instance of UserServiceException")
        void shouldBeInstanceOfUserServiceException() {
            // Given
            String message = "Database error";

            // When
            DatabaseException exception = new DatabaseException(message);

            // Then
            assertThat(exception).isInstanceOf(UserServiceException.class);
        }
    }

    @Nested
    @DisplayName("UserNotFoundException Tests")
    class UserNotFoundExceptionTests {

        @Test
        @DisplayName("Should create user not found exception with user ID")
        void shouldCreateUserNotFoundExceptionWithUserId() {
            // Given
            String userId = "user-123";

            // When
            UserNotFoundException exception = new UserNotFoundException(userId);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("USER_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo("User with ID 'user-123' not found");
            assertThat(exception.getMessageArgs()).containsExactly(userId);
            assertThat(exception.getContext()).isNull();
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create user not found exception with user ID and context")
        void shouldCreateUserNotFoundExceptionWithUserIdAndContext() {
            // Given
            String userId = "user-123";
            String context = "Thrift service";

            // When
            UserNotFoundException exception = new UserNotFoundException(userId, context);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("USER_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo("User with ID 'user-123' not found in context: Thrift service");
            assertThat(exception.getMessageArgs()).containsExactly(userId, context);
            assertThat(exception.getContext()).isEqualTo(context);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should handle null user ID")
        void shouldHandleNullUserId() {
            // Given
            String userId = null;

            // When
            UserNotFoundException exception = new UserNotFoundException(userId);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("USER_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo("User with ID 'null' not found");
            assertThat(exception.getMessageArgs()).containsExactly((Object) null);
        }

        @Test
        @DisplayName("Should handle empty user ID")
        void shouldHandleEmptyUserId() {
            // Given
            String userId = "";

            // When
            UserNotFoundException exception = new UserNotFoundException(userId);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("USER_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo("User with ID '' not found");
            assertThat(exception.getMessageArgs()).containsExactly("");
        }

        @Test
        @DisplayName("Should be instance of UserServiceException")
        void shouldBeInstanceOfUserServiceException() {
            // Given
            String userId = "user-123";

            // When
            UserNotFoundException exception = new UserNotFoundException(userId);

            // Then
            assertThat(exception).isInstanceOf(UserServiceException.class);
        }
    }

    @Nested
    @DisplayName("UserValidationException Tests")
    class UserValidationExceptionTests {

        @Test
        @DisplayName("Should create validation exception with message only")
        void shouldCreateValidationExceptionWithMessageOnly() {
            // Given
            String message = "Validation failed";

            // When
            UserValidationException exception = new UserValidationException(message);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("USER_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isNull();
            assertThat(exception.getContext()).isNull();
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getValidationErrors()).isNull();
        }

        @Test
        @DisplayName("Should create validation exception with message and validation errors")
        void shouldCreateValidationExceptionWithMessageAndValidationErrors() {
            // Given
            String message = "Validation failed";
            Map<String, List<String>> validationErrors = Map.of(
                    "name", List.of("Name is required"),
                    "email", List.of("Email format is invalid")
            );

            // When
            UserValidationException exception = new UserValidationException(message, validationErrors);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("USER_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isNull();
            assertThat(exception.getContext()).isNull();
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getValidationErrors()).isEqualTo(validationErrors);
        }

        @Test
        @DisplayName("Should create validation exception with message, validation errors, and context")
        void shouldCreateValidationExceptionWithMessageValidationErrorsAndContext() {
            // Given
            String message = "Validation failed";
            Map<String, List<String>> validationErrors = Map.of(
                    "name", List.of("Name is required")
            );
            String context = "create user";

            // When
            UserValidationException exception = new UserValidationException(message, validationErrors, context);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("USER_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).containsExactly(validationErrors);
            assertThat(exception.getContext()).isEqualTo(context);
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getValidationErrors()).isEqualTo(validationErrors);
        }

        @Test
        @DisplayName("Should handle null validation errors")
        void shouldHandleNullValidationErrors() {
            // Given
            String message = "Validation failed";
            Map<String, List<String>> validationErrors = null;

            // When
            UserValidationException exception = new UserValidationException(message, validationErrors);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("USER_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getValidationErrors()).isNull();
        }

        @Test
        @DisplayName("Should handle empty validation errors")
        void shouldHandleEmptyValidationErrors() {
            // Given
            String message = "Validation failed";
            Map<String, List<String>> validationErrors = Map.of();

            // When
            UserValidationException exception = new UserValidationException(message, validationErrors);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("USER_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getValidationErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should be instance of UserServiceException")
        void shouldBeInstanceOfUserServiceException() {
            // Given
            String message = "Validation failed";

            // When
            UserValidationException exception = new UserValidationException(message);

            // Then
            assertThat(exception).isInstanceOf(UserServiceException.class);
        }
    }

    @Nested
    @DisplayName("Exception Chaining Tests")
    class ExceptionChainingTests {

        @Test
        @DisplayName("Should preserve cause in exception chain")
        void shouldPreserveCauseInExceptionChain() {
            // Given
            RuntimeException rootCause = new RuntimeException("Root cause");
            UserServiceException intermediateException = new UserServiceException("INTERMEDIATE", "Intermediate error", rootCause);
            DatabaseException topException = new DatabaseException("Top level error", intermediateException);

            // When & Then
            assertThat(topException.getCause()).isEqualTo(intermediateException);
            assertThat(topException.getCause().getCause()).isEqualTo(rootCause);
            assertThat(topException.getCause().getCause().getMessage()).isEqualTo("Root cause");
        }

        @Test
        @DisplayName("Should handle null cause gracefully")
        void shouldHandleNullCauseGracefully() {
            // Given
            String message = "Test error";

            // When
            UserServiceException exception = new UserServiceException("TEST_ERROR", message, (Throwable) null);

            // Then
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getMessage()).isEqualTo(message);
        }
    }
}
