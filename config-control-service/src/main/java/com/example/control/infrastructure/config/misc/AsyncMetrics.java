package com.example.control.infrastructure.config.misc;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Component for registering Micrometer metrics for async thread pools.
 * <p>
 * Registers metrics for:
 * <ul>
 *   <li>Thread pool state: active threads, pool size, queue size</li>
 *   <li>Task execution timing: execution duration percentiles</li>
 *   <li>Task lifecycle: submitted, completed, rejected counts</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class AsyncMetrics {

  private final MeterRegistry meterRegistry;
  private final Map<String, ThreadPoolTaskExecutor> executors = new ConcurrentHashMap<>();

  public AsyncMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * Registers an executor for metrics collection.
   *
   * @param executorName the name of the executor
   * @param executor     the thread pool executor
   */
  public void registerExecutor(String executorName, ThreadPoolTaskExecutor executor) {
    executors.put(executorName, executor);
    registerMetricsForExecutor(executorName, executor);
    log.info("Registered metrics for async executor: {}", executorName);
  }

  @PostConstruct
  public void registerMetrics() {
    log.info("AsyncMetrics component initialized. Executors will be registered as they are created.");
  }

  /**
   * Registers all metrics for a specific executor.
   *
   * @param executorName the executor name
   * @param executor     the thread pool executor
   */
  private void registerMetricsForExecutor(String executorName, ThreadPoolTaskExecutor executor) {
    ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
    if (threadPoolExecutor == null) {
      log.warn("ThreadPoolExecutor is null for executor: {}", executorName);
      return;
    }

    // Register gauges for pool state
    Gauge.builder("executor.active", threadPoolExecutor, ThreadPoolExecutor::getActiveCount)
        .description("Number of active threads in the executor")
        .tag("executor", executorName)
        .register(meterRegistry);

    Gauge.builder("executor.pool.size", threadPoolExecutor, ThreadPoolExecutor::getPoolSize)
        .description("Current number of threads in the pool")
        .tag("executor", executorName)
        .register(meterRegistry);

    Gauge.builder("executor.queue.size", threadPoolExecutor, e -> e.getQueue().size())
        .description("Current number of tasks in the queue")
        .tag("executor", executorName)
        .register(meterRegistry);

    Gauge.builder("executor.queue.remaining", threadPoolExecutor,
            e -> e.getQueue().remainingCapacity())
        .description("Remaining capacity in the queue")
        .tag("executor", executorName)
        .register(meterRegistry);

    Gauge.builder("executor.queue.capacity", threadPoolExecutor,
            e -> e.getQueue().size() + e.getQueue().remainingCapacity())
        .description("Total queue capacity")
        .tag("executor", executorName)
        .register(meterRegistry);

    Gauge.builder("executor.completed", threadPoolExecutor,
            ThreadPoolExecutor::getCompletedTaskCount)
        .description("Total number of completed tasks")
        .tag("executor", executorName)
        .register(meterRegistry);

    log.debug("Registered metrics for executor: {}", executorName);
  }

  /**
   * Creates a Timer for tracking task execution time.
   * This should be used via AspectJ or manual instrumentation.
   *
   * @param executorName the executor name
   * @return a Timer instance
   */
  public Timer getExecutionTimer(String executorName) {
    return Timer.builder("executor.tasks.execution")
        .description("Time taken to execute async tasks")
        .tag("executor", executorName)
        .register(meterRegistry);
  }
}

