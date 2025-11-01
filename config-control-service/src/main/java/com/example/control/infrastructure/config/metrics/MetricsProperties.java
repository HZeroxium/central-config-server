package com.example.control.infrastructure.config.metrics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for custom metrics collection.
 * <p>
 * Binds to {@code app.metrics.*} properties in application.yml and provides
 * type-safe access to metrics configuration with validation.
 * </p>
 *
 * <p>
 * Example configuration:
 *
 * <pre>
 * app:
 *   metrics:
 *     enabled: true
 *     prefix: config_control
 *     tags:
 *       application: config-control-service
 *       service: config-control-service
 *     aspectj:
 *       enabled: true
 *       sampling-rate: 1.0
 *       include-parameters: false
 *     http:
 *       enabled: true
 *       track-payload-size: true
 *       track-response-size: true
 * </pre>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.metrics")
public class MetricsProperties {

  /**
   * Whether metrics collection is enabled globally.
   */
  private boolean enabled = true;

  /**
   * Metric name prefix for all custom metrics.
   */
  @NotBlank(message = "Metrics prefix cannot be blank")
  private String prefix = "config_control";

  /**
   * Default tags to apply to all metrics.
   */
  @NotNull(message = "Metrics tags cannot be null")
  private Map<String, String> tags = new HashMap<>();

  /**
   * AspectJ-based metrics configuration.
   */
  @NotNull(message = "AspectJ configuration is required")
  private AspectjConfig aspectj = new AspectjConfig();

  /**
   * HTTP metrics configuration.
   */
  @NotNull(message = "HTTP configuration is required")
  private HttpConfig http = new HttpConfig();

  /**
   * RPC metrics configuration.
   */
  @NotNull(message = "RPC configuration is required")
  private RpcConfig rpc = new RpcConfig();

  /**
   * Cache metrics configuration.
   */
  @NotNull(message = "Cache configuration is required")
  private CacheConfig cache = new CacheConfig();

  /**
   * Business metrics configuration.
   */
  @NotNull(message = "Business configuration is required")
  private BusinessConfig business = new BusinessConfig();

  /**
   * Histogram configuration.
   */
  @NotNull(message = "Histogram configuration is required")
  private HistogramConfig histogram = new HistogramConfig();

  /**
   * AspectJ-based metrics configuration.
   */
  @Data
  public static class AspectjConfig {
    /**
     * Whether AspectJ metrics are enabled.
     */
    private boolean enabled = true;

    /**
     * Sampling rate (0.0 to 1.0).
     */
    private double samplingRate = 1.0;

    /**
     * Whether to include method parameters in metric names.
     */
    private boolean includeParameters = false;
  }

  /**
   * HTTP metrics configuration.
   */
  @Data
  public static class HttpConfig {
    /**
     * Whether HTTP metrics are enabled.
     */
    private boolean enabled = true;

    /**
     * Whether to track request payload size.
     */
    private boolean trackPayloadSize = true;

    /**
     * Whether to track response payload size.
     */
    private boolean trackResponseSize = true;
  }

  /**
   * RPC metrics configuration.
   */
  @Data
  public static class RpcConfig {
    /**
     * Whether RPC metrics are enabled.
     */
    private boolean enabled = true;

    /**
     * Whether to track Thrift metrics.
     */
    private boolean thrift = true;

    /**
     * Whether to track gRPC metrics.
     */
    private boolean grpc = true;
  }

  /**
   * Cache metrics configuration.
   */
  @Data
  public static class CacheConfig {
    /**
     * Whether cache metrics are enabled.
     */
    private boolean enabled = true;

    /**
     * Whether to track hit/miss ratio.
     */
    private boolean trackHitMissRatio = true;

    /**
     * Whether to track evictions.
     */
    private boolean trackEvictions = true;
  }

  /**
   * Business metrics configuration.
   */
  @Data
  public static class BusinessConfig {
    /**
     * Whether business metrics are enabled.
     */
    private boolean enabled = true;

    /**
     * Whether to track heartbeat metrics.
     */
    private boolean heartbeat = true;

    /**
     * Whether to track drift metrics.
     */
    private boolean drift = true;

    /**
     * Whether to track config refresh metrics.
     */
    private boolean configRefresh = true;

    /**
     * Whether to track instance count metrics.
     */
    private boolean instanceCount = true;

    /**
     * Gauge update interval.
     */
    @NotNull(message = "Gauge update interval is required")
    private Duration gaugeUpdateInterval = Duration.ofSeconds(30);
  }

  /**
   * Histogram configuration.
   */
  @Data
  public static class HistogramConfig {
    /**
     * Percentiles to calculate.
     */
    @NotNull(message = "Percentiles list cannot be null")
    private List<Double> percentiles = new ArrayList<>();

    /**
     * Whether to enable percentiles histogram.
     */
    private boolean percentilesHistogram = true;

    /**
     * SLO boundaries for histograms.
     */
    @NotNull(message = "SLO boundaries list cannot be null")
    private List<Duration> sloBoundaries = new ArrayList<>();

    /**
     * Minimum expected value.
     */
    @NotNull(message = "Minimum expected value is required")
    private Duration minimumExpectedValue = Duration.ofMillis(1);

    /**
     * Maximum expected value.
     */
    @NotNull(message = "Maximum expected value is required")
    private Duration maximumExpectedValue = Duration.ofMinutes(1);
  }
}
