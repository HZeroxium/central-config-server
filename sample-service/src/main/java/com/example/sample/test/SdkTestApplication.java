package com.example.sample.test;

import com.vng.zing.zcm.client.ClientApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

/**
 * Standalone test application for ZCM SDK.
 * 
 * Usage:
 *   java -jar sample-service-test.jar
 *   or
 *   ./gradlew bootRun --args='--spring.profiles.active=test'
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SdkTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdkTestApplication.class, args);
    }

    @Bean
    public CommandLineRunner testSdk(ClientApi client) {
        return args -> {
            System.out.println("=== ZCM SDK Test ===");
            
            // Test 1: Config API
            System.out.println("\n1. Config Hash:");
            String hash = client.config().hash();
            System.out.println("   Hash: " + hash);
            
            // Test 2: Config Snapshot
            System.out.println("\n2. Config Snapshot:");
            var snapshot = client.config().snapshot();
            System.out.println("   Application: " + snapshot.get("application"));
            System.out.println("   Profile: " + snapshot.get("profile"));
            
            // Test 3: Load Balancer
            System.out.println("\n3. Load Balancer:");
            System.out.println("   Strategy: " + client.loadBalancer().strategy());
            
            // Test 4: Service Discovery
            System.out.println("\n4. Service Discovery:");
            var instances = client.loadBalancer().instances("config-control-service");
            System.out.println("   Found " + instances.size() + " instances");
            instances.forEach(inst -> 
                System.out.println("   - " + inst.getHost() + ":" + inst.getPort())
            );
            
            // Test 5: Choose Instance
            System.out.println("\n5. Choose Instance:");
            var chosen = client.loadBalancer().choose("config-control-service");
            if (chosen != null) {
                System.out.println("   Chosen: " + chosen.getHost() + ":" + chosen.getPort());
            }
            
            // Test 6: Ping
            System.out.println("\n6. Ping Control Service:");
            try {
                client.pingNow();
                System.out.println("   Ping sent successfully");
            } catch (Exception e) {
                System.out.println("   Ping failed: " + e.getMessage());
            }
            
            // Test 7: KV API (if enabled)
            try {
                System.out.println("\n7. KV API:");
                String value = client.kv().getString("sample-service", "config/test.key");
                System.out.println("   Value: " + value);
            } catch (Exception e) {
                System.out.println("   KV not available: " + e.getMessage());
            }
            
            System.out.println("\n=== Test Complete ===");
        };
    }
}