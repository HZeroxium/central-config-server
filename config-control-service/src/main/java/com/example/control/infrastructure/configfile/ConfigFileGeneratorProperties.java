package com.example.control.infrastructure.configfile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for config file generation.
 * <p>
 * Maps properties from {@code config-file-generator.*} in application.yml.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "config-file-generator")
public class ConfigFileGeneratorProperties {

    /**
     * Whether config file generation is enabled.
     */
    private boolean enabled = true;

    /**
     * Base path to the config repository directory.
     * Default: ./ztf-spring-cloud-config-server
     */
    @NotBlank(message = "Base path is required")
    private String basePath = "./ztf-spring-cloud-config-server";

    /**
     * Whether to skip existing files (don't overwrite).
     */
    private boolean skipExisting = true;

    /**
     * Whether to generate feature-flags.yml files.
     */
    private boolean generateFeatureFlags = true;

    /**
     * Whether to generate banner.txt files.
     */
    private boolean generateBanner = false;

    /**
     * Seed for deterministic value generation.
     */
    @Min(value = 0, message = "Deterministic seed must be non-negative")
    private long deterministicSeed = 42L;

    /**
     * Template configuration.
     */
    @NotNull(message = "Template configuration is required")
    private TemplateConfig templates = new TemplateConfig();

    /**
     * Template paths configuration.
     */
    @Data
    public static class TemplateConfig {
        /**
         * Path to application.yml template.
         */
        @NotBlank(message = "Application YML template path is required")
        private String applicationYml = "classpath:templates/config-files/application.yml.template";

        /**
         * Path to application-env.yml template.
         */
        @NotBlank(message = "Application env YML template path is required")
        private String applicationEnvYml = "classpath:templates/config-files/application-env.yml.template";

        /**
         * Path to feature-flags.yml template.
         */
        @NotBlank(message = "Feature flags YML template path is required")
        private String featureFlagsYml = "classpath:templates/config-files/feature-flags.yml.template";

        /**
         * Path to banner.txt template.
         */
        @NotBlank(message = "Banner TXT template path is required")
        private String bannerTxt = "classpath:templates/config-files/banner.txt.template";
    }
}

