package com.example.control.infrastructure.observability;

import com.example.control.infrastructure.config.security.UserContext;
import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

/**
 * Utility class for enriching MDC (Mapped Diagnostic Context) with business
 * context.
 * <p>
 * MDC allows adding contextual information to logs that will be automatically
 * included
 * in all log statements within the same thread. This is useful for tracing
 * requests
 * across service boundaries and correlating logs with metrics and traces.
 * </p>
 * <p>
 * <b>Usage:</b>
 * <ul>
 * <li>Call enrichment methods at the start of business operations</li>
 * <li>Call {@link #clearBusinessContext()} at the end (after try-finally or
 * similar)</li>
 * <li>Note: traceId and spanId are managed by Micrometer Tracing and should NOT
 * be cleared</li>
 * </ul>
 * </p>
 * <p>
 * <b>MDC Keys used:</b>
 * <ul>
 * <li>{@code userId} - Current user ID</li>
 * <li>{@code serviceId} - Service ID being operated on</li>
 * <li>{@code teamId} - Team ID for team-based operations</li>
 * <li>{@code operation} - Business operation name</li>
 * <li>{@code traceId} - Distributed trace ID (managed by Micrometer
 * Tracing)</li>
 * <li>{@code spanId} - Current span ID (managed by Micrometer Tracing)</li>
 * </ul>
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@UtilityClass
public class MdcEnrichment {

  /**
   * MDC keys for business context.
   */
  public static final String KEY_USER_ID = "userId";
  public static final String KEY_SERVICE_ID = "serviceId";
  public static final String KEY_TEAM_ID = "teamId";
  public static final String KEY_OPERATION = "operation";

  /**
   * Enriches MDC with user context.
   * <p>
   * Adds userId from UserContext to MDC for log correlation.
   *
   * @param userContext the user context to extract information from
   */
  public static void enrichWithUser(UserContext userContext) {
    if (userContext != null && userContext.getUserId() != null) {
      MDC.put(KEY_USER_ID, userContext.getUserId());
    }
  }

  /**
   * Enriches MDC with service context.
   * <p>
   * Adds serviceId to MDC for log correlation.
   *
   * @param serviceId the service ID
   */
  public static void enrichWithService(String serviceId) {
    if (serviceId != null && !serviceId.trim().isEmpty()) {
      MDC.put(KEY_SERVICE_ID, serviceId);
    }
  }

  /**
   * Enriches MDC with team context.
   * <p>
   * Adds teamId to MDC for log correlation.
   *
   * @param teamId the team ID
   */
  public static void enrichWithTeam(String teamId) {
    if (teamId != null && !teamId.trim().isEmpty()) {
      MDC.put(KEY_TEAM_ID, teamId);
    }
  }

  /**
   * Enriches MDC with operation context.
   * <p>
   * Adds operation name to MDC for log correlation.
   *
   * @param operation the operation name
   */
  public static void enrichWithOperation(String operation) {
    if (operation != null && !operation.trim().isEmpty()) {
      MDC.put(KEY_OPERATION, operation);
    }
  }

  /**
   * Enriches MDC with full business context from UserContext.
   * <p>
   * Adds userId and optionally teamIds (if available) to MDC.
   *
   * @param userContext the user context
   */
  public static void enrichWithUserContext(UserContext userContext) {
    if (userContext != null) {
      enrichWithUser(userContext);
      // Optionally add first team ID if available
      if (userContext.getTeamIds() != null && !userContext.getTeamIds().isEmpty()) {
        enrichWithTeam(userContext.getTeamIds().get(0));
      }
    }
  }

  /**
   * Clears business context from MDC while preserving trace/span IDs.
   * <p>
   * Removes business-specific MDC keys but keeps traceId and spanId
   * which are managed by Micrometer Tracing.
   */
  public static void clearBusinessContext() {
    MDC.remove(KEY_USER_ID);
    MDC.remove(KEY_SERVICE_ID);
    MDC.remove(KEY_TEAM_ID);
    MDC.remove(KEY_OPERATION);
    // Note: Do NOT remove traceId and spanId - they are managed by Micrometer
    // Tracing
  }

  /**
   * Clears all MDC context including trace/span IDs.
   * <p>
   * Use with caution - this will remove trace correlation information.
   * Only use in scenarios where you need to reset all MDC state.
   */
  public static void clearAll() {
    MDC.clear();
  }
}
