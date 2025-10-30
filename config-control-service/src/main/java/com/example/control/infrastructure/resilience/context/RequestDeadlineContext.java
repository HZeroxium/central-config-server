package com.example.control.infrastructure.resilience.context;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * ThreadLocal context for request deadline propagation.
 * <p>
 * Enables coordinated timeouts across service call chains by tracking
 * the deadline for the entire request. Downstream services can check
 * remaining time and fail-fast if deadline is near or expired.
 * </p>
 * <p>
 * Integrates with MDC for distributed tracing.
 * </p>
 */
@Slf4j
public final class RequestDeadlineContext {

  private static final String MDC_DEADLINE_KEY = "deadline";
  private static final ThreadLocal<Instant> DEADLINE_HOLDER = new ThreadLocal<>();

  private RequestDeadlineContext() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Set the deadline for the current request.
   *
   * @param deadline The absolute deadline instant
   */
  public static void setDeadline(Instant deadline) {
    if (deadline == null) {
      log.warn("Attempted to set null deadline, ignoring");
      return;
    }
    DEADLINE_HOLDER.set(deadline);
    MDC.put(MDC_DEADLINE_KEY, deadline.toString());
    log.trace("Set request deadline: {}", deadline);
  }

  /**
   * Set the deadline relative to now by timeout duration.
   *
   * @param timeout Timeout duration from now
   */
  public static void setDeadlineFromTimeout(Duration timeout) {
    if (timeout == null || timeout.isNegative() || timeout.isZero()) {
      log.warn("Invalid timeout duration: {}, ignoring", timeout);
      return;
    }
    Instant deadline = Instant.now().plus(timeout);
    setDeadline(deadline);
  }

  /**
   * Get the current request deadline.
   *
   * @return Optional containing the deadline if set
   */
  public static Optional<Instant> getDeadline() {
    return Optional.ofNullable(DEADLINE_HOLDER.get());
  }

  /**
   * Get remaining time until deadline.
   *
   * @return Optional containing remaining duration, empty if no deadline set
   */
  public static Optional<Duration> getRemainingTime() {
    return getDeadline().map(deadline -> {
      Instant now = Instant.now();
      if (now.isAfter(deadline)) {
        return Duration.ZERO;
      }
      return Duration.between(now, deadline);
    });
  }

  /**
   * Check if the deadline has expired.
   *
   * @return true if deadline is set and has passed, false otherwise
   */
  public static boolean isExpired() {
    return getDeadline()
        .map(deadline -> Instant.now().isAfter(deadline))
        .orElse(false);
  }

  /**
   * Check if there is enough time remaining for an operation.
   *
   * @param required Minimum required duration
   * @return true if enough time remains or no deadline set, false otherwise
   */
  public static boolean hasTimeRemaining(Duration required) {
    if (required == null || required.isNegative() || required.isZero()) {
      return true;
    }

    return getRemainingTime()
        .map(remaining -> remaining.compareTo(required) >= 0)
        .orElse(true); // No deadline means no constraint
  }

  /**
   * Throw exception if deadline is expired.
   *
   * @throws DeadlineExceededException if deadline has passed
   */
  public static void checkDeadline() throws DeadlineExceededException {
    if (isExpired()) {
      Instant deadline = getDeadline().orElse(null);
      throw new DeadlineExceededException("Request deadline exceeded: " + deadline);
    }
  }

  /**
   * Clear the deadline for the current thread.
   * <p>
   * Should be called in finally blocks to prevent thread pool pollution.
   * </p>
   */
  public static void clear() {
    DEADLINE_HOLDER.remove();
    MDC.remove(MDC_DEADLINE_KEY);
    log.trace("Cleared request deadline");
  }

  /**
   * Exception thrown when request deadline is exceeded.
   */
  public static class DeadlineExceededException extends RuntimeException {
    public DeadlineExceededException(String message) {
      super(message);
    }

    public DeadlineExceededException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
