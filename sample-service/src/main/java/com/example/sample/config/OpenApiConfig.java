package com.example.sample.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Value("${app.name:sample-service}")
  private String appName;

  @Value("${app.version:1.0.0}")
  private String appVersion;

  @Value("${spring.profiles.active:dev}")
  private String environment;

  @Bean
  public OpenAPI sampleServiceOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Sample Service API")
            .description("SDK diagnostics and sample endpoints")
            .version(appVersion)
            .contact(new Contact().name("Sample Team").email("team@example.com"))
            .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
        .servers(List.of(new Server().url("/").description(environment + " server")));
  }
}


