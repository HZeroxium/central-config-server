package com.example.control.infrastructure.config.misc;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for async execution with separate thread pools per workload
 * type.
 * <p>
 * Provides four thread pool executors:
 * <ul>
 * <li><b>notificationExecutor</b>: For email notifications (I/O bound, moderate
 * throughput). Uses virtual threads by default for better scalability.</li>
 * <li><b>rpcExecutor</b>: For RPC server startup (rare, minimal concurrency).
 * Uses platform threads.</li>
 * <li><b>defaultExecutor</b> / <b>taskExecutor</b>: Fallback for other async
 * operations. Uses platform threads.</li>
 * <li><b>configHashFetchExecutor</b>: For parallel fetching of configuration
 * hashes from Config Server during heartbeat batch processing (I/O bound,
 * controlled concurrency). Uses platform threads.</li>
 * </ul>
 * </p>
 * <p>
 * Features:
 * <ul>
 * <li>MDC propagation via MdcTaskDecorator (applied first)</li>
 * <li>SecurityContext propagation via
 * DelegatingSecurityContextAsyncTaskExecutor
 * (applied second)</li>
 * <li>Global async exception handler with metrics</li>
 * <li>Hybrid virtual threads support for I/O-bound tasks (Java 21+)</li>
 * <li>Consistent graceful shutdown with configurable timeout and force shutdown
 * option</li>
 * </ul>
 * </p>
 * <p>
 * Metrics registration:
 * <ul>
 * <li>Platform thread executors ({@code rpcExecutor}, {@code defaultExecutor})
 * are
 * automatically instrumented by Spring Boot's
 * {@link org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration}</li>
 * <li>Virtual thread executors are automatically instrumented by Spring Boot
 * 3.2+
 * with {@code micrometer-java21} (via
 * {@link org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration})</li>
 * </ul>
 * Metrics appear as {@code executor.active}, {@code executor.completed},
 * {@code executor.queue.size}, etc., tagged by executor bean name.
 * </p>
 * <p>
 * TaskDecorator chaining order (verified correct):
 * <ol>
 * <li>MdcTaskDecorator captures MDC context (via builder)</li>
 * <li>DelegatingSecurityContextAsyncTaskExecutor propagates SecurityContext
 * (wraps
 * executor)</li>
 * </ol>
 * This ensures proper context propagation for observability and security.
 * </p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties(AsyncProperties.class)
public class AsyncConfig implements AsyncConfigurer {

  private final MeterRegistry meterRegistry;
  private final AsyncProperties asyncProperties;
  private TaskDecorator taskDecorator;
  private Executor defaultExecutor;
  private AsyncExceptionHandler exceptionHandler;

  /**
   * Constructor with injected dependencies.
   * <p>
   * Note: defaultExecutor is not injected to avoid circular dependency. It is set
   * via @PostConstruct after bean creation.
   * Note: taskDecorator is set via @PostConstruct to avoid circular dependency.
   *
   * @param meterRegistry   Micrometer registry for metrics
   * @param asyncProperties Async configuration properties
   */
  public AsyncConfig(
      MeterRegistry meterRegistry,
      AsyncProperties asyncProperties) {
    this.meterRegistry = meterRegistry;
    this.asyncProperties = asyncProperties;
  }

  /**
   * Initializes exception handler after all beans are created.
   * <p>
   * This ensures proper initialization order and avoids circular dependencies.
   * Task decorator is initialized in the bean method to ensure it's available
   * when other bean methods need it.
   */
  @PostConstruct
  public void initialize() {
    // Exception handler is initialized here to guarantee it's ready before
    // getAsyncUncaughtExceptionHandler() is called
    this.exceptionHandler = new AsyncExceptionHandler(meterRegistry);
    log.debug("Initialized async exception handler");
  }

  /**
   * Task decorator bean for MDC context propagation.
   * SecurityContext is handled separately by
   * DelegatingSecurityContextAsyncTaskExecutor.
   * <p>
   * Initialized here to ensure it's available when other bean methods need it.
   *
   * @return MDC task decorator instance
   */
  @Bean
  public TaskDecorator taskDecorator() {
    // Initialize here to ensure it's available when executor bean methods are
    // called
    if (taskDecorator == null) {
      this.taskDecorator = new MdcTaskDecorator();
    }
    return taskDecorator;
  }

