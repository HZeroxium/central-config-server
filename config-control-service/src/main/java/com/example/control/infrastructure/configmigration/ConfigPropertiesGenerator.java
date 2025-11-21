package com.example.control.infrastructure.configmigration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for generating @ConfigurationProperties classes from YAML configuration.
 * <p>
 * Analyzes YAML structure and generates Java classes with proper nesting,
 * validation annotations, and JavaDoc comments.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigPropertiesGenerator {

    private final PropertyGroupAnalyzer groupAnalyzer;
    private final ValidationAnnotationGenerator validationGenerator;

    /**
     * Generates @ConfigurationProperties classes from YAML content.
     *
     * @param yamlContent the YAML content
     * @param packageName the target package name
     * @return generation result with class code and metadata
     */
    public ConfigPropertiesGenerationResult generate(String yamlContent, String packageName) {
        if (yamlContent == null || yamlContent.isBlank()) {
            throw new IllegalArgumentException("YAML content cannot be null or empty");
        }

        if (packageName == null || packageName.isBlank()) {
            packageName = "com.example.config";
        }

        try {
            Map<String, PropertyGroupAnalyzer.PropertyGroupInfo> groups = groupAnalyzer.analyzeGroups(yamlContent);

            List<GeneratedClass> classes = new ArrayList<>();
            Set<String> allImports = new LinkedHashSet<>();

            for (PropertyGroupAnalyzer.PropertyGroupInfo group : groups.values()) {
                GeneratedClass generatedClass = generateClass(group, packageName);
                classes.add(generatedClass);
                allImports.addAll(generatedClass.getImports());
            }

            String metadataJson = generateMetadataJson(groups, packageName);

            return ConfigPropertiesGenerationResult.builder()
                    .classes(classes)
                    .allImports(new ArrayList<>(allImports))
                    .metadataJson(metadataJson)
                    .packageName(packageName)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate ConfigurationProperties classes", e);
            throw new RuntimeException("Failed to generate classes: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a single @ConfigurationProperties class.
     *
     * @param groupInfo  the property group info
     * @param packageName the package name
     * @return GeneratedClass
     */
    private GeneratedClass generateClass(PropertyGroupAnalyzer.PropertyGroupInfo groupInfo, String packageName) {
        StringBuilder code = new StringBuilder();
        Set<String> imports = new LinkedHashSet<>();

        // Package declaration
        code.append("package ").append(packageName).append(";\n\n");

        // Imports
        imports.add("import org.springframework.boot.context.properties.ConfigurationProperties;");
        imports.add("import lombok.Data;");
        imports.add("import jakarta.validation.Valid;");
        imports.add("import jakarta.validation.constraints.*;");

        // Generate nested classes first
        List<String> nestedClassCodes = new ArrayList<>();
        for (PropertyGroupAnalyzer.PropertyGroupInfo nested : groupInfo.getNestedGroups()) {
            GeneratedClass nestedClass = generateNestedClass(nested, packageName);
            nestedClassCodes.add(nestedClass.getCode());
            imports.addAll(nestedClass.getImports());
        }

        // Class JavaDoc
        code.append("/**\n");
        code.append(" * Configuration properties for ").append(groupInfo.getPrefix()).append(".\n");
        code.append(" *\n");
        code.append(" * <p>Auto-generated from YAML configuration.\n");
        code.append(" * Prefix: ").append(groupInfo.getPrefix()).append("\n");
        code.append(" */\n");

        // Class annotation
        code.append("@ConfigurationProperties(prefix = \"").append(groupInfo.getPrefix()).append("\")\n");
        code.append("@Data\n");
        code.append("public class ").append(groupInfo.getClassName()).append(" {\n\n");

        // Fields
        for (PropertyGroupAnalyzer.FieldInfo field : groupInfo.getFields()) {
            generateField(code, field, imports);
        }

        // Nested properties
        for (PropertyGroupAnalyzer.PropertyGroupInfo nested : groupInfo.getNestedGroups()) {
            String nestedClassName = nested.getClassName();
            String fieldName = toCamelCase(nested.getPrefix().substring(groupInfo.getPrefix().length() + 1));
            code.append("    @Valid\n");
            code.append("    private ").append(nestedClassName).append(" ").append(fieldName).append(" = new ").append(nestedClassName).append("();\n\n");
        }

        code.append("}\n");

        // Add nested classes after main class
        for (String nestedCode : nestedClassCodes) {
            code.append("\n").append(nestedCode);
        }

        return GeneratedClass.builder()
                .className(groupInfo.getClassName())
                .prefix(groupInfo.getPrefix())
                .code(code.toString())
                .imports(new ArrayList<>(imports))
                .build();
    }

    /**
     * Generates a nested @ConfigurationProperties class.
     *
     * @param nestedInfo the nested group info
     * @param packageName the package name
     * @return GeneratedClass
     */
    private GeneratedClass generateNestedClass(PropertyGroupAnalyzer.PropertyGroupInfo nestedInfo, String packageName) {
        StringBuilder code = new StringBuilder();
        Set<String> imports = new LinkedHashSet<>();

        imports.add("import org.springframework.boot.context.properties.ConfigurationProperties;");
        imports.add("import lombok.Data;");
        imports.add("import jakarta.validation.constraints.*;");

        // Class JavaDoc
        code.append("/**\n");
        code.append(" * Configuration properties for ").append(nestedInfo.getPrefix()).append(".\n");
        code.append(" */\n");

        // Class annotation
        code.append("@ConfigurationProperties(prefix = \"").append(nestedInfo.getPrefix()).append("\")\n");
        code.append("@Data\n");
        code.append("public static class ").append(nestedInfo.getClassName()).append(" {\n\n");

        // Fields
        for (PropertyGroupAnalyzer.FieldInfo field : nestedInfo.getFields()) {
            generateField(code, field, imports);
        }

        code.append("}\n");

        return GeneratedClass.builder()
                .className(nestedInfo.getClassName())
                .prefix(nestedInfo.getPrefix())
                .code(code.toString())
                .imports(new ArrayList<>(imports))
                .build();
    }

    /**
     * Generates a field declaration with annotations.
     *
     * @param code    the code builder
     * @param field   the field info
     * @param imports the imports set
     */
    private void generateField(StringBuilder code, PropertyGroupAnalyzer.FieldInfo field, Set<String> imports) {
        List<String> annotations = validationGenerator.generateAnnotations(
                field.getName(), field.getType(), field.isNullable(), field.getDefaultValue());

        for (String annotation : annotations) {
            code.append("    ").append(annotation).append("\n");
        }

        String defaultValue = "";
        if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
            if (field.getType().equals("String")) {
                defaultValue = " = \"" + escapeString(field.getDefaultValue()) + "\"";
            } else if (field.getType().equals("Boolean")) {
                defaultValue = " = " + field.getDefaultValue();
            } else if (field.getType().equals("Integer") || field.getType().equals("Long") || field.getType().equals("Double")) {
                defaultValue = " = " + field.getDefaultValue();
            }
        }

        code.append("    private ").append(field.getType()).append(" ").append(field.getName())
                .append(defaultValue).append(";\n\n");

        // Add imports for annotations
        imports.addAll(validationGenerator.generateImports(annotations));
    }

    /**
     * Escapes string for Java code.
     *
     * @param str the string
     * @return escaped string
     */
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Converts to camelCase.
     *
     * @param name the name
     * @return camelCase name
     */
    private String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : name.toCharArray()) {
            if (c == '-' || c == '_' || c == '.') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

    /**
     * Generates Spring Boot configuration metadata JSON.
     *
     * @param groups     the property groups
     * @param packageName the package name
     * @return metadata JSON string
     */
    private String generateMetadataJson(Map<String, PropertyGroupAnalyzer.PropertyGroupInfo> groups, String packageName) {
        // This is a simplified version - full implementation would generate complete metadata
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"groups\": [\n");

        boolean first = true;
        for (PropertyGroupAnalyzer.PropertyGroupInfo group : groups.values()) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            json.append("    {\n");
            json.append("      \"name\": \"").append(group.getPrefix()).append("\",\n");
            json.append("      \"type\": \"").append(packageName).append(".").append(group.getClassName()).append("\",\n");
            json.append("      \"sourceType\": \"").append(packageName).append(".").append(group.getClassName()).append("\"\n");
            json.append("    }");
        }

        json.append("\n  ],\n");
        json.append("  \"properties\": [\n");

        first = true;
        for (PropertyGroupAnalyzer.PropertyGroupInfo group : groups.values()) {
            for (PropertyGroupAnalyzer.FieldInfo field : group.getFields()) {
                if (!first) {
                    json.append(",\n");
                }
                first = false;
                json.append("    {\n");
                json.append("      \"name\": \"").append(group.getPrefix()).append(".").append(field.getOriginalName()).append("\",\n");
                json.append("      \"type\": \"").append(field.getType()).append("\",\n");
                json.append("      \"description\": \"Configuration property for ").append(field.getOriginalName()).append("\"\n");
                json.append("    }");
            }
        }

        json.append("\n  ]\n");
        json.append("}\n");

        return json.toString();
    }

    /**
     * Generation result.
     */
    @lombok.Data
    @lombok.Builder
    public static class ConfigPropertiesGenerationResult {
        private List<GeneratedClass> classes;
        private List<String> allImports;
        private String metadataJson;
        private String packageName;
    }

    /**
     * Generated class representation.
     */
    @lombok.Data
    @lombok.Builder
    public static class GeneratedClass {
        private String className;
        private String prefix;
        private String code;
        private List<String> imports;
    }
}

