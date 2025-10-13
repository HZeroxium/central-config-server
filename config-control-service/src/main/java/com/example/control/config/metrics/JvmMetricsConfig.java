package com.example.control.config.metrics;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for JVM and system metrics.
 * <p>
 * This configuration enables comprehensive JVM monitoring including:
 * <ul>
 *   <li>Memory usage and heap pressure</li>
 *   <li>Garbage collection statistics</li>
 *   <li>Thread metrics</li>
 *   <li>Class loader metrics</li>
 *   <li>System metrics (CPU, uptime)</li>
 * </ul>
 * <p>
 * All metrics are automatically registered with the MeterRegistry
 * and will be exposed via the Prometheus endpoint.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class JvmMetricsConfig {

    /**
     * JVM Memory metrics.
     * <p>
     * Provides metrics for:
     * <ul>
     *   <li>Heap memory usage</li>
     *   <li>Non-heap memory usage</li>
     *   <li>Memory pool statistics</li>
     * </ul>
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        log.info("Enabled JVM memory metrics");
        return new JvmMemoryMetrics();
    }

    /**
     * JVM Garbage Collection metrics.
     * <p>
     * Provides metrics for:
     * <ul>
     *   <li>GC pause times</li>
     *   <li>GC collection counts</li>
     *   <li>GC memory allocation rates</li>
     * </ul>
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        log.info("Enabled JVM garbage collection metrics");
        return new JvmGcMetrics();
    }

    /**
     * JVM Thread metrics.
     * <p>
     * Provides metrics for:
     * <ul>
     *   <li>Live thread count</li>
     *   <li>Daemon thread count</li>
     *   <li>Peak thread count</li>
     *   <li>Thread states</li>
     * </ul>
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        log.info("Enabled JVM thread metrics");
        return new JvmThreadMetrics();
    }

    /**
     * JVM Class Loader metrics.
     * <p>
     * Provides metrics for:
     * <ul>
     *   <li>Loaded class count</li>
     *   <li>Unloaded class count</li>
     * </ul>
     */
    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        log.info("Enabled class loader metrics");
        return new ClassLoaderMetrics();
    }

    /**
     * JVM Heap Pressure metrics.
     * <p>
     * Provides metrics for heap pressure indicators
     * that can help predict OutOfMemoryError conditions.
     */
    @Bean
    public JvmHeapPressureMetrics jvmHeapPressureMetrics() {
        log.info("Enabled JVM heap pressure metrics");
        return new JvmHeapPressureMetrics();
    }

    /**
     * System Processor metrics.
     * <p>
     * Provides metrics for:
     * <ul>
     *   <li>CPU usage</li>
     *   <li>Load average</li>
     * </ul>
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        log.info("Enabled processor metrics");
        return new ProcessorMetrics();
    }

    /**
     * System Uptime metrics.
     * <p>
     * Provides metrics for:
     * <ul>
     *   <li>Application uptime</li>
     *   <li>Process uptime</li>
     * </ul>
     */
    @Bean
    public UptimeMetrics uptimeMetrics() {
        log.info("Enabled uptime metrics");
        return new UptimeMetrics();
    }
}
