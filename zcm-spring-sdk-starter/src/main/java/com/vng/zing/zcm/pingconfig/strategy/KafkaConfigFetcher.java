package com.vng.zing.zcm.pingconfig.strategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.pingconfig.metrics.PingMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Fetches Kafka configuration from config-control-service.
 * <p>
 * This fetcher retrieves Kafka bootstrap servers and topic name from the
 * control service's infrastructure endpoint. It supports API key authentication
 * and handles errors gracefully with fallback to property-based configuration.
 */
@Slf4j
public class KafkaConfigFetcher {

    private final RestClient restClient;
    private final SdkProperties sdkProperties;
    private final Environment environment;
    private final PingMetrics pingMetrics; // Optional - can be null

    /**
     * Creates a new KafkaConfigFetcher.
     *
     * @param restClient    RestClient for HTTP requests
     * @param sdkProperties SDK configuration properties
     * @param environment   Spring environment for env var overrides
     * @param pingMetrics   Ping metrics component (optional, can be null)
     */
    public KafkaConfigFetcher(RestClient restClient, SdkProperties sdkProperties, Environment environment, PingMetrics pingMetrics) {
        this.restClient = restClient;
        this.sdkProperties = sdkProperties;
        this.environment = environment;
        this.pingMetrics = pingMetrics;
    }

    /**
     * Fetches Kafka configuration from config-control-service.
     * <p>
     * Attempts to GET /api/infrastructure/kafka-config from the control service.
     * Falls back to property/environment-based configuration if fetch fails.
     *
     * @return KafkaConfig with bootstrap servers and topic, or null if fetch fails and no fallback
     */
    public KafkaConfig fetch() {
        String controlUrl = sdkProperties.getControlUrl();
        if (!StringUtils.hasText(controlUrl)) {
            log.warn("Control URL not configured, cannot fetch Kafka config from config-control-service");
            return getFallbackConfig();
        }

        try {
            // Record metrics (if available)
            if (pingMetrics != null) {
                pingMetrics.recordKafkaConfigFetch();
            }

            String url = controlUrl + "/api/infrastructure/kafka-config";
            log.debug("Fetching Kafka config from config-control-service: {}", url);

            var requestBuilder = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON);

            // Add API key header if configured and enabled
            if (sdkProperties.getApiKey() != null
                    && sdkProperties.getApiKey().isEnabled()
                    && StringUtils.hasText(sdkProperties.getApiKey().getKey())) {
                requestBuilder.header("X-API-Key", sdkProperties.getApiKey().getKey());
                log.debug("Including API key in Kafka config fetch request");
            }

            KafkaConfigResponse response = requestBuilder
                    .retrieve()
                    .body(KafkaConfigResponse.class);

            if (response != null && StringUtils.hasText(response.getBootstrapServers())
                    && StringUtils.hasText(response.getTopic())) {
                KafkaConfig config = new KafkaConfig(response.getBootstrapServers(), response.getTopic());
                log.info("Successfully fetched Kafka config from config-control-service: bootstrapServers={}, topic={}",
                        config.bootstrapServers(), config.topic());
                return config;
            } else {
                log.warn("Invalid Kafka config response from config-control-service, using fallback");
                return getFallbackConfig();
            }
        } catch (Exception e) {
            // Record failure metrics (if available)
            if (pingMetrics != null) {
                pingMetrics.recordKafkaConfigFetchFailure();
            }

            log.warn("Failed to fetch Kafka config from config-control-service: {}. Using fallback configuration.",
                    e.getMessage());
            log.debug("Kafka config fetch exception details", e);
            return getFallbackConfig();
        }
    }

    /**
     * Gets fallback Kafka configuration from properties or environment variables.
     *
     * @return KafkaConfig from properties/env vars, or null if not available
     */
    private KafkaConfig getFallbackConfig() {
        // Try environment variables first
        String bootstrapServers = environment.getProperty("ZCM_SDK_PING_KAFKA_BOOTSTRAP_SERVERS",
                sdkProperties.getPing().getKafka().getBootstrapServers());
        String topic = environment.getProperty("ZCM_SDK_PING_KAFKA_TOPIC",
                sdkProperties.getPing().getKafka().getTopic());

        // If still not set, try spring.kafka.bootstrap-servers (common Spring property)
        if (!StringUtils.hasText(bootstrapServers)) {
            bootstrapServers = environment.getProperty("spring.kafka.bootstrap-servers");
        }

        if (StringUtils.hasText(bootstrapServers) && StringUtils.hasText(topic)) {
            KafkaConfig config = new KafkaConfig(bootstrapServers, topic);
            log.debug("Using fallback Kafka config: bootstrapServers={}, topic={}",
                    config.bootstrapServers(), config.topic());
            return config;
        }

        log.warn("No fallback Kafka configuration available (neither properties nor env vars set)");
        return null;
    }

    /**
     * Response DTO from config-control-service Kafka config endpoint.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class KafkaConfigResponse {
        @JsonProperty("bootstrapServers")
        private String bootstrapServers;

        @JsonProperty("topic")
        private String topic;

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public String getTopic() {
            return topic;
        }
    }
}

