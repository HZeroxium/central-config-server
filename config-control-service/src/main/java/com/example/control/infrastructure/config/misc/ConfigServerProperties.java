package com.example.control.infrastructure.config.misc;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "config.server")
public class ConfigServerProperties {
    /**
     * Base URL of Spring Cloud Config Server, e.g. http://config-server:8888
     */
    @NotBlank
    private String url = "http://config-server:8888";
}
