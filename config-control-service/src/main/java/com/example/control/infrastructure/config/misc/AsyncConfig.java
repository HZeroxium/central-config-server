package com.example.control.infrastructure.config.misc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for async execution with separate thread pools per workload
 * type.
 * <p>
 * Provides three thread pool executors:
 * <ul>
 * <li><b>notificationExecutor</b>: For email notifications (I/O bound, moderate
 * throughput)</li>
 * <li><b>rpcExecutor</b>: For RPC server startup (rare, minimal
 * concurrency)</li>
 * <li><b>defaultExecutor</b> / <b>taskExecutor</b>: Fallback for other async
 * operations</li>
 * </ul>
 * </p>
 * <p>
 * Features:
 * <ul>
 * <li>MDC propagation via MdcTaskDecorator</li>
 * <li>SecurityContext propagation via
 * DelegatingSecurityContextAsyncTaskExecutor</li>
 * <li>Global async exception handler with metrics</li>
 * <li>Automatic metrics registration via Spring Boot
 * TaskExecutorMetricsAutoConfiguration</li>
 * <li>Graceful shutdown with configurable timeout and force shutdown
 * option</li>
 * </p>
 * <p>
 * Metrics are automatically registered by Spring Boot Actuator's
 * {@link org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration}.
 * No manual metrics registration is needed.
 * </p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties(AsyncProperties.class)
public class AsyncConfig implements AsyncConfigurer {

  private final MeterRegistry meterRegistry;
  private final AsyncProperties asyncProperties;
  private final Executor defaultExecutor;
  private AsyncExceptionHandler exceptionHandler;

  /**
   * Constructor with injected dependencies.
   * Default executor is injected lazily to avoid circular dependency issues.
   *
   * @param meterRegistry   Micrometer registry for metrics
   * @param asyncProperties Async configuration properties
   * @param defaultExecutor Default executor (injected lazily after creation)
   */
  public AsyncConfig(
      MeterRegistry meterRegistry,
      AsyncProperties asyncProperties,
      @Lazy @Qualifier("defaultExecutor") Executor defaultExecutor) {
    this.meterRegistry = meterRegistry;
    this.asyncProperties = asyncProperties;
    this.defaultExecutor = defaultExecutor;
  }

  /**
   * Task decorator bean for MDC context propagation.
   * SecurityContext is handled separately by
   * DelegatingSecurityContextAsyncTaskExecutor.
   *
   * @return MDC task decorator instance
   */
  @Bean
  public TaskDecorator taskDecorator() {
    return new MdcTaskDecorator();
  }

  /**
   * Async exception handler bean.
   *
   * @return async exception handler
   */
  @Bean
  public AsyncExceptionHandler asyncExceptionHandler() {
    this.exceptionHandler = new AsyncExceptionHandler(meterRegistry);
    return exceptionHandler;
  }

  /**
   * Notification executor for email notifications.
   * <p>
   * Configured with moderate pool size and bounded queue for I/O-bound email
   * sending.
   * Uses CallerRunsPolicy to throttle when queue is full.
   * Wrapped with DelegatingSecurityContextAsyncTaskExecutor for SecurityContext
   * propagation.
   * </p>
   *
   * @param builder       the task executor builder
   * @param taskDecorator the MDC task decorator for context propagation
   * @return configured notification executor with SecurityContext propagation
   */
  @Bean(name = "notificationExecutor")
  public AsyncTaskExecutor notificationExecutor(
      ThreadPoolTaskExecutorBuilder builder,
      TaskDecorator taskDecorator) {
    AsyncProperties.PoolProps props = asyncProperties.getNotification();
    ThreadPoolTaskExecutor executor = builder
        .corePoolSize(props.getCorePoolSize())
        .maxPoolSize(props.getMaxPoolSize())
        .queueCapacity(props.getQueueCapacity())
        .keepAlive(asyncProperties.getNotificationKeepAlive())
        .threadNamePrefix(props.getThreadNamePrefix())
        .taskDecorator(taskDecorator)
        .awaitTermination(true)
        .awaitTerminationPeriod(asyncProperties.getShutdownWaitTimeout())
        .build();

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();

    // Wrap with SecurityContext propagation
    AsyncTaskExecutor wrappedExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);

    log.info(
        "Configured notification executor: core={}, max={}, queue={}, keepAlive={}s",
        props.getCorePoolSize(),
        props.getMaxPoolSize(),
        props.getQueueCapacity(),
        props.getKeepAlive().getSeconds());

