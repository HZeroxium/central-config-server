package com.example.control.api.http.dto.migration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs for configuration migration API operations.
 * <p>
 * Provides request and response DTOs for format conversion,
 * code generation, and SDK integration.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
public final class ConfigMigrationDtos {

    private ConfigMigrationDtos() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ======================================================================
    // INI to YAML Conversion
    // ======================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "IniToYamlRequest", description = "Request to convert INI file to YAML format")
    public static class IniToYamlRequest {
        @NotBlank(message = "INI content cannot be blank")
        @Schema(description = "INI file content", example = "[database]\nhost=localhost\nport=5432", requiredMode = Schema.RequiredMode.REQUIRED)
        private String iniContent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "IniToYamlResponse", description = "Response containing converted YAML content")
    public static class IniToYamlResponse {
        @Schema(description = "Converted YAML content", example = "database:\n  host: localhost\n  port: 5432")
        private String yamlContent;

        @Schema(description = "YAML as structured map")
        private Map<String, Object> yamlMap;

        @Schema(description = "Conversion suggestions")
        private List<String> suggestions;
    }

    // ======================================================================
    // Properties to YAML Conversion
    // ======================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PropertiesToYamlRequest", description = "Request to convert Properties file to YAML format")
    public static class PropertiesToYamlRequest {
        @NotBlank(message = "Properties content cannot be blank")
        @Schema(description = "Properties file content", example = "spring.application.name=my-service\nserver.port=8080", requiredMode = Schema.RequiredMode.REQUIRED)
        private String propertiesContent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PropertiesToYamlResponse", description = "Response containing converted YAML content")
    public static class PropertiesToYamlResponse {
        @Schema(description = "Converted YAML content")
        private String yamlContent;

        @Schema(description = "YAML as structured map")
        private Map<String, Object> yamlMap;

        @Schema(description = "Conversion suggestions")
        private List<String> suggestions;
    }

    // ======================================================================
    // ConfigurationProperties Generation
    // ======================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "GeneratePropertiesRequest", description = "Request to generate @ConfigurationProperties classes from YAML")
    public static class GeneratePropertiesRequest {
        @NotBlank(message = "YAML content cannot be blank")
        @Schema(description = "YAML configuration content", requiredMode = Schema.RequiredMode.REQUIRED)
        private String yamlContent;

        @Schema(description = "Target package name for generated classes", example = "com.example.config", defaultValue = "com.example.config")
        private String packageName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConfigPropertiesGenerationResponse", description = "Response containing generated @ConfigurationProperties classes")
    public static class ConfigPropertiesGenerationResponse {
        @Schema(description = "List of generated Java classes")
        private List<GeneratedClass> classes;

        @Schema(description = "All required import statements")
        private List<String> imports;

        @Schema(description = "Spring Boot configuration metadata JSON")
        private String metadataJson;

        @Schema(description = "Package name used for generation")
        private String packageName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "GeneratedClass", description = "A generated @ConfigurationProperties class")
    public static class GeneratedClass {
        @Schema(description = "Class name", example = "DatabaseProperties")
        private String className;

        @Schema(description = "Configuration prefix", example = "database")
        private String prefix;

        @Schema(description = "Generated Java source code")
        private String code;

        @Schema(description = "Required imports for this class")
        private List<String> imports;
    }

    // ======================================================================
    // SDK Config Generation
    // ======================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "GenerateSdkConfigRequest", description = "Request to generate ZCM SDK configuration")
    public static class GenerateSdkConfigRequest {
        @NotBlank(message = "Application YAML content cannot be blank")
        @Schema(description = "Existing application.yml content", requiredMode = Schema.RequiredMode.REQUIRED)
        private String applicationYml;

        @Schema(description = "Service name (optional, will be extracted from config if not provided)", example = "my-service")
        private String serviceName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SdkConfigGenerationResponse", description = "Response containing generated SDK configuration")
    public static class SdkConfigGenerationResponse {
        @Schema(description = "Service name", example = "my-service")
        private String serviceName;

        @Schema(description = "Generated SDK configuration as map")
        private Map<String, Object> generatedConfig;

        @Schema(description = "Generated SDK configuration as YAML string")
        private String generatedConfigYaml;

        @Schema(description = "Step-by-step integration instructions")
        private List<String> integrationSteps;

        @Schema(description = "Estimated integration time", example = "15 minutes")
        private String estimatedTime;

        @Schema(description = "Suggestions for optional features")
        private List<String> suggestions;
    }

    // ======================================================================
    // Config Analysis
    // ======================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "AnalyzeConfigRequest", description = "Request to analyze configuration and provide suggestions")
    public static class AnalyzeConfigRequest {
        @NotBlank(message = "Config content cannot be blank")
        @Schema(description = "Configuration content (YAML, Properties, or INI)", requiredMode = Schema.RequiredMode.REQUIRED)
        private String configContent;

        @Schema(description = "Configuration format", example = "yaml", allowableValues = {"yaml", "properties", "ini"}, defaultValue = "yaml")
        private String format;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ConfigAnalysisResponse", description = "Response containing configuration analysis and suggestions")
    public static class ConfigAnalysisResponse {
        @Schema(description = "Validation result")
        private ValidationResult validation;

        @Schema(description = "Linting issues")
        private List<LintingIssue> lintingIssues;

        @Schema(description = "Suggested property groups for @ConfigurationProperties")
        private List<PropertyGroupSuggestion> propertyGroupSuggestions;

        @Schema(description = "Migration checklist")
        private List<String> migrationChecklist;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ValidationResult", description = "Configuration validation result")
    public static class ValidationResult {
        @Schema(description = "Whether configuration is valid")
        private boolean valid;

        @Schema(description = "List of validation errors")
        private List<String> errors;

        @Schema(description = "List of validation warnings")
        private List<String> warnings;

        @Schema(description = "List of suggestions")
        private List<String> suggestions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LintingIssue", description = "A linting issue found in configuration")
    public static class LintingIssue {
        @Schema(description = "Issue severity", example = "INFO", allowableValues = {"INFO", "WARN", "ERROR"})
        private String severity;

        @Schema(description = "Issue message")
        private String message;

        @Schema(description = "Suggestion to fix the issue")
        private String suggestion;

        @Schema(description = "Property path related to the issue")
        private String property;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PropertyGroupSuggestion", description = "Suggestion for a property group")
    public static class PropertyGroupSuggestion {
        @Schema(description = "Property prefix", example = "database")
        private String prefix;

        @Schema(description = "Suggested class name", example = "DatabaseProperties")
        private String className;

        @Schema(description = "Number of properties in this group")
        private int propertyCount;

        @Schema(description = "List of property names")
        private List<String> properties;
    }

    // ======================================================================
    // Template Library
    // ======================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "TemplateListResponse", description = "Response containing list of available templates")
    public static class TemplateListResponse {
        @Schema(description = "List of available templates")
        private List<TemplateInfo> templates;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "TemplateDetailResponse", description = "Response containing template details")
    public static class TemplateDetailResponse {
        @Schema(description = "Template information")
        private TemplateInfo template;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "TemplateInfo", description = "Template information")
    public static class TemplateInfo {
        @Schema(description = "Template ID", example = "database")
        private String id;

        @Schema(description = "Template display name", example = "Database")
        private String name;

        @Schema(description = "Template description")
        private String description;

        @Schema(description = "Template category", example = "Database")
        private String category;

        @Schema(description = "Template content (YAML)")
        private String content;
    }
}

