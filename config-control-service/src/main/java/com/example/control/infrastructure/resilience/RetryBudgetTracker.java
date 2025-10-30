package com.example.control.infrastructure.resilience;

import com.example.control.infrastructure.config.resilience.ResilienceProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracks retry budget per service dependency using sliding window.
 * <p>
 * Prevents cascading failures by limiting the percentage of requests
 * that can be retried within a time window. If retry rate exceeds the
 * budget (e.g., 20% of requests), further retries are rejected.
 * </p>
 * <p>
 * Thread-safe implementation with per-service locks for concurrent access.
 * </p>
 */
@Slf4j
@Component
public class RetryBudgetTracker {

    private final ResilienceProperties resilienceProperties;
    private final MeterRegistry meterRegistry;

    // Per-service request and retry tracking with sliding window
    private final Map<String, ServiceMetrics> serviceMetrics = new ConcurrentHashMap<>();

    // Metrics counters
    private final Map<String, Counter> allowedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> rejectedCounters = new ConcurrentHashMap<>();

    public RetryBudgetTracker(ResilienceProperties resilienceProperties, MeterRegistry meterRegistry) {
        this.resilienceProperties = resilienceProperties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Check if retry is allowed for the given service within budget.
     *
     * @param serviceName Service name (e.g., "configserver", "consul")
     * @return true if retry is allowed, false if budget exceeded
     */
    public boolean canRetry(String serviceName) {
        if (!resilienceProperties.getRetryBudget().isEnabled()) {
            return true;
        }

        ServiceMetrics metrics = serviceMetrics.computeIfAbsent(serviceName, ServiceMetrics::new);

        metrics.lock.lock();
        try {
            cleanupOldEntries(metrics);

            long totalRequests = metrics.requestTimestamps.size();
            long totalRetries = metrics.retryTimestamps.size();

            if (totalRequests == 0) {
                // No requests yet, allow retry but record it only after confirming
                incrementAllowedCounter(serviceName);
                recordRetry(serviceName, metrics);
                return true;
            }

            int maxRetryPercentage = getMaxRetryPercentage(serviceName);
            double currentRetryPercentage = (double) totalRetries / totalRequests * 100.0;

            if (currentRetryPercentage < maxRetryPercentage) {
                // Budget allows retry - record it only after confirmation
                incrementAllowedCounter(serviceName);
                recordRetry(serviceName, metrics);
                log.debug("Retry allowed for {}: {}/{} ({:.1f}% < {}%)",
                        serviceName, totalRetries, totalRequests, currentRetryPercentage, maxRetryPercentage);
                return true;
            } else {
                incrementRejectedCounter(serviceName);
                log.warn("Retry budget exceeded for {}: {}/{} ({:.1f}% >= {}%)",
                        serviceName, totalRetries, totalRequests, currentRetryPercentage, maxRetryPercentage);
                return false;
            }
        } finally {
            metrics.lock.unlock();
        }
    }

    /**
     * Record a request (not a retry) for budget calculation.
     *
     * @param serviceName Service name
     */
    public void recordRequest(String serviceName) {
        if (!resilienceProperties.getRetryBudget().isEnabled()) {
            return;
        }

        ServiceMetrics metrics = serviceMetrics.computeIfAbsent(serviceName, ServiceMetrics::new);

        metrics.lock.lock();
        try {
            metrics.requestTimestamps.offer(Instant.now());
            cleanupOldEntries(metrics);
        } finally {
            metrics.lock.unlock();
        }
    }

    private void recordRetry(String serviceName, ServiceMetrics metrics) {
        metrics.retryTimestamps.offer(Instant.now());
    }

    private void cleanupOldEntries(ServiceMetrics metrics) {
        long windowMillis = resilienceProperties.getRetryBudget().getWindowSize().toMillis();
        Instant cutoff = Instant.now().minusMillis(windowMillis);

        // Remove old request timestamps
        while (!metrics.requestTimestamps.isEmpty() &&
                metrics.requestTimestamps.peek().isBefore(cutoff)) {
            metrics.requestTimestamps.poll();
        }

        // Remove old retry timestamps
        while (!metrics.retryTimestamps.isEmpty() &&
                metrics.retryTimestamps.peek().isBefore(cutoff)) {
            metrics.retryTimestamps.poll();
        }
    }

    private int getMaxRetryPercentage(String serviceName) {
        ResilienceProperties.RetryBudget.ServiceBudget serviceBudget = resilienceProperties.getRetryBudget()
                .getPerService().get(serviceName);

        if (serviceBudget != null && serviceBudget.getMaxRetryPercentage() > 0) {
            return serviceBudget.getMaxRetryPercentage();
        }

        return resilienceProperties.getRetryBudget().getMaxRetryPercentage();
    }

    private void incrementAllowedCounter(String serviceName) {
        allowedCounters.computeIfAbsent(serviceName, name -> Counter.builder("retry.budget.allowed")
                .tag("service", name)
                .description("Number of retries allowed within budget")
                .register(meterRegistry)).increment();
    }

    private void incrementRejectedCounter(String serviceName) {
        rejectedCounters.computeIfAbsent(serviceName, name -> Counter.builder("retry.budget.rejected")
                .tag("service", name)
                .description("Number of retries rejected due to budget exceeded")
                .register(meterRegistry)).increment();
    }

    /**
     * Get current retry budget utilization percentage for a service.
     *
     * @param serviceName Service name
     * @return Retry percentage (0-100), or 0 if no data
     */
    public double getRetryUtilization(String serviceName) {
        ServiceMetrics metrics = serviceMetrics.get(serviceName);
        if (metrics == null) {
            return 0.0;
        }

        metrics.lock.lock();
        try {
            cleanupOldEntries(metrics);
            long totalRequests = metrics.requestTimestamps.size();
            long totalRetries = metrics.retryTimestamps.size();

            if (totalRequests == 0) {
                return 0.0;
            }

            return (double) totalRetries / totalRequests * 100.0;
        } finally {
            metrics.lock.unlock();
        }
    }

    /**
     * Per-service metrics holder with thread-safe sliding window queues.
     */
    private static class ServiceMetrics {
        private final String serviceName;
        private final Queue<Instant> requestTimestamps = new LinkedList<>();
        private final Queue<Instant> retryTimestamps = new LinkedList<>();
        private final ReentrantLock lock = new ReentrantLock();

        ServiceMetrics(String serviceName) {
            this.serviceName = serviceName;
        }
    }
}
