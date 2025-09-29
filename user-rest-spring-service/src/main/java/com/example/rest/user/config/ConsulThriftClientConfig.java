package com.example.rest.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.example.user.thrift.UserService;

import java.util.List;
import java.util.Random;

/**
 * Configuration for Consul-aware Thrift client with load balancing
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ThriftClientProperties.class)
@RequiredArgsConstructor
public class ConsulThriftClientConfig {

    private final ThriftClientProperties thriftClientProperties;
    private final DiscoveryClient discoveryClient;
    private final LoadBalancerClient loadBalancerClient;
    private final Random random = new Random();

    @Bean
    @Primary
    public ThriftClientFactory thriftClientFactory() {
        return new ConsulThriftClientFactory();
    }

    /**
     * Interface for creating Thrift clients
     */
    public interface ThriftClientFactory {
        UserService.Client createClient() throws Exception;

        void closeClient(UserService.Client client);
    }

    /**
     * Consul-aware implementation that discovers and load balances across service
     * instances
     */
    public class ConsulThriftClientFactory implements ThriftClientFactory {

        private static final String SERVICE_NAME = "user-thrift-server-service";

        @Override
        public UserService.Client createClient() throws Exception {
            ServiceInstance instance = selectServiceInstance();
            if (instance == null) {
                log.warn("No healthy instances found for service: {}. Falling back to configured host:port",
                        SERVICE_NAME);
                return createFallbackClient();
            }

            String host = instance.getHost();
            // Get the Thrift port from metadata, fallback to configured port
            int thriftPort = getThriftPort(instance);

            log.debug("Creating Thrift client connection to discovered instance {}:{} (thrift port)", host, thriftPort);

            TTransport transport = new TSocket(host, thriftPort, thriftClientProperties.getTimeout());
            transport.open();
            TBinaryProtocol protocol = new TBinaryProtocol(transport);

            log.debug("Thrift client connection established successfully to {}:{}", host, thriftPort);
            return new UserService.Client(protocol);
        }

        private int getThriftPort(ServiceInstance instance) {
            // Try to get thrift port from service metadata
            if (instance.getMetadata() != null && instance.getMetadata().containsKey("thrift-port")) {
                try {
                    return Integer.parseInt(instance.getMetadata().get("thrift-port"));
                } catch (NumberFormatException e) {
                    log.warn("Invalid thrift-port in metadata: {}", instance.getMetadata().get("thrift-port"));
                }
            }
            // Fallback to default thrift port
            return thriftClientProperties.getPort();
        }

        @Override
        public void closeClient(UserService.Client client) {
            if (client != null && client.getInputProtocol() != null) {
                try {
                    TTransport transport = client.getInputProtocol().getTransport();
                    if (transport != null && transport.isOpen()) {
                        transport.close();
                        log.debug("Thrift client connection closed successfully");
                    }
                } catch (Exception e) {
                    log.warn("Failed to close Thrift client connection", e);
                }
            }
        }

        private ServiceInstance selectServiceInstance() {
            try {
                // Use Spring Cloud LoadBalancer
                ServiceInstance instance = loadBalancerClient.choose(SERVICE_NAME);
                if (instance != null) {
                    log.debug("Selected service instance: {}:{} via load balancer",
                            instance.getHost(), instance.getPort());
                    return instance;
                }

                // Fallback to manual selection from discovery client
                List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_NAME);
                if (instances.isEmpty()) {
                    log.warn("No instances found for service: {}", SERVICE_NAME);
                    return null;
                }

                // Simple random load balancing as fallback
                ServiceInstance selected = instances.get(random.nextInt(instances.size()));
                log.debug("Selected service instance: {}:{} via manual load balancing",
                        selected.getHost(), selected.getPort());
                return selected;

            } catch (Exception e) {
                log.error("Failed to discover service instances for: {}", SERVICE_NAME, e);
                return null;
            }
        }

        private UserService.Client createFallbackClient() throws Exception {
            log.debug("Creating fallback Thrift client connection to {}:{}",
                    thriftClientProperties.getHost(), thriftClientProperties.getPort());

            TTransport transport = new TSocket(
                    thriftClientProperties.getHost(),
                    thriftClientProperties.getPort(),
                    thriftClientProperties.getTimeout());
            transport.open();
            TBinaryProtocol protocol = new TBinaryProtocol(transport);

            log.debug("Fallback Thrift client connection established successfully");
            return new UserService.Client(protocol);
        }
    }
}
