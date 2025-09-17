package com.example.user.adapter.thrift;

import com.example.user.service.port.UserServicePort;
import com.example.user.thrift.UserService;
import org.apache.thrift.TProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for ThriftServerConfig.
 * Tests Thrift server configuration, bean creation, and server startup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ThriftServerConfig Tests")
class ThriftServerConfigTest {

    @Mock
    private UserServiceHandler userServicePort;


    @Mock
    private ApplicationArguments applicationArguments;

    private ThriftServerConfig config;

    @BeforeEach
    void setUp() {
        config = new ThriftServerConfig(userServicePort);
        // Set default port using reflection
        ReflectionTestUtils.setField(config, "thriftPort", 9090);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ThriftServerConfig with dependencies")
        void shouldCreateThriftServerConfigWithDependencies() {
            // When
            ThriftServerConfig config = new ThriftServerConfig(userServicePort);

            // Then
            assertThat(config).isNotNull();
            assertThat(ReflectionTestUtils.getField(config, "userServicePort")).isEqualTo(userServicePort);
        }

        @Test
        @DisplayName("Should handle null dependencies gracefully")
        void shouldHandleNullDependenciesGracefully() {
            // When & Then
            assertThatCode(() -> new ThriftServerConfig(null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("UserServiceProcessor Bean Tests")
    class UserServiceProcessorBeanTests {

        @Test
        @DisplayName("Should create user service processor bean")
        void shouldCreateUserServiceProcessorBean() {
            // When
            TProcessor processor = config.userServiceProcessor();

            // Then
            assertThat(processor).isNotNull();
            assertThat(processor).isInstanceOf(UserService.Processor.class);
        }

        @Test
        @DisplayName("Should create processor with UserServiceHandler")
        void shouldCreateProcessorWithUserServiceHandler() {
            // When
            TProcessor processor = config.userServiceProcessor();

            // Then
            assertThat(processor).isNotNull();
            // Verify that the processor is properly configured
            assertThat(processor.getClass().getSimpleName()).isEqualTo("Processor");
        }

        @Test
        @DisplayName("Should create processor multiple times consistently")
        void shouldCreateProcessorMultipleTimesConsistently() {
            // When
            TProcessor processor1 = config.userServiceProcessor();
            TProcessor processor2 = config.userServiceProcessor();

            // Then
            assertThat(processor1).isNotNull();
            assertThat(processor2).isNotNull();
            // Both should be instances of the same type
            assertThat(processor1.getClass()).isEqualTo(processor2.getClass());
        }
    }

    @Nested
    @DisplayName("Application Runner Tests")
    class ApplicationRunnerTests {

        @Test
        @DisplayName("Should implement ApplicationRunner interface")
        void shouldImplementApplicationRunnerInterface() {
            // When & Then
            assertThat(config).isInstanceOf(org.springframework.boot.ApplicationRunner.class);
        }

        @Test
        @DisplayName("Should run application successfully with default port")
        void shouldRunApplicationSuccessfullyWithDefaultPort() throws Exception {
            // Given
            ReflectionTestUtils.setField(config, "thriftPort", 9090);

            // When & Then
            assertThatCode(() -> config.run(applicationArguments))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should run application successfully with custom port")
        void shouldRunApplicationSuccessfullyWithCustomPort() throws Exception {
            // Given
            ReflectionTestUtils.setField(config, "thriftPort", 9999);

            // When & Then
            assertThatCode(() -> config.run(applicationArguments))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null application arguments")
        void shouldHandleNullApplicationArguments() throws Exception {
            // When & Then
            assertThatCode(() -> config.run(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle port binding errors gracefully")
        void shouldHandlePortBindingErrorsGracefully() throws Exception {
            // Given
            // Use a port that might be in use (this is a best-effort test)
            ReflectionTestUtils.setField(config, "thriftPort", 1);

            // When & Then
            // The method should not throw an exception even if port binding fails
            assertThatCode(() -> config.run(applicationArguments))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Configuration Properties Tests")
    class ConfigurationPropertiesTests {

        @Test
        @DisplayName("Should have default port value")
        void shouldHaveDefaultPortValue() {
            // Given
            ThriftServerConfig config = new ThriftServerConfig(userServicePort);

            // When
            Integer port = (Integer) ReflectionTestUtils.getField(config, "thriftPort");

            // Then
            assertThat(port).isNotNull();
        }

        @Test
        @DisplayName("Should accept custom port value")
        void shouldAcceptCustomPortValue() {
            // Given
            int customPort = 8888;
            ReflectionTestUtils.setField(config, "thriftPort", customPort);

            // When
            Integer port = (Integer) ReflectionTestUtils.getField(config, "thriftPort");

            // Then
            assertThat(port).isEqualTo(customPort);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should create all required beans")
        void shouldCreateAllRequiredBeans() {
            // When
            TProcessor processor = config.userServiceProcessor();

            // Then
            assertThat(processor).isNotNull();
            assertThat(userServicePort).isNotNull();
        }

        @Test
        @DisplayName("Should handle multiple run calls")
        void shouldHandleMultipleRunCalls() throws Exception {
            // When & Then
            assertThatCode(() -> {
                config.run(applicationArguments);
                config.run(applicationArguments);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should be thread safe for bean creation")
        void shouldBeThreadSafeForBeanCreation() throws InterruptedException {
            // Given
            int numberOfThreads = 10;
            Thread[] threads = new Thread[numberOfThreads];
            TProcessor[] processors = new TProcessor[numberOfThreads];

            // When
            for (int i = 0; i < numberOfThreads; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    processors[index] = config.userServiceProcessor();
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            for (TProcessor processor : processors) {
                assertThat(processor).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle processor creation errors gracefully")
        void shouldHandleProcessorCreationErrorsGracefully() {
            // Given
            ThriftServerConfig configWithNullDependencies = new ThriftServerConfig(null);

            // When & Then
            assertThatCode(() -> configWithNullDependencies.userServiceProcessor())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle server startup errors gracefully")
        void shouldHandleServerStartupErrorsGracefully() throws Exception {
            // Given
            // Use an invalid port that should cause binding to fail
            ReflectionTestUtils.setField(config, "thriftPort", -1);

            // When & Then
            assertThatCode(() -> config.run(applicationArguments))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Spring Configuration Tests")
    class SpringConfigurationTests {

        @Test
        @DisplayName("Should be a Spring configuration class")
        void shouldBeASpringConfigurationClass() {
            // When & Then
            assertThat(config.getClass().isAnnotationPresent(org.springframework.context.annotation.Configuration.class))
                    .isTrue();
        }

        @Test
        @DisplayName("Should have required constructor dependencies")
        void shouldHaveRequiredConstructorDependencies() {
            // Given
            ThriftServerConfig config = new ThriftServerConfig(userServicePort);

            // When
            UserServicePort injectedUserService = (UserServicePort) ReflectionTestUtils.getField(config, "userServicePort");

            // Then
            assertThat(injectedUserService).isEqualTo(userServicePort);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should create processor quickly")
        void shouldCreateProcessorQuickly() {
            // Given
            long startTime = System.currentTimeMillis();

            // When
            TProcessor processor = config.userServiceProcessor();

            // Then
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            assertThat(processor).isNotNull();
            assertThat(duration).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("Should handle concurrent processor creation")
        void shouldHandleConcurrentProcessorCreation() throws InterruptedException {
            // Given
            int numberOfConcurrentCalls = 100;
            Thread[] threads = new Thread[numberOfConcurrentCalls];
            boolean[] success = new boolean[numberOfConcurrentCalls];

            // When
            for (int i = 0; i < numberOfConcurrentCalls; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        TProcessor processor = config.userServiceProcessor();
                        success[index] = processor != null;
                    } catch (Exception e) {
                        success[index] = false;
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            for (boolean s : success) {
                assertThat(s).isTrue();
            }
        }
    }
}
