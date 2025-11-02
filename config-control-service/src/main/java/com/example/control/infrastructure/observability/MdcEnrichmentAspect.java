package com.example.control.infrastructure.observability;

import com.example.control.infrastructure.config.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect for automatically enriching MDC with business context for @Observed
 * methods.
 * <p>
 * This aspect intercepts methods annotated with
 * {@link io.micrometer.observation.annotation.Observed}
 * and automatically extracts business context from method parameters to enrich
 * MDC.
 * This ensures all log statements within instrumented methods include business
 * context.
 * </p>
 * <p>
 * <b>Extracted context:</b>
 * <ul>
 * <li>{@code UserContext} parameter → userId, teamIds</li>
 * <li>{@code String serviceId} parameter → serviceId</li>
 * <li>{@code String teamId} parameter → teamId</li>
 * <li>{@code String operation} from @Observed lowCardinalityKeyValues →
 * operation</li>
 * </ul>
 * </p>
 * <p>
 * <b>Order:</b> This aspect runs with order 1000 to execute before other
 * aspects
 * but after Spring's transaction management.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@Order(1000) // Run before other aspects but after transaction management
public class MdcEnrichmentAspect {

  /**
   * Intercepts @Observed methods and enriches MDC with business context.
   * <p>
   * Extracts context from method parameters and @Observed annotation,
   * then enriches MDC before method execution and clears business context after.
   *
   * @param joinPoint the join point
   * @param observed  the @Observed annotation
   * @return the method result
   * @throws Throwable if the method throws an exception
   */
  @Around("@annotation(observed)")
  public Object enrichMdcForObserved(ProceedingJoinPoint joinPoint,
      io.micrometer.observation.annotation.Observed observed) throws Throwable {
    try {
      // Extract business context from method parameters
      Object[] args = joinPoint.getArgs();
      if (args != null) {
        extractAndEnrichContext(args, observed);
      }

      // Proceed with method execution
      return joinPoint.proceed();
    } finally {
      // Clear business context after method execution
      // Note: traceId/spanId are preserved (managed by Micrometer Tracing)
      MdcEnrichment.clearBusinessContext();
    }
  }

  /**
   * Extracts business context from method parameters and enriches MDC.
   *
   * @param args     method arguments
   * @param observed @Observed annotation
   */
  private void extractAndEnrichContext(Object[] args, io.micrometer.observation.annotation.Observed observed) {
    // Extract UserContext
    Arrays.stream(args)
        .filter(arg -> arg instanceof UserContext)
        .map(arg -> (UserContext) arg)
        .findFirst()
        .ifPresent(MdcEnrichment::enrichWithUserContext);

    // Extract serviceId (String parameter named serviceId or containing "service")
    Arrays.stream(args)
        .filter(arg -> arg instanceof String)
        .map(arg -> (String) arg)
        .filter(this::looksLikeServiceId)
        .findFirst()
        .ifPresent(MdcEnrichment::enrichWithService);

    // Extract teamId (String parameter named teamId or newTeamId or targetTeamId)
    Arrays.stream(args)
        .filter(arg -> arg instanceof String)
        .map(arg -> (String) arg)
        .filter(this::looksLikeTeamId)
        .findFirst()
        .ifPresent(MdcEnrichment::enrichWithTeam);

    // Extract operation from @Observed lowCardinalityKeyValues
    String[] keyValues = observed.lowCardinalityKeyValues();
    if (keyValues != null && keyValues.length >= 2) {
      // Search for "operation" key
      for (int i = 0; i < keyValues.length - 1; i += 2) {
        if ("operation".equals(keyValues[i]) && i + 1 < keyValues.length) {
          MdcEnrichment.enrichWithOperation(keyValues[i + 1]);
          break;
        }
      }
    }
  }

  /**
   * Heuristic to identify serviceId parameters.
   * <p>
   * Checks if the string looks like a service ID (UUID format or alphanumeric).
   *
   * @param value the string value to check
   * @return true if it looks like a service ID
   */
  private boolean looksLikeServiceId(String value) {
    if (value == null || value.length() < 3) {
      return false;
    }
    // UUID format or alphanumeric with hyphens/underscores
    return value.matches("^[a-zA-Z0-9_-]+$") && (value.length() > 10 || value.contains("-"));
  }

  /**
   * Heuristic to identify teamId parameters.
   * <p>
   * Checks if the string looks like a team ID (alphanumeric).
   *
   * @param value the string value to check
   * @return true if it looks like a team ID
   */
  private boolean looksLikeTeamId(String value) {
    if (value == null || value.length() < 3) {
      return false;
    }
    // Alphanumeric with hyphens/underscores, typically shorter than service IDs
    return value.matches("^[a-zA-Z0-9_-]+$");
  }
}
