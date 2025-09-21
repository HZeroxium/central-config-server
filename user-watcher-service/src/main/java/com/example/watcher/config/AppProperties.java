package com.example.watcher.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank
    private String name = "user-watcher-service";

    @NotBlank
    private String version = "1.0.0";

    @NotBlank
    private String environment = "development";

    @Pattern(regexp = "mongo|jpa")
    private String persistenceType = "mongo";
}
