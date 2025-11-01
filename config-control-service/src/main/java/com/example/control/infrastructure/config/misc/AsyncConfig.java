package com.example.control.infrastructure.config.misc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for async execution with separate thread pools per workload type.
 * <p>
 * Provides three thread pool executors:
 * <ul>
 *   <li><b>notificationExecutor</b>: For email notifications (I/O bound, moderate throughput)</li>
 *   <li><b>rpcExecutor</b>: For RPC server startup (rare, minimal concurrency)</li>
 *   <li><b>defaultExecutor</b>: Fallback for other async operations</li>
 * </ul>
 * </p>
 * <p>
 * Features:
 * <ul>
 *   <li>MDC and SecurityContext propagation via TaskDecorator</li>
 *   <li>Global async exception handler with metrics</li>
 *   <li>Micrometer metrics integration</li>
 *   <li>Graceful shutdown with configurable timeout</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

  private final MeterRegistry meterRegistry;
  private ThreadPoolTaskExecutor defaultExecutorBean;

  public AsyncConfig(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  // Notification executor configuration
  @Value("${app.async.notification.core-pool-size:4}")
  private int notificationCorePoolSize;

  @Value("${app.async.notification.max-pool-size:8}")
  private int notificationMaxPoolSize;

  @Value("${app.async.notification.queue-capacity:100}")
  private int notificationQueueCapacity;

  @Value("${app.async.notification.keep-alive-seconds:60}")
  private int notificationKeepAliveSeconds;

  @Value("${app.async.notification.thread-name-prefix:async-notify-}")
  private String notificationThreadNamePrefix;

  // RPC executor configuration
  @Value("${app.async.rpc.core-pool-size:1}")
  private int rpcCorePoolSize;

  @Value("${app.async.rpc.max-pool-size:2}")
  private int rpcMaxPoolSize;

  @Value("${app.async.rpc.queue-capacity:10}")
  private int rpcQueueCapacity;

  @Value("${app.async.rpc.keep-alive-seconds:30}")
  private int rpcKeepAliveSeconds;

  @Value("${app.async.rpc.thread-name-prefix:async-rpc-}")
  private String rpcThreadNamePrefix;

  // Default executor configuration
  @Value("${app.async.default.core-pool-size:8}")
  private int defaultCorePoolSize;

  @Value("${app.async.default.max-pool-size:16}")
  private int defaultMaxPoolSize;

  @Value("${app.async.default.queue-capacity:200}")
  private int defaultQueueCapacity;

  @Value("${app.async.default.keep-alive-seconds:60}")
  private int defaultKeepAliveSeconds;

  @Value("${app.async.default.thread-name-prefix:async-default-}")
  private String defaultThreadNamePrefix;

  // Shutdown configuration
  @Value("${app.async.shutdown.wait-timeout-seconds:30}")
  private int shutdownWaitTimeoutSeconds;

  @Value("${app.async.shutdown.force-shutdown:false}")
  private boolean forceShutdown;

  /**
   * Task decorator bean for context propagation.
   *
   * @return task decorator instance
   */
  @Bean
  public TaskDecorator taskDecorator() {
    return new AsyncTaskDecorator();
  }

  /**
   * Async exception handler bean.
   *
   * @return async exception handler
   */
  @Bean
  public AsyncExceptionHandler asyncExceptionHandler() {
    return new AsyncExceptionHandler(meterRegistry);
  }

  /**
   * Async metrics bean.
   *
   * @return async metrics component
   */
  @Bean
  public AsyncMetrics asyncMetrics() {
    return new AsyncMetrics(meterRegistry);
  }

  /**
   * Notification executor for email notifications.
   * <p>
   * Configured with moderate pool size and bounded queue for I/O-bound email sending.
   * Uses CallerRunsPolicy to throttle when queue is full.
   * </p>
   *
   * @param builder       the task executor builder
   * @param taskDecorator the task decorator for context propagation
   * @param metrics       the async metrics component
   * @return configured notification executor
   */
  @Bean(name = "notificationExecutor")
  public ThreadPoolTaskExecutor notificationExecutor(ThreadPoolTaskExecutorBuilder builder,
      TaskDecorator taskDecorator, AsyncMetrics metrics) {
    ThreadPoolTaskExecutor executor = builder
        .corePoolSize(notificationCorePoolSize)
        .maxPoolSize(notificationMaxPoolSize)
        .queueCapacity(notificationQueueCapacity)
        .keepAlive(Duration.ofSeconds(notificationKeepAliveSeconds))
        .threadNamePrefix(notificationThreadNamePrefix)
        .taskDecorator(taskDecorator)
        .awaitTermination(true)
        .awaitTerminationPeriod(Duration.ofSeconds(shutdownWaitTimeoutSeconds))
        .build();

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.initialize();

    // Register for metrics
    metrics.registerExecutor("notification", executor);

    log.info(
        "Configured notification executor: core={}, max={}, queue={}, keepAlive={}s",
        notificationCorePoolSize,
        notificationMaxPoolSize,
        notificationQueueCapacity,
        notificationKeepAliveSeconds);

    return executor;
  }

  /**
   * RPC executor for Thrift server startup.
   * <p>
   * Configured with minimal pool size since server startup is rare.
   * Uses AbortPolicy as failures are critical.
   * </p>
   *
   * @param builder       the task executor builder
   * @param taskDecorator the task decorator for context propagation
   * @param metrics       the async metrics component
   * @return configured RPC executor
   */
  @Bean(name = "rpcExecutor")
  public ThreadPoolTaskExecutor rpcExecutor(ThreadPoolTaskExecutorBuilder builder,
      TaskDecorator taskDecorator, AsyncMetrics metrics) {
    ThreadPoolTaskExecutor executor = builder
        .corePoolSize(rpcCorePoolSize)
        .maxPoolSize(rpcMaxPoolSize)
        .queueCapacity(rpcQueueCapacity)
        .keepAlive(Duration.ofSeconds(rpcKeepAliveSeconds))
        .threadNamePrefix(rpcThreadNamePrefix)
        .taskDecorator(taskDecorator)
        .awaitTermination(true)
        .awaitTerminationPeriod(Duration.ofSeconds(shutdownWaitTimeoutSeconds))
        .build();

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.initialize();

    // Register for metrics
    metrics.registerExecutor("rpc", executor);

    log.info(
        "Configured RPC executor: core={}, max={}, queue={}, keepAlive={}s",
        rpcCorePoolSize,
        rpcMaxPoolSize,
        rpcQueueCapacity,
        rpcKeepAliveSeconds);

    return executor;
  }

  /**
   * Default executor for general async operations.
   * <p>
   * Larger pool size and queue capacity for general-purpose async tasks.
   * Uses CallerRunsPolicy to throttle when queue is full.
   * </p>
   *
   * @param builder       the task executor builder
   * @param taskDecorator the task decorator for context propagation
   * @param metrics       the async metrics component
   * @return configured default executor
   */
  @Bean(name = "defaultExecutor")
  public ThreadPoolTaskExecutor defaultExecutor(ThreadPoolTaskExecutorBuilder builder,
      TaskDecorator taskDecorator, AsyncMetrics metrics) {
    ThreadPoolTaskExecutor executor = builder
        .corePoolSize(defaultCorePoolSize)
        .maxPoolSize(defaultMaxPoolSize)
        .queueCapacity(defaultQueueCapacity)
        .keepAlive(Duration.ofSeconds(defaultKeepAliveSeconds))
        .threadNamePrefix(defaultThreadNamePrefix)
        .taskDecorator(taskDecorator)
        .awaitTermination(true)
        .awaitTerminationPeriod(Duration.ofSeconds(shutdownWaitTimeoutSeconds))
        .build();

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.initialize();

    // Register for metrics
    metrics.registerExecutor("default", executor);

    // Store reference for getAsyncExecutor()
    this.defaultExecutorBean = executor;

    log.info(
        "Configured default executor: core={}, max={}, queue={}, keepAlive={}s",
        defaultCorePoolSize,
        defaultMaxPoolSize,
        defaultQueueCapacity,
        defaultKeepAliveSeconds);

    return executor;
  }

  /**
   * Returns the default async executor.
   * <p>
   * This is used when @Async is called without specifying an executor name.
   * </p>
   *
   * @return default executor
   */
  @Override
  public Executor getAsyncExecutor() {
    // Return default executor bean - initialized lazily to avoid circular dependency
    if (defaultExecutorBean == null) {
      throw new IllegalStateException(
          "Default executor not initialized. Ensure defaultExecutor bean is created first.");
    }
    return defaultExecutorBean;
  }

  /**
   * Returns the global async exception handler.
   *
   * @return async exception handler
   */
  @Override
  public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return asyncExceptionHandler();
  }
}