    return wrappedExecutor;
  }

  /**
   * RPC executor for Thrift server startup.
   * <p>
   * Configured with minimal pool size since server startup is rare.
   * Uses AbortPolicy as failures are critical.
   * Wrapped with DelegatingSecurityContextAsyncTaskExecutor for SecurityContext
   * propagation.
   * </p>
   *
   * @param builder       the task executor builder
   * @param taskDecorator the MDC task decorator for context propagation
   * @return configured RPC executor with SecurityContext propagation
   */
  @Bean(name = "rpcExecutor")
  public AsyncTaskExecutor rpcExecutor(
      ThreadPoolTaskExecutorBuilder builder,
      TaskDecorator taskDecorator) {
    AsyncProperties.PoolProps props = asyncProperties.getRpc();
    ThreadPoolTaskExecutor executor = builder
        .corePoolSize(props.getCorePoolSize())
        .maxPoolSize(props.getMaxPoolSize())
        .queueCapacity(props.getQueueCapacity())
        .keepAlive(asyncProperties.getRpcKeepAlive())
        .threadNamePrefix(props.getThreadNamePrefix())
        .taskDecorator(taskDecorator)
        .awaitTermination(true)
        .awaitTerminationPeriod(asyncProperties.getShutdownWaitTimeout())
        .build();

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();

    // Wrap with SecurityContext propagation
    AsyncTaskExecutor wrappedExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);

    log.info(
        "Configured RPC executor: core={}, max={}, queue={}, keepAlive={}s",
        props.getCorePoolSize(),
        props.getMaxPoolSize(),
        props.getQueueCapacity(),
        props.getKeepAlive().getSeconds());

    return wrappedExecutor;
  }

  /**
   * Default executor for general async operations.
   * <p>
   * Larger pool size and queue capacity for general-purpose async tasks.
   * Uses CallerRunsPolicy to throttle when queue is full.
   * Wrapped with DelegatingSecurityContextAsyncTaskExecutor for SecurityContext
   * propagation.
   * </p>
   * <p>
   * This executor is registered with both "defaultExecutor" and "taskExecutor"
   * bean names
   * to align with Spring Boot conventions. The "taskExecutor" alias ensures
   * compatibility
   * with Spring Boot's auto-configuration mechanisms.
   * </p>
   *
   * @param builder       the task executor builder
   * @param taskDecorator the MDC task decorator for context propagation
   * @return configured default executor with SecurityContext propagation
   */
  @Bean(name = { "defaultExecutor", "taskExecutor" })
  public AsyncTaskExecutor defaultExecutor(
      ThreadPoolTaskExecutorBuilder builder,
      TaskDecorator taskDecorator) {
    AsyncProperties.PoolProps props = asyncProperties.getDefault();
    ThreadPoolTaskExecutor executor = builder
        .corePoolSize(props.getCorePoolSize())
        .maxPoolSize(props.getMaxPoolSize())
        .queueCapacity(props.getQueueCapacity())
        .keepAlive(asyncProperties.getDefaultKeepAlive())
        .threadNamePrefix(props.getThreadNamePrefix())
        .taskDecorator(taskDecorator)
        .awaitTermination(true)
        .awaitTerminationPeriod(asyncProperties.getShutdownWaitTimeout())
        .build();

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    // Implement forceShutdown logic
    if (asyncProperties.isForceShutdown()) {
      executor.setAwaitTerminationSeconds((int) asyncProperties.getShutdownWaitTimeout().getSeconds());
    }

    executor.initialize();

    // Wrap with SecurityContext propagation
    AsyncTaskExecutor wrappedExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);

    log.info(
        "Configured default executor: core={}, max={}, queue={}, keepAlive={}s, forceShutdown={}",
        props.getCorePoolSize(),
        props.getMaxPoolSize(),
        props.getQueueCapacity(),
        props.getKeepAlive().getSeconds(),
        asyncProperties.isForceShutdown());

    return wrappedExecutor;
  }

  /**
   * Returns the default async executor.
   * <p>
   * This is used when @Async is called without specifying an executor name.
   * The executor is injected via constructor to avoid race conditions.
   * </p>
   *
   * @return default executor with SecurityContext propagation
   */
  @Override
  public Executor getAsyncExecutor() {
    return defaultExecutor;
  }

  /**
   * Returns the global async exception handler.
   *
   * @return async exception handler
   */
  @Override
  public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    // Return the bean instance created by asyncExceptionHandler() method
    // Spring will call this method after all beans are initialized
    return exceptionHandler != null ? exceptionHandler : asyncExceptionHandler();
  }
}
