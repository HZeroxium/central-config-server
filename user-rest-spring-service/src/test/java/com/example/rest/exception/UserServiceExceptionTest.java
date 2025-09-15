package com.example.rest.exception;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for UserServiceException and its hierarchy.
 * Tests all constructors, error codes, context handling, and exception chaining.
 */
@DisplayName("UserServiceException Tests")
class UserServiceExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with error code and message")
        void shouldCreateExceptionWithErrorCodeAndMessage() {
            // Given
            String errorCode = ErrorCode.USER_NOT_FOUND;
            String message = "User not found";

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
            String errorCode = ErrorCode.DATABASE_ERROR;
            String message = "Database connection failed";
            RuntimeException cause = new RuntimeException("Connection timeout");

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
            String errorCode = ErrorCode.USER_VALIDATION_FAILED;
            String message = "User validation failed for field: {}";
            Object[] messageArgs = {"email"};

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
            String errorCode = ErrorCode.THRIFT_SERVICE_ERROR;
            String message = "Thrift service call failed for operation: {}";
            Object[] messageArgs = {"createUser"};
            String context = "UserService.create";

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
        @DisplayName("Should create exception with all parameters including cause")
        void shouldCreateExceptionWithAllParametersIncludingCause() {
            // Given
            String errorCode = ErrorCode.DATABASE_CONNECTION_FAILED;
            String message = "Database connection failed for host: {}";
            Object[] messageArgs = {"localhost:5432"};
            String context = "UserRepository.save";
            RuntimeException cause = new RuntimeException("Connection refused");

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, messageArgs, context, cause);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessageArgs()).isEqualTo(messageArgs);
            assertThat(exception.getContext()).isEqualTo(context);
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("Error Code Tests")
    class ErrorCodeTests {

        @Test
        @DisplayName("Should handle user related error codes")
        void shouldHandleUserRelatedErrorCodes() {
            // Given
            String[] userErrorCodes = {
                ErrorCode.USER_NOT_FOUND,
                ErrorCode.USER_ALREADY_EXISTS,
                ErrorCode.USER_VALIDATION_FAILED,
                ErrorCode.USER_CREATION_FAILED,
                ErrorCode.USER_UPDATE_FAILED,
                ErrorCode.USER_DELETION_FAILED
            };

            // When & Then
            for (String errorCode : userErrorCodes) {
                UserServiceException exception = new UserServiceException(errorCode, "Test message");
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            }
        }

        @Test
        @DisplayName("Should handle Thrift service error codes")
        void shouldHandleThriftServiceErrorCodes() {
            // Given
            String[] thriftErrorCodes = {
                ErrorCode.THRIFT_SERVICE_ERROR,
                ErrorCode.THRIFT_SERVICE_UNAVAILABLE,
                ErrorCode.THRIFT_SERVICE_TIMEOUT,
                ErrorCode.THRIFT_SERVICE_CONNECTION_FAILED
            };

            // When & Then
            for (String errorCode : thriftErrorCodes) {
                UserServiceException exception = new UserServiceException(errorCode, "Test message");
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            }
        }

        @Test
        @DisplayName("Should handle database error codes")
        void shouldHandleDatabaseErrorCodes() {
            // Given
            String[] databaseErrorCodes = {
                ErrorCode.DATABASE_ERROR,
                ErrorCode.DATABASE_CONNECTION_FAILED,
                ErrorCode.DATABASE_CONSTRAINT_VIOLATION,
                ErrorCode.DATABASE_TIMEOUT
            };

            // When & Then
            for (String errorCode : databaseErrorCodes) {
                UserServiceException exception = new UserServiceException(errorCode, "Test message");
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            }
        }

        @Test
        @DisplayName("Should handle validation error codes")
        void shouldHandleValidationErrorCodes() {
            // Given
            String[] validationErrorCodes = {
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.INVALID_INPUT,
                ErrorCode.MISSING_REQUIRED_FIELD,
                ErrorCode.INVALID_FORMAT
            };

            // When & Then
            for (String errorCode : validationErrorCodes) {
                UserServiceException exception = new UserServiceException(errorCode, "Test message");
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            }
        }

        @Test
        @DisplayName("Should handle system error codes")
        void shouldHandleSystemErrorCodes() {
            // Given
            String[] systemErrorCodes = {
                ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.SERVICE_UNAVAILABLE,
                ErrorCode.RATE_LIMIT_EXCEEDED
            };

            // When & Then
            for (String errorCode : systemErrorCodes) {
                UserServiceException exception = new UserServiceException(errorCode, "Test message");
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            }
        }
    }

    @Nested
    @DisplayName("Message Args Tests")
    class MessageArgsTests {

        @Test
        @DisplayName("Should handle single message arg")
        void shouldHandleSingleMessageArg() {
            // Given
            String errorCode = ErrorCode.USER_NOT_FOUND;
            String message = "User not found with ID: {}";
            Object[] messageArgs = {"user-123"};

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, messageArgs);

            // Then
            assertThat(exception.getMessageArgs()).isEqualTo(messageArgs);
            assertThat(exception.getMessageArgs()).hasSize(1);
            assertThat(exception.getMessageArgs()[0]).isEqualTo("user-123");
        }

        @Test
        @DisplayName("Should handle multiple message args")
        void shouldHandleMultipleMessageArgs() {
            // Given
            String errorCode = ErrorCode.VALIDATION_ERROR;
            String message = "Validation failed for field: {} with value: {}";
            Object[] messageArgs = {"email", "invalid-email"};

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, messageArgs);

            // Then
            assertThat(exception.getMessageArgs()).isEqualTo(messageArgs);
            assertThat(exception.getMessageArgs()).hasSize(2);
            assertThat(exception.getMessageArgs()[0]).isEqualTo("email");
            assertThat(exception.getMessageArgs()[1]).isEqualTo("invalid-email");
        }

        @Test
        @DisplayName("Should handle empty message args")
        void shouldHandleEmptyMessageArgs() {
            // Given
            String errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
            String message = "Internal server error";
            Object[] messageArgs = {};

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, messageArgs);

            // Then
            assertThat(exception.getMessageArgs()).isEqualTo(messageArgs);
            assertThat(exception.getMessageArgs()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null message args")
        void shouldHandleNullMessageArgs() {
            // Given
            String errorCode = ErrorCode.USER_NOT_FOUND;
            String message = "User not found";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getMessageArgs()).isNull();
        }
    }

    @Nested
    @DisplayName("Context Tests")
    class ContextTests {

        @Test
        @DisplayName("Should handle service context")
        void shouldHandleServiceContext() {
            // Given
            String errorCode = ErrorCode.USER_CREATION_FAILED;
            String message = "Failed to create user";
            String context = "UserService.create";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, null, context);

            // Then
            assertThat(exception.getContext()).isEqualTo(context);
        }

        @Test
        @DisplayName("Should handle repository context")
        void shouldHandleRepositoryContext() {
            // Given
            String errorCode = ErrorCode.DATABASE_ERROR;
            String message = "Database operation failed";
            String context = "UserRepository.save";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, null, context);

            // Then
            assertThat(exception.getContext()).isEqualTo(context);
        }

        @Test
        @DisplayName("Should handle controller context")
        void shouldHandleControllerContext() {
            // Given
            String errorCode = ErrorCode.VALIDATION_ERROR;
            String message = "Request validation failed";
            String context = "UserController.create";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, null, context);

            // Then
            assertThat(exception.getContext()).isEqualTo(context);
        }

        @Test
        @DisplayName("Should handle null context")
        void shouldHandleNullContext() {
            // Given
            String errorCode = ErrorCode.USER_NOT_FOUND;
            String message = "User not found";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getContext()).isNull();
        }
    }

    @Nested
    @DisplayName("Exception Chaining Tests")
    class ExceptionChainingTests {

        @Test
        @DisplayName("Should preserve cause exception")
        void shouldPreserveCauseException() {
            // Given
            String errorCode = ErrorCode.DATABASE_ERROR;
            String message = "Database operation failed";
            RuntimeException cause = new RuntimeException("Connection timeout");

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, cause);

            // Then
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause().getMessage()).isEqualTo("Connection timeout");
        }

        @Test
        @DisplayName("Should preserve nested cause exception")
        void shouldPreserveNestedCauseException() {
            // Given
            String errorCode = ErrorCode.THRIFT_SERVICE_ERROR;
            String message = "Thrift service call failed";
            RuntimeException nestedCause = new RuntimeException("Network error");
            RuntimeException cause = new RuntimeException("Service unavailable", nestedCause);

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, cause);

            // Then
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause().getCause()).isEqualTo(nestedCause);
        }

        @Test
        @DisplayName("Should handle null cause")
        void shouldHandleNullCause() {
            // Given
            String errorCode = ErrorCode.USER_NOT_FOUND;
            String message = "User not found";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle empty error code")
        void shouldHandleEmptyErrorCode() {
            // Given
            String errorCode = "";
            String message = "Test message";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("");
        }

        @Test
        @DisplayName("Should handle null error code")
        void shouldHandleNullErrorCode() {
            // Given
            String errorCode = null;
            String message = "Test message";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getErrorCode()).isNull();
        }

        @Test
        @DisplayName("Should handle empty message")
        void shouldHandleEmptyMessage() {
            // Given
            String errorCode = ErrorCode.USER_NOT_FOUND;
            String message = "";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getMessage()).isEqualTo("");
        }

        @Test
        @DisplayName("Should handle null message")
        void shouldHandleNullMessage() {
            // Given
            String errorCode = ErrorCode.USER_NOT_FOUND;
            String message = null;

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getMessage()).isNull();
        }

        @Test
        @DisplayName("Should handle very long error code")
        void shouldHandleVeryLongErrorCode() {
            // Given
            String errorCode = "VERY_LONG_ERROR_CODE_THAT_EXCEEDS_NORMAL_LENGTH_AND_SHOULD_BE_HANDLED_CORRECTLY";
            String message = "Test message";

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        }

        @Test
        @DisplayName("Should handle very long message")
        void shouldHandleVeryLongMessage() {
            // Given
            String errorCode = ErrorCode.USER_NOT_FOUND;
            String message = "A".repeat(1000);

            // When
            UserServiceException exception = new UserServiceException(errorCode, message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("Should handle very long context")
        void shouldHandleVeryLongContext() {
            // Given
            String errorCode = ErrorCode.USER_NOT_FOUND;
            String message = "Test message";
            String context = "A".repeat(500);

            // When
            UserServiceException exception = new UserServiceException(errorCode, message, null, context);

            // Then
            assertThat(exception.getContext()).isEqualTo(context);
        }
    }
}
