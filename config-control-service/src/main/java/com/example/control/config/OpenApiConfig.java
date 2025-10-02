package com.example.control.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger UI configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

  @Value("${server.port:8889}")
  private int serverPort;

  @Value("${app.name:config-control-service}")
  private String appName;

  @Value("${app.version:1.0.0}")
  private String appVersion;

  @Value("${app.environment:development}")
  private String environment;

  /**
   * Configure OpenAPI specification with service metadata.
   * 
   * @return configured OpenAPI instance
   */
  @Bean
  public OpenAPI configControlServiceOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Config Control Service API")
            .description("""
                Centralized configuration management and drift detection service.

                **Features:**
                - Service instance heartbeat tracking
                - Configuration drift detection and reporting
                - Service discovery integration with Consul
                - Config refresh orchestration via Kafka
                - Real-time monitoring and alerts

                **Architecture:**
                - Hexagonal architecture with distinct API, Application, Domain, and Infrastructure layers
                - MongoDB for persistence
                - Redis for caching
                - Kafka for event broadcasting
                """)
            .version(appVersion)
            .contact(new Contact()
                .name("Platform Team")
                .email("platform@example.com"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .servers(List.of(
            new Server()
                .url("http://localhost:" + serverPort)
                .description(environment + " server")));
  }
}
