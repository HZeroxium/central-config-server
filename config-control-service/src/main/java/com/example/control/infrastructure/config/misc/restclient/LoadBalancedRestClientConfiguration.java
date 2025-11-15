package com.example.control.infrastructure.config.misc.restclient;

import com.example.control.infrastructure.config.misc.DeadlinePropagationClientRequestFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for LoadBalanced RestClient beans with service discovery support.
 * <p>
 * Creates @LoadBalanced RestClient instances that can resolve service names
 * via Spring Cloud LoadBalancer (Consul service discovery).
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LoadBalancedRestClientConfiguration {

    private final RestClientProperties restClientProperties;
    private final DeadlinePropagationClientRequestFilter deadlinePropagationFilter;

    /**
     * Create LoadBalanced RestClient for Config Server with service discovery support.
     * <p>
     * This RestClient can resolve service names (e.g., "http://config-server/...")
     * via Spring Cloud LoadBalancer, which uses Consul for service discovery.
     * </p>
     *
     * @return LoadBalanced RestClient bean for config-server
     */
    @Bean(name = "loadBalancedConfigServerRestClient")
    @LoadBalanced
    public RestClient loadBalancedConfigServerRestClient() {
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

        log.info("Creating @LoadBalanced RestClient for config-server with service discovery support. " +
                "Timeouts: connect={}, read={}, write={}",
                connectTimeout, readTimeout, writeTimeout);

        ClientHttpRequestFactory requestFactory = createRequestFactory(
                connectTimeout, readTimeout, writeTimeout);

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor(deadlinePropagationFilter)
                .build();

        log.debug("@LoadBalanced RestClient for config-server created successfully");
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

