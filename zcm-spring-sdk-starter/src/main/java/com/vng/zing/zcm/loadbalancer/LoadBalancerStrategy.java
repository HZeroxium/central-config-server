package com.vng.zing.zcm.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import java.util.List;

/**
 * Defines the contract for service instance selection strategies
 * used by the client-side load balancer.
 * <p>
 * Implementations determine which {@link ServiceInstance} should
 * be chosen from a list of available instances, using algorithms such as:
 * <ul>
 *   <li>Round Robin</li>
 *   <li>Random</li>
 *   <li>Weighted Random</li>
 * </ul>
 *
 * <p>Each implementation should be stateless or thread-safe,
 * as multiple concurrent requests may share the same strategy.
 */
public interface LoadBalancerStrategy {

  /**
   * Selects one {@link ServiceInstance} from the provided list.
   *
   * @param serviceName the logical name of the target service
   * @param instances   the list of available service instances
   * @return the chosen instance, or {@code null} if the list is empty
   */
  ServiceInstance choose(String serviceName, List<ServiceInstance> instances);

  /**
   * Returns the name of this strategy for configuration and logging purposes.
   *
   * @return the strategy name (e.g., "ROUND_ROBIN", "RANDOM", "WEIGHTED_RANDOM")
   */
  String getName();

  /**
   * Enumeration of supported load-balancing strategies.
   * Provides a type-safe way to refer to policies and parse user-defined strings.
   */
  enum Policy {
    ROUND_ROBIN("ROUND_ROBIN"),
    RANDOM("RANDOM"),
    WEIGHTED_RANDOM("WEIGHTED_RANDOM");

    private final String value;

    Policy(String value) {
      this.value = value;
    }

    /**
     * Returns the string value of this policy.
     *
     * @return the string representation of the policy
     */
    public String getValue() {
      return value;
    }

    /**
     * Parses a string value into a {@link Policy}, with flexible aliases.
     * <p>Accepted examples:
     * <ul>
     *   <li>"ROUND_ROBIN" or "RR"</li>
     *   <li>"RANDOM"</li>
     *   <li>"WEIGHTED_RANDOM" or "WEIGHTED"</li>
     * </ul>
     *
     * @param value the policy name string
     * @return the corresponding policy, defaulting to {@link #ROUND_ROBIN}
     */
    public static Policy fromString(String value) {
      if (value == null || value.isEmpty()) {
        return ROUND_ROBIN;
      }

      for (Policy policy : values()) {
        if (policy.value.equalsIgnoreCase(value)
            || (policy == ROUND_ROBIN && "RR".equalsIgnoreCase(value))
            || (policy == WEIGHTED_RANDOM && "WEIGHTED".equalsIgnoreCase(value))) {
          return policy;
        }
      }

      return ROUND_ROBIN; // fallback default
    }
  }
}
