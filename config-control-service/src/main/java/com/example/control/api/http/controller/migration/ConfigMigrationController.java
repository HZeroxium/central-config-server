package com.example.control.api.http.controller.migration;

import com.example.control.api.http.dto.migration.ConfigMigrationDtos;
import com.example.control.infrastructure.configmigration.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for configuration migration and SDK integration tools.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Converting INI/Properties files to YAML</li>
 *   <li>Generating @ConfigurationProperties classes from YAML</li>
 *   <li>Generating ZCM SDK configuration</li>
 *   <li>Analyzing configuration and providing suggestions</li>
 *   <li>Accessing template library</li>
 * </ul>
 * </p>
 * <p>
 * All endpoints are public (no authentication required) to facilitate
 * developer integration workflows.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Tag(name = "Config Migration", description = "Tools for migrating and integrating SDK configuration")
public class ConfigMigrationController {

    private final IniToYamlConverter iniToYamlConverter;
    private final PropertiesToYamlConverter propertiesToYamlConverter;
    private final ConfigPropertiesGenerator configPropertiesGenerator;
    private final SdkConfigGenerator sdkConfigGenerator;
    private final YamlAnalyzer yamlAnalyzer;
    private final ConfigValidator configValidator;
    private final ConfigLinter configLinter;
    private final TemplateLibraryService templateLibraryService;

