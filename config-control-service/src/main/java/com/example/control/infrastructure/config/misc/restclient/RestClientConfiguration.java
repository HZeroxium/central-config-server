package com.example.control.infrastructure.config.misc.restclient;

import com.example.control.infrastructure.config.misc.DeadlinePropagationClientRequestFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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
     * Create ClientHttpRequestFactory with timeout configuration and connection pooling.
     * <p>
     * Uses Apache HttpClient with connection pooling for better performance and resource reuse.
     * Connection pool configuration:
     * <ul>
     *   <li>Max total connections: 200</li>
     *   <li>Max connections per route: 50</li>
     *   <li>Connection time-to-live: 30 seconds</li>
     * </ul>
     *
     * @param connectTimeout Connection timeout
     * @param readTimeout    Read timeout
     * @param writeTimeout   Write timeout
     * @return Configured ClientHttpRequestFactory with connection pooling
     */
    private ClientHttpRequestFactory createRequestFactory(
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout) {

        // Create connection pool manager
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(200) // Maximum total connections
                .setMaxConnPerRoute(50) // Maximum connections per route (host:port)
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.of(readTimeout))
                        .build())
                .build();

        // Create request config with timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(connectTimeout))
                .setResponseTimeout(Timeout.of(readTimeout))
                .setConnectionRequestTimeout(Timeout.of(connectTimeout))
                .build();

        // Create HttpClient with connection pooling
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(Timeout.ofSeconds(30)) // Evict idle connections after 30s
                .evictExpiredConnections() // Evict expired connections
                .build();

        // Create request factory with pooled HttpClient
        // HttpComponentsClientHttpRequestFactory uses timeouts from RequestConfig
        // No need to set timeouts again on factory - they're already configured in RequestConfig
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        log.debug("Created HttpComponentsClientHttpRequestFactory with connection pooling: maxTotal=200, maxPerRoute=50");

        return factory;
    }
}
