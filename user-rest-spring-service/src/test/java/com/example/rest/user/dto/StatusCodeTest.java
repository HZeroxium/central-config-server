package com.example.rest.user.dto;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StatusCode utility class.
 * Tests status code constants, HTTP status mapping, and message mapping.
 */
@DisplayName("StatusCode Tests")
class StatusCodeTest {

    @Nested
    @DisplayName("Status Code Constants Tests")
    class StatusCodeConstantsTests {

        @Test
        @DisplayName("Should have correct success code")
        void shouldHaveCorrectSuccessCode() {
            assertThat(StatusCode.SUCCESS).isEqualTo(0);
        }

        @Test
        @DisplayName("Should have correct client error codes")
        void shouldHaveCorrectClientErrorCodes() {
            assertThat(StatusCode.VALIDATION_ERROR).isEqualTo(1);
            assertThat(StatusCode.USER_NOT_FOUND).isEqualTo(2);
            assertThat(StatusCode.USER_ALREADY_EXISTS).isEqualTo(3);
            assertThat(StatusCode.INVALID_INPUT).isEqualTo(4);
            assertThat(StatusCode.MISSING_REQUIRED_FIELD).isEqualTo(5);
            assertThat(StatusCode.INVALID_FORMAT).isEqualTo(6);
            assertThat(StatusCode.UNAUTHORIZED).isEqualTo(7);
            assertThat(StatusCode.FORBIDDEN).isEqualTo(8);
            assertThat(StatusCode.NOT_FOUND).isEqualTo(9);
            assertThat(StatusCode.METHOD_NOT_ALLOWED).isEqualTo(10);
            assertThat(StatusCode.CONFLICT).isEqualTo(11);
            assertThat(StatusCode.UNPROCESSABLE_ENTITY).isEqualTo(12);
            assertThat(StatusCode.TOO_MANY_REQUESTS).isEqualTo(13);
        }

        @Test
        @DisplayName("Should have correct server error codes")
        void shouldHaveCorrectServerErrorCodes() {
            assertThat(StatusCode.INTERNAL_SERVER_ERROR).isEqualTo(100);
            assertThat(StatusCode.DATABASE_ERROR).isEqualTo(101);
            assertThat(StatusCode.DATABASE_CONNECTION_FAILED).isEqualTo(102);
            assertThat(StatusCode.DATABASE_TIMEOUT).isEqualTo(103);
            assertThat(StatusCode.DATABASE_CONSTRAINT_VIOLATION).isEqualTo(104);
            assertThat(StatusCode.THRIFT_SERVICE_ERROR).isEqualTo(105);
            assertThat(StatusCode.THRIFT_SERVICE_UNAVAILABLE).isEqualTo(106);
            assertThat(StatusCode.THRIFT_SERVICE_TIMEOUT).isEqualTo(107);
            assertThat(StatusCode.THRIFT_SERVICE_CONNECTION_FAILED).isEqualTo(108);
            assertThat(StatusCode.SERVICE_UNAVAILABLE).isEqualTo(109);
            assertThat(StatusCode.GATEWAY_TIMEOUT).isEqualTo(110);
            assertThat(StatusCode.BAD_GATEWAY).isEqualTo(111);
        }

        @Test
        @DisplayName("Should have correct business logic error codes")
        void shouldHaveCorrectBusinessLogicErrorCodes() {
            assertThat(StatusCode.BUSINESS_RULE_VIOLATION).isEqualTo(200);
            assertThat(StatusCode.INSUFFICIENT_PERMISSIONS).isEqualTo(201);
            assertThat(StatusCode.RESOURCE_LOCKED).isEqualTo(202);
            assertThat(StatusCode.OPERATION_NOT_SUPPORTED).isEqualTo(203);
        }
    }

    @Nested
    @DisplayName("HTTP Status Mapping Tests")
    class HttpStatusMappingTests {

        @Test
        @DisplayName("Should map success code to 200")
        void shouldMapSuccessCodeTo200() {
            assertThat(StatusCode.getHttpStatus(StatusCode.SUCCESS)).isEqualTo(200);
        }