    /**
     * Converts INI file to YAML format.
     *
     * @param request the conversion request
     * @return converted YAML content
     */
    @PostMapping("/ini-to-yaml")
    @Operation(
            summary = "Convert INI file to YAML",
            description = "Converts INI format configuration file to YAML format with proper structure"
    )
    @ApiResponse(responseCode = "200", description = "Conversion successful")
    @ApiResponse(responseCode = "400", description = "Invalid INI format")
    public ResponseEntity<ConfigMigrationDtos.IniToYamlResponse> convertIniToYaml(
            @Valid @RequestBody ConfigMigrationDtos.IniToYamlRequest request
    ) {
        log.debug("Converting INI to YAML");

        try {
            String yamlContent = iniToYamlConverter.convert(request.getIniContent());
            Map<String, Object> yamlMap = iniToYamlConverter.convertToSpringBootStructure(request.getIniContent());

            ConfigMigrationDtos.IniToYamlResponse response = ConfigMigrationDtos.IniToYamlResponse.builder()
                    .yamlContent(yamlContent)
                    .yamlMap(yamlMap)
                    .suggestions(List.of(
                            "Review the generated YAML structure",
                            "Consider using @ConfigurationProperties for type safety"
                    ))
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid INI format: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to convert INI to YAML", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Converts Properties file to YAML format.
     *
     * @param request the conversion request
     * @return converted YAML content
     */
    @PostMapping("/properties-to-yaml")
    @Operation(
            summary = "Convert Properties file to YAML",
            description = "Converts Properties format configuration file to YAML format with hierarchical structure"
    )
    @ApiResponse(responseCode = "200", description = "Conversion successful")
    @ApiResponse(responseCode = "400", description = "Invalid Properties format")
    public ResponseEntity<ConfigMigrationDtos.PropertiesToYamlResponse> convertPropertiesToYaml(
            @Valid @RequestBody ConfigMigrationDtos.PropertiesToYamlRequest request
    ) {
        log.debug("Converting Properties to YAML");

        try {
            String yamlContent = propertiesToYamlConverter.convert(request.getPropertiesContent());
            Map<String, Object> yamlMap = propertiesToYamlConverter.convertToSpringBootStructure(request.getPropertiesContent());

            ConfigMigrationDtos.PropertiesToYamlResponse response = ConfigMigrationDtos.PropertiesToYamlResponse.builder()
                    .yamlContent(yamlContent)
                    .yamlMap(yamlMap)
                    .suggestions(List.of(
                            "Review the generated YAML structure",
                            "Consider using @ConfigurationProperties for type safety",
                            "Properties are now organized hierarchically"
                    ))
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid Properties format: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to convert Properties to YAML", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generates @ConfigurationProperties classes from YAML.
     *
     * @param request the generation request
     * @return generated Java classes
     */
    @PostMapping("/generate-properties-class")
    @Operation(
            summary = "Generate @ConfigurationProperties class",
            description = "Analyzes YAML configuration and generates @ConfigurationProperties classes with validation annotations"
    )
    @ApiResponse(responseCode = "200", description = "Generation successful")
    @ApiResponse(responseCode = "400", description = "Invalid YAML format")
    public ResponseEntity<ConfigMigrationDtos.ConfigPropertiesGenerationResponse> generatePropertiesClass(
            @Valid @RequestBody ConfigMigrationDtos.GeneratePropertiesRequest request
    ) {
        log.debug("Generating @ConfigurationProperties classes");

        try {
            String packageName = request.getPackageName();
            if (packageName == null || packageName.isBlank()) {
                packageName = "com.example.config";
            }

            ConfigPropertiesGenerator.ConfigPropertiesGenerationResult result =
                    configPropertiesGenerator.generate(request.getYamlContent(), packageName);

            List<ConfigMigrationDtos.GeneratedClass> classes = result.getClasses().stream()
                    .map(c -> ConfigMigrationDtos.GeneratedClass.builder()
                            .className(c.getClassName())
                            .prefix(c.getPrefix())
                            .code(c.getCode())
                            .imports(c.getImports())
                            .build())
                    .collect(Collectors.toList());

            ConfigMigrationDtos.ConfigPropertiesGenerationResponse response =
                    ConfigMigrationDtos.ConfigPropertiesGenerationResponse.builder()
                            .classes(classes)
                            .imports(result.getAllImports())
                            .metadataJson(result.getMetadataJson())
                            .packageName(result.getPackageName())
                            .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid YAML format: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to generate ConfigurationProperties classes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generates ZCM SDK configuration from existing application.yml.
     *
     * @param request the generation request
     * @return generated SDK configuration
     */
    @PostMapping("/generate-sdk-config")
    @Operation(
            summary = "Generate SDK configuration",
            description = "Analyzes existing application.yml and generates ZCM SDK configuration snippet"
    )
    @ApiResponse(responseCode = "200", description = "Generation successful")
    @ApiResponse(responseCode = "400", description = "Invalid YAML format")
    public ResponseEntity<ConfigMigrationDtos.SdkConfigGenerationResponse> generateSdkConfig(
            @Valid @RequestBody ConfigMigrationDtos.GenerateSdkConfigRequest request
    ) {
        log.debug("Generating SDK configuration");

        try {
            SdkConfigGenerator.SdkConfigGenerationResult result =
                    sdkConfigGenerator.generate(request.getApplicationYml(), request.getServiceName());

            ConfigMigrationDtos.SdkConfigGenerationResponse response =
                    ConfigMigrationDtos.SdkConfigGenerationResponse.builder()
                            .serviceName(result.getServiceName())
                            .generatedConfig(result.getGeneratedConfig())
                            .generatedConfigYaml(result.getGeneratedConfigYaml())
                            .integrationSteps(result.getIntegrationSteps())
                            .estimatedTime(result.getEstimatedTime())
                            .suggestions(result.getSuggestions())
                            .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid YAML format: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to generate SDK configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Analyzes configuration and provides migration suggestions.
     *
     * @param request the analysis request
     * @return analysis result with suggestions
     */
    @PostMapping("/analyze-config")
    @Operation(
            summary = "Analyze configuration",
            description = "Analyzes configuration file and provides validation, linting, and migration suggestions"
    )
    @ApiResponse(responseCode = "200", description = "Analysis successful")
    @ApiResponse(responseCode = "400", description = "Invalid configuration format")
    public ResponseEntity<ConfigMigrationDtos.ConfigAnalysisResponse> analyzeConfig(
            @Valid @RequestBody ConfigMigrationDtos.AnalyzeConfigRequest request
    ) {
        log.debug("Analyzing configuration");

        try {
            String format = request.getFormat();
            if (format == null || format.isBlank()) {
                format = "yaml";
            }

            String yamlContent;
            if ("ini".equalsIgnoreCase(format)) {
                yamlContent = iniToYamlConverter.convert(request.getConfigContent());
            } else if ("properties".equalsIgnoreCase(format)) {
                yamlContent = propertiesToYamlConverter.convert(request.getConfigContent());
            } else {
                yamlContent = request.getConfigContent();
            }

            // Validate
            ConfigValidator.ValidationResult validation = configValidator.validate(yamlContent);

            // Lint
            ConfigLinter.LintingResult linting = configLinter.lint(yamlContent);
            List<ConfigMigrationDtos.LintingIssue> lintingIssues = linting.getIssues().stream()
                    .map(i -> ConfigMigrationDtos.LintingIssue.builder()
                            .severity(i.getSeverity())
                            .message(i.getMessage())
                            .suggestion(i.getSuggestion())
                            .property(i.getProperty())
                            .build())
                    .collect(Collectors.toList());

            // Analyze property groups
            List<YamlAnalyzer.PropertyGroup> groups = yamlAnalyzer.analyzePropertyGroups(yamlContent);
            List<ConfigMigrationDtos.PropertyGroupSuggestion> groupSuggestions = groups.stream()
                    .map(g -> ConfigMigrationDtos.PropertyGroupSuggestion.builder()
                            .prefix(g.getPrefix())
                            .className(capitalize(g.getPrefix()) + "Properties")
                            .propertyCount(g.getFields().size())
                            .properties(g.getFields().stream()
                                    .map(YamlAnalyzer.PropertyField::getName)
                                    .collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());

            // Generate migration checklist
            List<String> checklist = generateMigrationChecklist(yamlContent);

            ConfigMigrationDtos.ConfigAnalysisResponse response =
                    ConfigMigrationDtos.ConfigAnalysisResponse.builder()
                            .validation(ConfigMigrationDtos.ValidationResult.builder()
                                    .valid(validation.isValid())
                                    .errors(validation.getErrors())
                                    .warnings(validation.getWarnings())
                                    .suggestions(validation.getSuggestions())
                                    .build())
                            .lintingIssues(lintingIssues)
                            .propertyGroupSuggestions(groupSuggestions)
                            .migrationChecklist(checklist)
                            .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to analyze configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lists all available templates.
     *
     * @return list of templates
     */
    @GetMapping("/templates")
    @Operation(
            summary = "List available templates",
            description = "Returns list of all available configuration templates"
    )
    @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    public ResponseEntity<ConfigMigrationDtos.TemplateListResponse> listTemplates() {
        log.debug("Listing templates");

        List<TemplateLibraryService.TemplateInfo> templates = templateLibraryService.listTemplates();
        List<ConfigMigrationDtos.TemplateInfo> templateInfos = templates.stream()
                .map(t -> ConfigMigrationDtos.TemplateInfo.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .description(t.getDescription())
                        .category(t.getCategory())
                        .content(t.getContent())
                        .build())
                .collect(Collectors.toList());

        ConfigMigrationDtos.TemplateListResponse response =
                ConfigMigrationDtos.TemplateListResponse.builder()
                        .templates(templateInfos)
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Gets template details by ID.
     *
     * @param templateId the template ID
     * @return template details
     */
    @GetMapping("/templates/{templateId}")
    @Operation(
            summary = "Get template details",
            description = "Returns detailed information about a specific template"
    )
    @ApiResponse(responseCode = "200", description = "Template retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Template not found")
    public ResponseEntity<ConfigMigrationDtos.TemplateDetailResponse> getTemplate(
            @PathVariable String templateId
    ) {
        log.debug("Getting template: {}", templateId);

        TemplateLibraryService.TemplateInfo template = templateLibraryService.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        ConfigMigrationDtos.TemplateInfo templateInfo = ConfigMigrationDtos.TemplateInfo.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .content(template.getContent())
                .build();

        ConfigMigrationDtos.TemplateDetailResponse response =
                ConfigMigrationDtos.TemplateDetailResponse.builder()
                        .template(templateInfo)
                        .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Generates migration checklist.
     *
     * @param yamlContent the YAML content
     * @return checklist items
     */
    private List<String> generateMigrationChecklist(String yamlContent) {
        List<String> checklist = new ArrayList<>();
        checklist.add("Review generated @ConfigurationProperties classes");
        checklist.add("Add @ConfigurationPropertiesScan to main class");
        checklist.add("Copy generated SDK config to application.yml");
        checklist.add("Add SDK dependency to build.gradle");
        checklist.add("Test configuration loading");
        checklist.add("Verify service registration in Consul");
        checklist.add("Monitor drift detection in admin dashboard");
        return checklist;
    }

    /**
     * Capitalizes first letter.
     *
     * @param str the string
     * @return capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}

