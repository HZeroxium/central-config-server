package com.example.control.infrastructure.configfile;

import com.example.control.domain.model.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main service for generating config files for ApplicationServices.
 * <p>
 * Orchestrates template rendering and file writing to create Spring Cloud
 * Config Server YAML files for services.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "config-file-generator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConfigFileGeneratorService {

    private final ConfigFileGeneratorProperties properties;
    private final ConfigFileTemplateEngine templateEngine;
    private final ConfigFileWriter fileWriter;

    /**
     * Generates config files for a single ApplicationService.
     *
     * @param service application service
     * @return generation result
     */
    public GenerationResult generateForService(ApplicationService service) {
        GenerationResult result = new GenerationResult();
        result.serviceId = service.getId().id();

        try {
            // Ensure base path exists
            fileWriter.ensureBasePathExists();

            // Generate base application.yml
            generateBaseConfig(service, result);

            // Generate environment-specific configs
            List<String> environments = service.getEnvironments();
            if (environments != null && !environments.isEmpty()) {
                for (String env : environments) {
                    generateEnvironmentConfig(service, env, result);
                }
            }

            // Generate optional files
            if (properties.isGenerateFeatureFlags()) {
                generateFeatureFlags(service, result);
            }

            if (properties.isGenerateBanner()) {
                generateBanner(service, result);
            }

            log.info("Generated config files for service: {} ({} files)", result.serviceId, result.filesGenerated);
            return result;

        } catch (Exception e) {
            log.error("Failed to generate config files for service: {}", result.serviceId, e);
            result.error = e.getMessage();
            return result;
        }
    }

    /**
     * Generates config files for multiple services.
     *
     * @param services list of application services
     * @return aggregate generation result
     */
    public AggregateGenerationResult generateForServices(List<ApplicationService> services) {
        AggregateGenerationResult aggregate = new AggregateGenerationResult();
        aggregate.totalServices = services.size();

        for (ApplicationService service : services) {
            GenerationResult result = generateForService(service);
            aggregate.results.add(result);

            if (result.isSuccess()) {
                aggregate.successCount++;
                aggregate.totalFilesGenerated += result.filesGenerated;
            } else {
                aggregate.failureCount++;
            }
        }

        log.info("Config file generation completed: {} services, {} files, {} success, {} failures",
                aggregate.totalServices, aggregate.totalFilesGenerated,
                aggregate.successCount, aggregate.failureCount);

        return aggregate;
    }

    /**
     * Generates config files for HeartbeatLoadTester pattern services.
     * <p>
     * Creates services matching the pattern used in HeartbeatLoadTester:
     * - payment-service-1, payment-service-2, ...
     * - order-service-1, order-service-2, ...
     * </p>
     *
     * @param numServices        number of services to generate
     * @param instancesPerService instances per service (for variation)
     * @return aggregate generation result
     */
    public AggregateGenerationResult generateForLoadTest(int numServices, int instancesPerService) {
        log.info("Generating config files for {} load test services", numServices);

        String[] serviceDomains = {"payment", "order", "inventory", "billing", "auth", "search", "reporting"};
        String[] serviceSuffixes = {"service", "api", "processor"};

        List<ApplicationService> services = new ArrayList<>();
        for (int s = 0; s < numServices; s++) {
            String domain = serviceDomains[s % serviceDomains.length];
            String suffix = serviceSuffixes[(s / serviceDomains.length) % serviceSuffixes.length];
            int variant = (s / serviceDomains.length) + 1;
            String serviceId = String.format("%s-%s-%d", domain, suffix, variant);
            String displayName = String.format("%s %s %d", capitalize(domain), capitalize(suffix), variant);

            // Create mock ApplicationService for load test
            ApplicationService service = ApplicationService.builder()
                    .id(com.example.control.domain.valueobject.id.ApplicationServiceId.of(serviceId))
                    .displayName(displayName)
                    .environments(List.of("dev", "staging", "prod"))
                    .tags(List.of("load-test", "microservice"))
                    .lifecycle(ApplicationService.ServiceLifecycle.ACTIVE)
                    .attributes(createLoadTestAttributes(serviceId))
                    .build();

            services.add(service);
        }

        return generateForServices(services);
    }

    /**
     * Generates base application.yml file.
     */
    private void generateBaseConfig(ApplicationService service, GenerationResult result) throws IOException {
        String content = templateEngine.render(
                properties.getTemplates().getApplicationYml(),
                service,
                null
        );

        boolean written = fileWriter.writeFile(
                service.getId().id(),
                "application.yml",
                content,
                properties.isSkipExisting()
        );

        if (written) {
            result.filesGenerated++;
        } else {
            result.filesSkipped++;
        }
    }

    /**
     * Generates environment-specific application-{env}.yml file.
     */
    private void generateEnvironmentConfig(ApplicationService service, String environment, GenerationResult result) throws IOException {
        String content = templateEngine.render(
                properties.getTemplates().getApplicationEnvYml(),
                service,
                environment
        );

        String fileName = "application-" + environment + ".yml";
        boolean written = fileWriter.writeFile(
                service.getId().id(),
                fileName,
                content,
                properties.isSkipExisting()
        );

        if (written) {
            result.filesGenerated++;
        } else {
            result.filesSkipped++;
        }
    }

    /**
     * Generates feature-flags.yml file.
     */
    private void generateFeatureFlags(ApplicationService service, GenerationResult result) throws IOException {
        String content = templateEngine.render(
                properties.getTemplates().getFeatureFlagsYml(),
                service,
                null
        );

        boolean written = fileWriter.writeFile(
                service.getId().id(),
                "feature-flags.yml",
                content,
                properties.isSkipExisting()
        );

        if (written) {
            result.filesGenerated++;
        } else {
            result.filesSkipped++;
        }
    }

    /**
     * Generates banner.txt file.
     */
    private void generateBanner(ApplicationService service, GenerationResult result) throws IOException {
        String content = templateEngine.render(
                properties.getTemplates().getBannerTxt(),
                service,
                null
        );

        boolean written = fileWriter.writeFile(
                service.getId().id(),
                "banner.txt",
                content,
                properties.isSkipExisting()
        );

        if (written) {
            result.filesGenerated++;
        } else {
            result.filesSkipped++;
        }
    }

    /**
     * Creates attributes for load test services.
     */
    private java.util.Map<String, String> createLoadTestAttributes(String serviceId) {
        java.util.Map<String, String> attrs = new java.util.HashMap<>();
        attrs.put("framework", "spring-boot");
        attrs.put("language", "java-21");
        attrs.put("build-tool", "gradle");
        attrs.put("version", "1.0.0");
        return attrs;
    }

    /**
     * Capitalizes first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Result of generating config files for a single service.
     */
    public static class GenerationResult {
        public String serviceId;
        public int filesGenerated = 0;
        public int filesSkipped = 0;
        public String error;

        public boolean isSuccess() {
            return error == null;
        }
    }

    /**
     * Aggregate result of generating config files for multiple services.
     */
    public static class AggregateGenerationResult {
        public int totalServices = 0;
        public int successCount = 0;
        public int failureCount = 0;
        public int totalFilesGenerated = 0;
        public List<GenerationResult> results = new ArrayList<>();
    }
}