        @Test
        @DisplayName("Should map client error codes to 4xx range")
        void shouldMapClientErrorCodesTo4xxRange() {
            assertThat(StatusCode.getHttpStatus(StatusCode.VALIDATION_ERROR)).isEqualTo(400);
            assertThat(StatusCode.getHttpStatus(StatusCode.USER_NOT_FOUND)).isEqualTo(404);
            assertThat(StatusCode.getHttpStatus(StatusCode.USER_ALREADY_EXISTS)).isEqualTo(409);
            assertThat(StatusCode.getHttpStatus(StatusCode.INVALID_INPUT)).isEqualTo(400);
            assertThat(StatusCode.getHttpStatus(StatusCode.MISSING_REQUIRED_FIELD)).isEqualTo(400);
            assertThat(StatusCode.getHttpStatus(StatusCode.INVALID_FORMAT)).isEqualTo(400);
            assertThat(StatusCode.getHttpStatus(StatusCode.UNAUTHORIZED)).isEqualTo(401);
            assertThat(StatusCode.getHttpStatus(StatusCode.FORBIDDEN)).isEqualTo(403);
            assertThat(StatusCode.getHttpStatus(StatusCode.NOT_FOUND)).isEqualTo(404);
            assertThat(StatusCode.getHttpStatus(StatusCode.METHOD_NOT_ALLOWED)).isEqualTo(405);
            assertThat(StatusCode.getHttpStatus(StatusCode.CONFLICT)).isEqualTo(409);
            assertThat(StatusCode.getHttpStatus(StatusCode.UNPROCESSABLE_ENTITY)).isEqualTo(422);
            assertThat(StatusCode.getHttpStatus(StatusCode.TOO_MANY_REQUESTS)).isEqualTo(429);
        }

        @Test
        @DisplayName("Should map server error codes to 5xx range")
        void shouldMapServerErrorCodesTo5xxRange() {
            assertThat(StatusCode.getHttpStatus(StatusCode.INTERNAL_SERVER_ERROR)).isEqualTo(500);
            assertThat(StatusCode.getHttpStatus(StatusCode.DATABASE_ERROR)).isEqualTo(500);
            assertThat(StatusCode.getHttpStatus(StatusCode.DATABASE_CONNECTION_FAILED)).isEqualTo(503);
            assertThat(StatusCode.getHttpStatus(StatusCode.DATABASE_TIMEOUT)).isEqualTo(504);
            assertThat(StatusCode.getHttpStatus(StatusCode.DATABASE_CONSTRAINT_VIOLATION)).isEqualTo(500);
            assertThat(StatusCode.getHttpStatus(StatusCode.THRIFT_SERVICE_ERROR)).isEqualTo(502);
            assertThat(StatusCode.getHttpStatus(StatusCode.THRIFT_SERVICE_UNAVAILABLE)).isEqualTo(503);
            assertThat(StatusCode.getHttpStatus(StatusCode.THRIFT_SERVICE_TIMEOUT)).isEqualTo(504);
            assertThat(StatusCode.getHttpStatus(StatusCode.THRIFT_SERVICE_CONNECTION_FAILED)).isEqualTo(502);
            assertThat(StatusCode.getHttpStatus(StatusCode.SERVICE_UNAVAILABLE)).isEqualTo(503);
            assertThat(StatusCode.getHttpStatus(StatusCode.GATEWAY_TIMEOUT)).isEqualTo(504);
            assertThat(StatusCode.getHttpStatus(StatusCode.BAD_GATEWAY)).isEqualTo(502);
        }

        @Test
        @DisplayName("Should map business logic error codes to appropriate HTTP status")
        void shouldMapBusinessLogicErrorCodesToAppropriateHttpStatus() {
            assertThat(StatusCode.getHttpStatus(StatusCode.BUSINESS_RULE_VIOLATION)).isEqualTo(422);
            assertThat(StatusCode.getHttpStatus(StatusCode.INSUFFICIENT_PERMISSIONS)).isEqualTo(403);
            assertThat(StatusCode.getHttpStatus(StatusCode.RESOURCE_LOCKED)).isEqualTo(423);
            assertThat(StatusCode.getHttpStatus(StatusCode.OPERATION_NOT_SUPPORTED)).isEqualTo(501);
        }

