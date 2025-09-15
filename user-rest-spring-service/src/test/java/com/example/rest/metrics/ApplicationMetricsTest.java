package com.example.rest.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for ApplicationMetrics class.
 * Tests all metrics collection methods using SimpleMeterRegistry.
 */
@DisplayName("ApplicationMetrics Tests")
class ApplicationMetricsTest {

    private MeterRegistry meterRegistry;
    private ApplicationMetrics applicationMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        applicationMetrics = new ApplicationMetrics(meterRegistry);
    }

    @Nested
    @DisplayName("REST API Metrics Tests")
    class RestApiMetricsTests {

        @Test
        @DisplayName("Should increment REST API requests counter")
        void shouldIncrementRestApiRequestsCounter() {
            // Given
            String endpoint = "/users";
            String method = "GET";

            // When
            applicationMetrics.incrementRestApiRequests(endpoint, method);

            // Then
            Counter counter = meterRegistry.find("rest.api.requests")
                    .tags("service", "user-rest-spring-service", "endpoint", endpoint, "method", method)
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should increment REST API errors counter")
        void shouldIncrementRestApiErrorsCounter() {
            // Given
            String endpoint = "/users";
            String method = "POST";
            String errorType = "ValidationException";

            // When
            applicationMetrics.incrementRestApiErrors(endpoint, method, errorType);

            // Then
            Counter counter = meterRegistry.find("rest.api.errors")
                    .tags("service", "user-rest-spring-service", "endpoint", endpoint, "method", method, "error_type", errorType)
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record REST API duration")
        void shouldRecordRestApiDuration() {
            // Given
            String endpoint = "/users/{id}";
            String method = "GET";
            Timer.Sample sample = applicationMetrics.startRestApiTimer();

            // When
            try {
                Thread.sleep(100); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            applicationMetrics.recordRestApiDuration(sample, endpoint, method);

            // Then
            Timer timer = meterRegistry.find("rest.api.duration")
                    .tags("service", "user-rest-spring-service", "endpoint", endpoint, "method", method)
                    .timer();
            assertThat(timer).isNotNull();
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle multiple REST API requests")
        void shouldHandleMultipleRestApiRequests() {
            // Given
            String endpoint = "/users";
            String method = "GET";

            // When
            applicationMetrics.incrementRestApiRequests(endpoint, method);
            applicationMetrics.incrementRestApiRequests(endpoint, method);
            applicationMetrics.incrementRestApiRequests(endpoint, method);

            // Then
            Counter counter = meterRegistry.find("rest.api.requests")
                    .tags("service", "user-rest-spring-service", "endpoint", endpoint, "method", method)
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Should handle different endpoints and methods")
        void shouldHandleDifferentEndpointsAndMethods() {
            // Given
            String[] endpoints = {"/users", "/users/{id}", "/users/ping"};
            String[] methods = {"GET", "POST", "PUT", "DELETE"};

            // When
            for (String endpoint : endpoints) {
                for (String method : methods) {
                    applicationMetrics.incrementRestApiRequests(endpoint, method);
                }
            }

            // Then
            for (String endpoint : endpoints) {
                for (String method : methods) {
                    Counter counter = meterRegistry.find("rest.api.requests")
                            .tags("service", "user-rest-spring-service", "endpoint", endpoint, "method", method)
                            .counter();
                    assertThat(counter).isNotNull();
                    assertThat(counter.count()).isEqualTo(1.0);
                }
            }
        }
    }

    @Nested
    @DisplayName("Thrift Client Metrics Tests")
    class ThriftClientMetricsTests {

        @Test
        @DisplayName("Should increment Thrift client requests counter")
        void shouldIncrementThriftClientRequestsCounter() {
            // Given
            String operation = "create";

            // When
            applicationMetrics.incrementThriftClientRequests(operation);

            // Then
            Counter counter = meterRegistry.find("thrift.client.requests")
                    .tags("service", "user-rest-spring-service", "operation", operation)
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should increment Thrift client errors counter")
        void shouldIncrementThriftClientErrorsCounter() {
            // Given
            String operation = "getById";
            String errorType = "ThriftServiceException";

            // When
            applicationMetrics.incrementThriftClientErrors(operation, errorType);

            // Then
            Counter counter = meterRegistry.find("thrift.client.errors")
                    .tags("service", "user-rest-spring-service", "operation", operation, "error_type", errorType)
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record Thrift client duration")
        void shouldRecordThriftClientDuration() {
            // Given
            String operation = "update";
            Timer.Sample sample = applicationMetrics.startThriftClientTimer();

            // When
            try {
                Thread.sleep(50); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            applicationMetrics.recordThriftClientDuration(sample, operation);

            // Then
            Timer timer = meterRegistry.find("thrift.client.duration")
                    .tags("service", "user-rest-spring-service", "operation", operation)
                    .timer();
            assertThat(timer).isNotNull();
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle multiple Thrift client operations")
        void shouldHandleMultipleThriftClientOperations() {
            // Given
            String[] operations = {"create", "getById", "update", "delete", "list", "listPaged", "count", "ping"};

            // When
            for (String operation : operations) {
                applicationMetrics.incrementThriftClientRequests(operation);
            }

            // Then
            for (String operation : operations) {
                Counter counter = meterRegistry.find("thrift.client.requests")
                        .tags("service", "user-rest-spring-service", "operation", operation)
                        .counter();
                assertThat(counter).isNotNull();
                assertThat(counter.count()).isEqualTo(1.0);
            }
        }
    }

    @Nested
    @DisplayName("User Operations Metrics Tests")
    class UserOperationsMetricsTests {

        @Test
        @DisplayName("Should increment user operations counter")
        void shouldIncrementUserOperationsCounter() {
            // Given
            String operation = "create";

            // When
            applicationMetrics.incrementUserOperations(operation);

            // Then
            Counter counter = meterRegistry.find("user.operations")
                    .tags("service", "user-rest-spring-service", "operation", operation)
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should increment user operations errors counter")
        void shouldIncrementUserOperationsErrorsCounter() {
            // Given
            String operation = "update";
            String errorType = "ValidationException";

            // When
            applicationMetrics.incrementUserOperationsErrors(operation, errorType);

            // Then
            Counter counter = meterRegistry.find("user.operations.errors")
                    .tags("service", "user-rest-spring-service", "operation", operation, "error_type", errorType)
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record user operation duration")
        void shouldRecordUserOperationDuration() {
            // Given
            String operation = "delete";
            Timer.Sample sample = applicationMetrics.startUserOperationTimer();

            // When
            try {
                Thread.sleep(75); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            applicationMetrics.recordUserOperationDuration(sample, operation);

            // Then
            Timer timer = meterRegistry.find("user.operations.duration")
                    .tags("service", "user-rest-spring-service", "operation", operation)
                    .timer();
            assertThat(timer).isNotNull();
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle multiple user operations")
        void shouldHandleMultipleUserOperations() {
            // Given
            String[] operations = {"create", "get", "update", "delete", "list"};

            // When
            for (String operation : operations) {
                applicationMetrics.incrementUserOperations(operation);
            }

            // Then
            for (String operation : operations) {
                Counter counter = meterRegistry.find("user.operations")
                        .tags("service", "user-rest-spring-service", "operation", operation)
                        .counter();
                assertThat(counter).isNotNull();
                assertThat(counter.count()).isEqualTo(1.0);
            }
        }
    }

    @Nested
    @DisplayName("Custom Metrics Tests")
    class CustomMetricsTests {

        @Test
        @DisplayName("Should record custom metric")
        void shouldRecordCustomMetric() {
            // Given
            String name = "custom.operation";
            String description = "Custom operation metric";
            String[] tags = {"type", "test", "category", "unit"};

            // When
            applicationMetrics.recordCustomMetric(name, description, tags);

            // Then
            Counter counter = meterRegistry.find(name)
                    .tags(tags)
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record custom timer")
        void shouldRecordCustomTimer() {
            // Given
            String name = "custom.timer";
            String description = "Custom timer metric";
            Duration duration = Duration.ofMillis(150);
            String[] tags = {"type", "test", "category", "timer"};

            // When
            applicationMetrics.recordCustomTimer(name, description, duration, tags);

            // Then
            Timer timer = meterRegistry.find(name)
                    .tags(tags)
                    .timer();
            assertThat(timer).isNotNull();
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(150.0);
        }

        @Test
        @DisplayName("Should handle multiple custom metrics")
        void shouldHandleMultipleCustomMetrics() {
            // Given
            String name = "custom.metric";
            String description = "Custom metric";
            String[] tags = {"type", "test"};

            // When
            applicationMetrics.recordCustomMetric(name, description, tags);
            applicationMetrics.recordCustomMetric(name, description, tags);
            applicationMetrics.recordCustomMetric(name, description, tags);

            // Then
            Counter counter = meterRegistry.find(name)
                    .tags(tags)
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle null endpoint gracefully")
        void shouldHandleNullEndpointGracefully() {
            // Given
            String endpoint = null;
            String method = "GET";

            // When & Then - Micrometer doesn't handle null tags gracefully, so we expect NPE
            assertThatThrownBy(() -> applicationMetrics.incrementRestApiRequests(endpoint, method))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null method gracefully")
        void shouldHandleNullMethodGracefully() {
            // Given
            String endpoint = "/users";
            String method = null;

            // When & Then - Micrometer doesn't handle null tags gracefully, so we expect NPE
            assertThatThrownBy(() -> applicationMetrics.incrementRestApiRequests(endpoint, method))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null operation gracefully")
        void shouldHandleNullOperationGracefully() {
            // Given
            String operation = null;

            // When & Then - Micrometer doesn't handle null tags gracefully, so we expect NPE
            assertThatThrownBy(() -> applicationMetrics.incrementThriftClientRequests(operation))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null error type gracefully")
        void shouldHandleNullErrorTypeGracefully() {
            // Given
            String operation = "create";
            String errorType = null;

            // When & Then - Micrometer doesn't handle null tags gracefully, so we expect NPE
            assertThatThrownBy(() -> applicationMetrics.incrementThriftClientErrors(operation, errorType))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle empty strings gracefully")
        void shouldHandleEmptyStringsGracefully() {
            // Given
            String endpoint = "";
            String method = "";
            String operation = "";

            // When & Then
            assertThatCode(() -> {
                applicationMetrics.incrementRestApiRequests(endpoint, method);
                applicationMetrics.incrementThriftClientRequests(operation);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle very long strings gracefully")
        void shouldHandleVeryLongStringsGracefully() {
            // Given
            String longString = "A".repeat(1000);
            String endpoint = longString;
            String method = longString;
            String operation = longString;

            // When & Then
            assertThatCode(() -> {
                applicationMetrics.incrementRestApiRequests(endpoint, method);
                applicationMetrics.incrementThriftClientRequests(operation);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle special characters gracefully")
        void shouldHandleSpecialCharactersGracefully() {
            // Given
            String endpoint = "/users/{id}?param=value&other=test";
            String method = "GET";
            String operation = "create-user";

            // When & Then
            assertThatCode(() -> {
                applicationMetrics.incrementRestApiRequests(endpoint, method);
                applicationMetrics.incrementThriftClientRequests(operation);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Timer Sample Tests")
    class TimerSampleTests {

        @Test
        @DisplayName("Should create valid timer samples")
        void shouldCreateValidTimerSamples() {
            // When
            Timer.Sample restSample = applicationMetrics.startRestApiTimer();
            Timer.Sample thriftSample = applicationMetrics.startThriftClientTimer();
            Timer.Sample userSample = applicationMetrics.startUserOperationTimer();

            // Then
            assertThat(restSample).isNotNull();
            assertThat(thriftSample).isNotNull();
            assertThat(userSample).isNotNull();
        }

        @Test
        @DisplayName("Should handle timer sample recording without errors")
        void shouldHandleTimerSampleRecordingWithoutErrors() {
            // Given
            Timer.Sample sample = applicationMetrics.startRestApiTimer();
            String endpoint = "/users";
            String method = "GET";

            // When & Then
            assertThatCode(() -> applicationMetrics.recordRestApiDuration(sample, endpoint, method))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle multiple timer samples concurrently")
        void shouldHandleMultipleTimerSamplesConcurrently() {
            // Given
            Timer.Sample[] samples = new Timer.Sample[10];

            // When
            for (int i = 0; i < 10; i++) {
                samples[i] = applicationMetrics.startRestApiTimer();
            }

            // Then
            for (int i = 0; i < 10; i++) {
                final int index = i; // Make it effectively final
                assertThat(samples[index]).isNotNull();
                assertThatCode(() -> applicationMetrics.recordRestApiDuration(samples[index], "/users", "GET"))
                        .doesNotThrowAnyException();
            }
        }
    }
}