  /**
   * Async exception handler bean.
   *
   * @return async exception handler
   */
  @Bean
  public AsyncExceptionHandler asyncExceptionHandler() {
    // This method is called by Spring, but we also initialize in @PostConstruct
    // to ensure it's ready early
    return exceptionHandler != null ? exceptionHandler : new AsyncExceptionHandler(meterRegistry);
  }

  /**
   * Notification executor for email notifications.
   * <p>
   * Uses virtual threads by default (Java 21+) for I/O-bound email sending
   * operations, providing better scalability with lower overhead.
   * Uses CallerRunsPolicy to throttle when queue is full (for platform threads).
   * Wrapped with DelegatingSecurityContextAsyncTaskExecutor for SecurityContext
   * propagation.
   * </p>
   *
   * @param builder       the task executor builder (for platform threads)
   * @param meterRegistry Micrometer registry for metrics registration
   * @return configured notification executor with SecurityContext propagation
   */
  @Bean(name = "notificationExecutor")
  public AsyncTaskExecutor notificationExecutor(ThreadPoolTaskExecutorBuilder builder,
      MeterRegistry meterRegistry) {
    AsyncProperties.PoolProps props = asyncProperties.getNotification();

    AsyncTaskExecutor executor;
    if (props.isUseVirtualThreads()) {
      // Use virtual threads for I/O-bound tasks
      // VirtualThreadTaskExecutor doesn't support setTaskDecorator directly,
      // so we wrap tasks manually via a custom executor
      VirtualThreadTaskExecutor virtualExecutor = new VirtualThreadTaskExecutor(
          props.getThreadNamePrefix());

      // Wrap with task decorator
      // Metrics are automatically registered by Spring Boot 3.2+ with
      // micrometer-java21
      executor = new AsyncTaskExecutor() {
        @Override
        public void execute(Runnable task) {
          Runnable decoratedTask = taskDecorator.decorate(task);
          virtualExecutor.execute(decoratedTask);
        }

        @Override
        public Future<?> submit(Runnable task) {
          CompletableFuture<Void> future = new CompletableFuture<>();
          Runnable decoratedTask = taskDecorator.decorate(() -> {
            try {
              task.run();
              future.complete(null);
            } catch (Exception e) {
              future.completeExceptionally(e);
            }
          });
          virtualExecutor.execute(decoratedTask);
          return future;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
          CompletableFuture<T> future = new CompletableFuture<>();
          Runnable decoratedTask = taskDecorator.decorate(() -> {
            try {
              T result = task.call();
              future.complete(result);
            } catch (Exception e) {
              future.completeExceptionally(e);
            }
          });
          virtualExecutor.execute(decoratedTask);
          return future;
        }
      };

      log.info(
          "Configured notification executor with virtual threads: threadNamePrefix={}, metrics auto-instrumented",
          props.getThreadNamePrefix());
    } else {
      // Use platform threads with pool configuration
      ThreadPoolTaskExecutor platformExecutor = builder
          .corePoolSize(props.getCorePoolSize())
          .maxPoolSize(props.getMaxPoolSize())
          .queueCapacity(props.getQueueCapacity())
          .keepAlive(asyncProperties.getNotificationKeepAlive())
          .threadNamePrefix(props.getThreadNamePrefix())
          .taskDecorator(taskDecorator)
          .awaitTermination(true)
          .awaitTerminationPeriod(asyncProperties.getShutdownWaitTimeout())
          .build();

      platformExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

      // Apply consistent shutdown logic
      if (asyncProperties.isForceShutdown()) {
        platformExecutor.setWaitForTasksToCompleteOnShutdown(false);
        platformExecutor.setAwaitTerminationSeconds(
            (int) asyncProperties.getShutdownWaitTimeout().getSeconds());
      }

      platformExecutor.initialize();
      executor = platformExecutor;

      log.info(
          "Configured notification executor with platform threads: core={}, max={}, queue={}, keepAlive={}s, forceShutdown={}",
          props.getCorePoolSize(),
          props.getMaxPoolSize(),
          props.getQueueCapacity(),
          props.getKeepAlive().getSeconds(),
          asyncProperties.isForceShutdown());
    }

    // Wrap with SecurityContext propagation (applied after MDC decorator)
    return new DelegatingSecurityContextAsyncTaskExecutor(executor);
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
   * @param builder the task executor builder
   * @return configured RPC executor with SecurityContext propagation
   */
  @Bean(name = "rpcExecutor")
  public AsyncTaskExecutor rpcExecutor(ThreadPoolTaskExecutorBuilder builder) {
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

    // Apply consistent shutdown logic
    if (asyncProperties.isForceShutdown()) {
      executor.setWaitForTasksToCompleteOnShutdown(false);
      executor.setAwaitTerminationSeconds(
          (int) asyncProperties.getShutdownWaitTimeout().getSeconds());
    }

    executor.initialize();

    // Wrap with SecurityContext propagation
    AsyncTaskExecutor wrappedExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);

    log.info(
        "Configured RPC executor: core={}, max={}, queue={}, keepAlive={}s, forceShutdown={}",
        props.getCorePoolSize(),
        props.getMaxPoolSize(),
        props.getQueueCapacity(),
        props.getKeepAlive().getSeconds(),
        asyncProperties.isForceShutdown());

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
   * bean
   * names
   * to align with Spring Boot conventions. The "taskExecutor" alias ensures
   * compatibility
   * with Spring Boot's auto-configuration mechanisms.
   * </p>
   *
   * @param builder the task executor builder
   * @return configured default executor with SecurityContext propagation
   */
  @Bean(name = { "defaultExecutor", "taskExecutor" })
  public AsyncTaskExecutor defaultExecutor(ThreadPoolTaskExecutorBuilder builder) {
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

    // Apply consistent shutdown logic (using builder API exclusively)
    if (asyncProperties.isForceShutdown()) {
      executor.setWaitForTasksToCompleteOnShutdown(false);
      executor.setAwaitTerminationSeconds(
          (int) asyncProperties.getShutdownWaitTimeout().getSeconds());
    }

    executor.initialize();

    // Wrap with SecurityContext propagation
    AsyncTaskExecutor wrappedExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);

    // Store reference for getAsyncExecutor() method
    this.defaultExecutor = wrappedExecutor;

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
   * Config hash fetch executor for parallel configuration hash fetching.
   * <p>
   * Dedicated thread pool for I/O-bound operations that fetch configuration
   * hashes from Config Server during heartbeat batch processing. Uses platform
   * threads with controlled concurrency to prevent overwhelming the Config Server.
   * Uses CallerRunsPolicy to throttle when queue is full.
   * Wrapped with DelegatingSecurityContextAsyncTaskExecutor for SecurityContext
   * propagation.
   * </p>
   *
   * @param builder the task executor builder
   * @return configured config hash fetch executor with SecurityContext propagation
   */
  @Bean(name = "configHashFetchExecutor")
  public AsyncTaskExecutor configHashFetchExecutor(ThreadPoolTaskExecutorBuilder builder) {
    AsyncProperties.PoolProps props = asyncProperties.getConfigHashFetch();

    ThreadPoolTaskExecutor executor = builder
        .corePoolSize(props.getCorePoolSize())
        .maxPoolSize(props.getMaxPoolSize())
        .queueCapacity(props.getQueueCapacity())
        .keepAlive(asyncProperties.getConfigHashFetchKeepAlive())
        .threadNamePrefix(props.getThreadNamePrefix())
        .taskDecorator(taskDecorator)
        .awaitTermination(true)
        .awaitTerminationPeriod(asyncProperties.getShutdownWaitTimeout())
        .build();

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    // Apply consistent shutdown logic
    if (asyncProperties.isForceShutdown()) {
      executor.setWaitForTasksToCompleteOnShutdown(false);
      executor.setAwaitTerminationSeconds(
          (int) asyncProperties.getShutdownWaitTimeout().getSeconds());
    }

    executor.initialize();

    // Wrap with SecurityContext propagation
    AsyncTaskExecutor wrappedExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);

    log.info(
        "Configured config hash fetch executor: core={}, max={}, queue={}, keepAlive={}s, forceShutdown={}",
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
   * The executor reference is set during bean creation via defaultExecutor()
   * method.
   * </p>
   *
   * @return default executor with SecurityContext propagation
   */
  @Override
  public Executor getAsyncExecutor() {
    // This should never be null as defaultExecutor bean is created before this
    // method is called
    if (defaultExecutor == null) {
      throw new IllegalStateException(
          "Default executor not initialized. This should not happen if Spring Boot is properly configured.");
    }
    return defaultExecutor;
  }

  /**
   * Returns the global async exception handler.
   * <p>
   * Guaranteed to return non-null as exceptionHandler is initialized in
   * @PostConstruct.
   *
   * @return async exception handler (never null)
   */
  @Override
  public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    // Guaranteed non-null after @PostConstruct initialization
    if (exceptionHandler == null) {
      throw new IllegalStateException(
          "Exception handler not initialized. This should not happen if Spring Boot is properly configured.");
    }
    return exceptionHandler;
  }
}