        @Test
        @DisplayName("Should return 500 for unknown status codes")
        void shouldReturn500ForUnknownStatusCodes() {
            assertThat(StatusCode.getHttpStatus(999)).isEqualTo(500);
            assertThat(StatusCode.getHttpStatus(-1)).isEqualTo(500);
            assertThat(StatusCode.getHttpStatus(50)).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Status Message Mapping Tests")
    class StatusMessageMappingTests {

        @Test
        @DisplayName("Should return correct message for success code")
        void shouldReturnCorrectMessageForSuccessCode() {
            assertThat(StatusCode.getStatusMessage(StatusCode.SUCCESS)).isEqualTo("Success");
        }

        @Test
        @DisplayName("Should return correct messages for client error codes")
        void shouldReturnCorrectMessagesForClientErrorCodes() {
            assertThat(StatusCode.getStatusMessage(StatusCode.VALIDATION_ERROR)).isEqualTo("Validation error");
            assertThat(StatusCode.getStatusMessage(StatusCode.USER_NOT_FOUND)).isEqualTo("User not found");
            assertThat(StatusCode.getStatusMessage(StatusCode.USER_ALREADY_EXISTS)).isEqualTo("User already exists");
            assertThat(StatusCode.getStatusMessage(StatusCode.INVALID_INPUT)).isEqualTo("Invalid input");
            assertThat(StatusCode.getStatusMessage(StatusCode.MISSING_REQUIRED_FIELD)).isEqualTo("Missing required field");
            assertThat(StatusCode.getStatusMessage(StatusCode.INVALID_FORMAT)).isEqualTo("Invalid format");
            assertThat(StatusCode.getStatusMessage(StatusCode.UNAUTHORIZED)).isEqualTo("Unauthorized");
            assertThat(StatusCode.getStatusMessage(StatusCode.FORBIDDEN)).isEqualTo("Forbidden");
            assertThat(StatusCode.getStatusMessage(StatusCode.NOT_FOUND)).isEqualTo("Not found");
            assertThat(StatusCode.getStatusMessage(StatusCode.METHOD_NOT_ALLOWED)).isEqualTo("Method not allowed");
            assertThat(StatusCode.getStatusMessage(StatusCode.CONFLICT)).isEqualTo("Conflict");
            assertThat(StatusCode.getStatusMessage(StatusCode.UNPROCESSABLE_ENTITY)).isEqualTo("Unprocessable entity");
            assertThat(StatusCode.getStatusMessage(StatusCode.TOO_MANY_REQUESTS)).isEqualTo("Too many requests");
        }

        @Test
        @DisplayName("Should return correct messages for server error codes")
        void shouldReturnCorrectMessagesForServerErrorCodes() {
            assertThat(StatusCode.getStatusMessage(StatusCode.INTERNAL_SERVER_ERROR)).isEqualTo("Internal server error");
            assertThat(StatusCode.getStatusMessage(StatusCode.DATABASE_ERROR)).isEqualTo("Database error");
            assertThat(StatusCode.getStatusMessage(StatusCode.DATABASE_CONNECTION_FAILED)).isEqualTo("Database connection failed");
            assertThat(StatusCode.getStatusMessage(StatusCode.DATABASE_TIMEOUT)).isEqualTo("Database timeout");
            assertThat(StatusCode.getStatusMessage(StatusCode.DATABASE_CONSTRAINT_VIOLATION)).isEqualTo("Database constraint violation");
            assertThat(StatusCode.getStatusMessage(StatusCode.THRIFT_SERVICE_ERROR)).isEqualTo("Thrift service error");
            assertThat(StatusCode.getStatusMessage(StatusCode.THRIFT_SERVICE_UNAVAILABLE)).isEqualTo("Thrift service unavailable");
            assertThat(StatusCode.getStatusMessage(StatusCode.THRIFT_SERVICE_TIMEOUT)).isEqualTo("Thrift service timeout");
            assertThat(StatusCode.getStatusMessage(StatusCode.THRIFT_SERVICE_CONNECTION_FAILED)).isEqualTo("Thrift service connection failed");
            assertThat(StatusCode.getStatusMessage(StatusCode.SERVICE_UNAVAILABLE)).isEqualTo("Service unavailable");
            assertThat(StatusCode.getStatusMessage(StatusCode.GATEWAY_TIMEOUT)).isEqualTo("Gateway timeout");
            assertThat(StatusCode.getStatusMessage(StatusCode.BAD_GATEWAY)).isEqualTo("Bad gateway");
        }

        @Test
        @DisplayName("Should return correct messages for business logic error codes")
        void shouldReturnCorrectMessagesForBusinessLogicErrorCodes() {
            assertThat(StatusCode.getStatusMessage(StatusCode.BUSINESS_RULE_VIOLATION)).isEqualTo("Business rule violation");
            assertThat(StatusCode.getStatusMessage(StatusCode.INSUFFICIENT_PERMISSIONS)).isEqualTo("Insufficient permissions");
            assertThat(StatusCode.getStatusMessage(StatusCode.RESOURCE_LOCKED)).isEqualTo("Resource locked");
            assertThat(StatusCode.getStatusMessage(StatusCode.OPERATION_NOT_SUPPORTED)).isEqualTo("Operation not supported");
        }

        @Test
        @DisplayName("Should return unknown error for unknown status codes")
        void shouldReturnUnknownErrorForUnknownStatusCodes() {
            assertThat(StatusCode.getStatusMessage(999)).isEqualTo("Unknown error");
            assertThat(StatusCode.getStatusMessage(-1)).isEqualTo("Unknown error");
            assertThat(StatusCode.getStatusMessage(50)).isEqualTo("Unknown error");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle boundary values correctly")
        void shouldHandleBoundaryValuesCorrectly() {
            // Test exact boundary values
            assertThat(StatusCode.getHttpStatus(0)).isEqualTo(200); // SUCCESS
            assertThat(StatusCode.getHttpStatus(1)).isEqualTo(400); // VALIDATION_ERROR
            assertThat(StatusCode.getHttpStatus(13)).isEqualTo(429); // TOO_MANY_REQUESTS
            assertThat(StatusCode.getHttpStatus(100)).isEqualTo(500); // INTERNAL_SERVER_ERROR
            assertThat(StatusCode.getHttpStatus(111)).isEqualTo(502); // BAD_GATEWAY
            assertThat(StatusCode.getHttpStatus(200)).isEqualTo(422); // BUSINESS_RULE_VIOLATION
            assertThat(StatusCode.getHttpStatus(203)).isEqualTo(501); // OPERATION_NOT_SUPPORTED
        }

        @Test
        @DisplayName("Should handle gaps in status code ranges")
        void shouldHandleGapsInStatusCodeRanges() {
            // Test values in gaps between ranges
            assertThat(StatusCode.getHttpStatus(14)).isEqualTo(500); // Gap between client and server errors
            assertThat(StatusCode.getHttpStatus(99)).isEqualTo(500); // Gap between client and server errors
            assertThat(StatusCode.getHttpStatus(199)).isEqualTo(500); // Gap between server and business errors
            assertThat(StatusCode.getHttpStatus(204)).isEqualTo(500); // Gap after business errors
        }

        @Test
        @DisplayName("Should be consistent with message and HTTP status mapping")
        void shouldBeConsistentWithMessageAndHttpStatusMapping() {
            // Test that all defined status codes have both message and HTTP status
            int[] allStatusCodes = {
                StatusCode.SUCCESS,
                StatusCode.VALIDATION_ERROR, StatusCode.USER_NOT_FOUND, StatusCode.USER_ALREADY_EXISTS,
                StatusCode.INVALID_INPUT, StatusCode.MISSING_REQUIRED_FIELD, StatusCode.INVALID_FORMAT,
                StatusCode.UNAUTHORIZED, StatusCode.FORBIDDEN, StatusCode.NOT_FOUND,
                StatusCode.METHOD_NOT_ALLOWED, StatusCode.CONFLICT, StatusCode.UNPROCESSABLE_ENTITY,
                StatusCode.TOO_MANY_REQUESTS,
                StatusCode.INTERNAL_SERVER_ERROR, StatusCode.DATABASE_ERROR, StatusCode.DATABASE_CONNECTION_FAILED,
                StatusCode.DATABASE_TIMEOUT, StatusCode.DATABASE_CONSTRAINT_VIOLATION,
                StatusCode.THRIFT_SERVICE_ERROR, StatusCode.THRIFT_SERVICE_UNAVAILABLE,
                StatusCode.THRIFT_SERVICE_TIMEOUT, StatusCode.THRIFT_SERVICE_CONNECTION_FAILED,
                StatusCode.SERVICE_UNAVAILABLE, StatusCode.GATEWAY_TIMEOUT, StatusCode.BAD_GATEWAY,
                StatusCode.BUSINESS_RULE_VIOLATION, StatusCode.INSUFFICIENT_PERMISSIONS,
                StatusCode.RESOURCE_LOCKED, StatusCode.OPERATION_NOT_SUPPORTED
            };

            for (int statusCode : allStatusCodes) {
                String message = StatusCode.getStatusMessage(statusCode);
                int httpStatus = StatusCode.getHttpStatus(statusCode);
                
                assertThat(message).isNotEqualTo("Unknown error");
                // Note: Some status codes legitimately map to 500 (server errors)
                assertThat(httpStatus).isBetween(200, 599);
            }
        }
    }
}
