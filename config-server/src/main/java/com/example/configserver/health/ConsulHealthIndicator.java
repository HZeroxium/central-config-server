// package com.example.configserver.health;

// import com.ecwid.consul.v1.ConsulClient;
// import com.ecwid.consul.v1.Response;
// import com.ecwid.consul.v1.agent.model.Self;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.actuate.health.Health;
// import org.springframework.boot.actuate.health.HealthIndicator;
// import org.springframework.cloud.consul.ConsulProperties;
// import org.springframework.stereotype.Component;

// /**
//  * Health indicator for Consul connectivity.
//  * <p>
//  * Checks if the Consul agent is reachable and if service registration is working.
//  * </p>
//  *
//  * @author Config Server Team
//  * @since 1.0.0
//  */
// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class ConsulHealthIndicator implements HealthIndicator {

//     private final ConsulClient consulClient;
//     private final ConsulProperties consulProperties;

//     @Override
//     public Health health() {
//         try {
//             // Check Consul agent connectivity by getting agent self information
//             Response<Self> agentSelf = consulClient.getAgentSelf();

//             if (agentSelf != null && agentSelf.getValue() != null) {
//                 Self self = agentSelf.getValue();
//                 String member = self.getMember() != null ? self.getMember().getName() : "unknown";

//                 return Health.up()
//                         .withDetail("host", consulProperties.getHost())
//                         .withDetail("port", consulProperties.getPort())
//                         .withDetail("member", member)
//                         .withDetail("status", "reachable")
//                         .build();
//             } else {
//                 return Health.down()
//                         .withDetail("host", consulProperties.getHost())
//                         .withDetail("port", consulProperties.getPort())
//                         .withDetail("status", "unreachable")
//                         .withDetail("error", "Agent self response was null")
//                         .build();
//             }

//         } catch (Exception e) {
//             log.debug("Consul health check encountered exception: {}", e.getMessage());
//             return Health.down()
//                     .withDetail("host", consulProperties.getHost())
//                     .withDetail("port", consulProperties.getPort())
//                     .withDetail("status", "unreachable")
//                     .withDetail("error", e.getMessage())
//                     .build();
//         }
//     }
// }

