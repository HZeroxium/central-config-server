package com.example.control.infrastructure.config.metrics;

import io.micrometer.core.instrument.config.MeterFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MeterFilters to standardize and filter metrics.
 * <p>
 * Provides MeterFilter beans for:
 * <ul>
 * <li>Tag standardization (if needed)</li>
 * <li>High-cardinality metric filtering (if needed)</li>
 * </ul>
 * </p>
 * <p>
 * Currently minimal - only adds filters that provide actual value.
 * Can be extended as needed for specific filtering requirements.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class MetricsFilterConfig {

  /**
   * MeterFilter for tag standardization.
   * <p>
   * Currently not needed as tags are standardized via management.metrics.tags.
   * Reserved for future use if tag renaming becomes necessary.
   * </p>
   *
   * @return MeterFilter for tag standardization (or null if not needed)
   */
  @Bean
  public MeterFilter tagStandardizationFilter() {
    // Currently not needed - tags are already standardized via
    // management.metrics.tags
    // Can be used to rename tags if needed in the future
    // Example: return MeterFilter.renameTag("com.example", "old.tag", "new.tag");
    return MeterFilter.accept();
  }

  /**
   * MeterFilter to deny jvm.gc.pause metrics (high cardinality from GC pause
   * tags).
   *
   * @return MeterFilter denying GC pause metrics
   */
  @Bean
  public MeterFilter denyGcPauseMetrics() {
    log.info("Registering filter: denying jvm.gc.pause metrics");
    return MeterFilter.denyNameStartsWith("jvm.gc.pause");
  }

  /**
   * MeterFilter to deny jvm.buffer.memory.used metrics (rarely used in
   * production).
   *
   * @return MeterFilter denying buffer memory metrics
   */
  @Bean
  public MeterFilter denyBufferMemoryMetrics() {
    log.info("Registering filter: denying jvm.buffer.memory.used metrics");
    return MeterFilter.denyNameStartsWith("jvm.buffer.memory.used");
  }

  /**
   * MeterFilter to limit URI tag cardinality for http.server.requests.
   * <p>
   * Prevents explosion from dynamic paths by limiting unique URI values to 100.
   *
   * @return MeterFilter limiting URI tag cardinality
   */
  @Bean
  public MeterFilter limitUriCardinalityFilter() {
    log.info("Registering filter: limiting http.server.requests URI tag cardinality to 100");
    return MeterFilter.maximumAllowableTags("http.server.requests", "uri", 100, MeterFilter.deny());
  }
}
