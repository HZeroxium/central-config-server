package com.example.control.infrastructure.config.restclient;

import com.example.control.infrastructure.config.DeadlinePropagationClientRequestFilter;
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
   * Create RestClient with specified timeouts and deadline propagation.
   *
   * @param clientName     Client name for logging
   * @param connectTimeout Connection timeout
   * @param readTimeout    Read timeout
   * @param writeTimeout   Write timeout
   * @return Configured RestClient
   */
  private RestClient createRestClient(
      String clientName,
      Duration connectTimeout,
      Duration readTimeout,
      Duration writeTimeout) {

    log.info("Creating RestClient '{}' with timeouts: connect={}, read={}, write={}",
        clientName, connectTimeout, readTimeout, writeTimeout);

    ClientHttpRequestFactory requestFactory = createRequestFactory(
        connectTimeout, readTimeout, writeTimeout);

    return RestClient.builder()
        .requestFactory(requestFactory)
        .requestInterceptor(deadlinePropagationFilter)
        .build();
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
