// package com.example.configserver.health;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.apache.kafka.clients.admin.AdminClient;
// import org.apache.kafka.clients.admin.AdminClientConfig;
// import org.apache.kafka.clients.admin.DescribeClusterOptions;
// import org.springframework.boot.actuate.health.Health;
// import org.springframework.boot.actuate.health.HealthIndicator;
// import org.springframework.kafka.core.KafkaAdmin;
// import org.springframework.stereotype.Component;

// import java.util.Map;
// import java.util.Properties;
// import java.util.concurrent.TimeUnit;

// /**
//  * Health indicator for Kafka connectivity (Spring Cloud Bus).
//  * <p>
//  * Checks if Kafka brokers are reachable and if the springCloudBus topic is accessible.
//  * </p>
//  *
//  * @author Config Server Team
//  * @since 1.0.0
//  */
// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class KafkaHealthIndicator implements HealthIndicator {

//     private final KafkaAdmin kafkaAdmin;

//     @Override
//     public Health health() {
//         try {
//             // Get broker addresses from KafkaAdmin
//             Map<String, Object> configs = kafkaAdmin.getConfigurationProperties();
//             String bootstrapServers = (String) configs.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);

//             // Create AdminClient to check connectivity
//             Properties props = new Properties();
//             props.putAll(configs);
//             props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

//             try (AdminClient adminClient = AdminClient.create(props)) {
//                 // Describe cluster to verify connectivity
//                 DescribeClusterOptions options = new DescribeClusterOptions()
//                         .timeoutMs(5000);

//                 var clusterDescription = adminClient.describeCluster(options)
//                         .clusterId()
//                         .get(5, TimeUnit.SECONDS);

//                 return Health.up()
//                         .withDetail("brokers", bootstrapServers != null ? bootstrapServers : "unknown")
//                         .withDetail("clusterId", clusterDescription != null ? clusterDescription : "unknown")
//                         .withDetail("status", "reachable")
//                         .build();

//             } catch (Exception e) {
//                 log.debug("Kafka health check encountered exception: {}", e.getMessage());
//                 return Health.down()
//                         .withDetail("brokers", bootstrapServers != null ? bootstrapServers : "unknown")
//                         .withDetail("status", "unreachable")
//                         .withDetail("error", e.getMessage())
//                         .build();
//             }

//         } catch (Exception e) {
//             log.warn("Error checking Kafka health", e);
//             return Health.down()
//                     .withDetail("status", "error")
//                     .withDetail("error", e.getMessage())
//                     .build();
//         }
//     }
// }

