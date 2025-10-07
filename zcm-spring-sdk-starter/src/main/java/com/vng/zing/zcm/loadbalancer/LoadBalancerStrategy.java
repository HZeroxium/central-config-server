package com.vng.zing.zcm.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import java.util.List;

public interface LoadBalancerStrategy {
  
  /**
   * Choose a service instance from available instances.
   * @param serviceName the service name
   * @param instances list of available instances
   * @return chosen instance or null if list is empty
   */
  ServiceInstance choose(String serviceName, List<ServiceInstance> instances);
  
  /**
   * Strategy name for configuration and logging.
   */
  String getName();
  
  /**
   * Load balancer policy enum for type-safe configuration.
   */
  enum Policy {
    ROUND_ROBIN("ROUND_ROBIN"),
    RANDOM("RANDOM"),
    WEIGHTED_RANDOM("WEIGHTED_RANDOM");
    
    private final String value;
    
    Policy(String value) {
      this.value = value;
    }
    
    public String getValue() {
      return value;
    }
    
    public static Policy fromString(String value) {
      if (value == null || value.isEmpty()) {
        return ROUND_ROBIN;
      }
      
      for (Policy policy : values()) {
        if (policy.value.equalsIgnoreCase(value) || 
            (policy == ROUND_ROBIN && "RR".equalsIgnoreCase(value)) ||
            (policy == WEIGHTED_RANDOM && "WEIGHTED".equalsIgnoreCase(value))) {
          return policy;
        }
      }
      
      return ROUND_ROBIN; // default fallback
    }
  }
}
