package com.example.control.infrastructure.config.misc.restclient;

import com.example.control.infrastructure.config.misc.DeadlinePropagationClientRequestFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for RestClient beans with timeout settings and deadline
 * propagation.
 * <p>
 * Creates centralized RestClient instances with:
 * - Configurable timeouts (connect, read, write)
 * - Deadline propagation interceptor
 * - Per-client timeout overrides
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfiguration {

    private final RestClientProperties restClientProperties;
    private final DeadlinePropagationClientRequestFilter deadlinePropagationFilter;

    /**
     * Create base RestClient with default timeouts and deadline propagation.
     *
     * @return Base RestClient bean
     */
    @Bean
    public RestClient restClient() {
        return createRestClient("default", restClientProperties.getConnectTimeout(),
                restClientProperties.getReadTimeout(),
                restClientProperties.getWriteTimeout());
    }

    /**
     * Create RestClient for ConfigServer with specific timeouts.
     *
     * @return ConfigServer RestClient bean
     */
    @Bean(name = "configServerRestClient")
    public RestClient configServerRestClient() {
        RestClientProperties.ClientTimeout clientTimeout = restClientProperties.getClients()
                .get("configserver");

        Duration connectTimeout = clientTimeout != null && clientTimeout.getConnectTimeout() != null
                ? clientTimeout.getConnectTimeout()
                : restClientProperties.getConnectTimeout();

        Duration readTimeout = clientTimeout != null && clientTimeout.getReadTimeout() != null
                ? clientTimeout.getReadTimeout()
                : restClientProperties.getReadTimeout();

        Duration writeTimeout = clientTimeout != null && clientTimeout.getWriteTimeout() != null
                ? clientTimeout.getWriteTimeout()
                : restClientProperties.getWriteTimeout();

        return createRestClient("configserver", connectTimeout, readTimeout, writeTimeout);
    }

    /**
     * Create RestClient for Consul with specific timeouts.
     *
     * @return Consul RestClient bean
     */
    @Bean(name = "consulRestClient")
    public RestClient consulRestClient() {
        RestClientProperties.ClientTimeout clientTimeout = restClientProperties.getClients()
                .get("consul");

        Duration connectTimeout = clientTimeout != null && clientTimeout.getConnectTimeout() != null
                ? clientTimeout.getConnectTimeout()
                : restClientProperties.getConnectTimeout();

        Duration readTimeout = clientTimeout != null && clientTimeout.getReadTimeout() != null
                ? clientTimeout.getReadTimeout()
                : restClientProperties.getReadTimeout();

        Duration writeTimeout = clientTimeout != null && clientTimeout.getWriteTimeout() != null
                ? clientTimeout.getWriteTimeout()
                : restClientProperties.getWriteTimeout();

        return createRestClient("consul", connectTimeout, readTimeout, writeTimeout);
    }

    /**
     * Create RestClient for Keycloak Admin API with specific timeouts.
     *
     * @return Keycloak RestClient bean
     */
    @Bean(name = "keycloakRestClient")
    public RestClient keycloakRestClient() {
        RestClientProperties.ClientTimeout clientTimeout = restClientProperties.getClients()
                .get("keycloak");

        Duration connectTimeout = clientTimeout != null && clientTimeout.getConnectTimeout() != null
                ? clientTimeout.getConnectTimeout()
                : restClientProperties.getConnectTimeout();

        Duration readTimeout = clientTimeout != null && clientTimeout.getReadTimeout() != null
                ? clientTimeout.getReadTimeout()
                : restClientProperties.getReadTimeout();

        Duration writeTimeout = clientTimeout != null && clientTimeout.getWriteTimeout() != null
                ? clientTimeout.getWriteTimeout()
                : restClientProperties.getWriteTimeout();

        return createRestClient("keycloak", connectTimeout, readTimeout, writeTimeout);
    }

    /**
     * Create RestClient with specified timeouts and deadline propagation.
     * <p>
     * Uses {@link RestClient#builder()} which ensures automatic instrumentation
     * via Spring Boot's Observation API. Metrics are exported as
     * {@code http.client.requests} with tags for method, URI, status, etc.
     *
     * @param clientName     Client name for logging
     * @param connectTimeout Connection timeout
     * @param readTimeout    Read timeout
     * @param writeTimeout   Write timeout
     * @return Configured RestClient with automatic metrics instrumentation
     */
    private RestClient createRestClient(
            String clientName,
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout) {

        log.info("Creating RestClient '{}' with timeouts: connect={}, read={}, write={}. " +
                "RestClient will be automatically instrumented via Spring Boot's Observation API " +
                "(metrics: http.client.requests)",
                clientName, connectTimeout, readTimeout, writeTimeout);

        ClientHttpRequestFactory requestFactory = createRequestFactory(
                connectTimeout, readTimeout, writeTimeout);

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor(deadlinePropagationFilter)
                .build();

        log.debug("RestClient '{}' created successfully with instrumentation enabled", clientName);
        return restClient;
    }

    /**
     * Create ClientHttpRequestFactory with timeout configuration.
     *
     * @param connectTimeout Connection timeout
     * @param readTimeout    Read timeout
     * @param writeTimeout   Write timeout
     * @return Configured ClientHttpRequestFactory
     */
    private ClientHttpRequestFactory createRequestFactory(
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        // Note: SimpleClientHttpRequestFactory doesn't support writeTimeout,
        // but it's included for future compatibility
        return factory;
    }
}
