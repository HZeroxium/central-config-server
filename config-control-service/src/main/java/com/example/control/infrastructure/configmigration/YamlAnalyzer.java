package com.example.control.infrastructure.configmigration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Service for analyzing YAML configuration files and suggesting
 * @ConfigurationProperties structure.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class YamlAnalyzer {

    private final Yaml yaml;

    public YamlAnalyzer() {
        this.yaml = new Yaml();
    }

    /**
     * Analyzes YAML content and identifies property groups suitable for
     * @ConfigurationProperties classes.
     *
     * @param yamlContent the YAML content to analyze
     * @return list of suggested property groups
     */
    public List<PropertyGroup> analyzePropertyGroups(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            if (yamlMap == null) {
                return Collections.emptyList();
            }

            List<PropertyGroup> groups = new ArrayList<>();

            for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Skip Spring Boot internal properties
                if (key.startsWith("spring.") || key.startsWith("server.") ||
                    key.startsWith("management.") || key.startsWith("logging.")) {
                    continue;
                }

                PropertyGroup group = analyzePropertyGroup(key, value);
                if (group != null) {
                    groups.add(group);
                }
            }

            return groups;

        } catch (Exception e) {
            log.error("Failed to analyze YAML content", e);
            return Collections.emptyList();
        }
    }

    /**
     * Analyzes a single property group.
     *
     * @param prefix the property prefix
     * @param value  the property value
     * @return PropertyGroup or null if not suitable
     */
    @SuppressWarnings("unchecked")
    private PropertyGroup analyzePropertyGroup(String prefix, Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.isEmpty()) {
                return null;
            }

            List<PropertyField> fields = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                PropertyField field = analyzeField(entry.getKey(), entry.getValue());
                if (field != null) {
                    fields.add(field);
                }
            }

            if (fields.isEmpty()) {
                return null;
            }

            return PropertyGroup.builder()
                    .prefix(prefix)
                    .fields(fields)
                    .nestedGroups(analyzeNestedGroups(prefix, map))
                    .build();
        }

        return null;
    }

    /**
     * Analyzes nested groups within a map.
     *
     * @param parentPrefix the parent prefix
     * @param map          the map to analyze
     * @return list of nested property groups
     */
    @SuppressWarnings("unchecked")
    private List<PropertyGroup> analyzeNestedGroups(String parentPrefix, Map<String, Object> map) {
        List<PropertyGroup> nestedGroups = new ArrayList<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                if (!nestedMap.isEmpty() && hasComplexStructure(nestedMap)) {
                    String nestedPrefix = parentPrefix + "." + key;
                    PropertyGroup nested = analyzePropertyGroup(nestedPrefix, value);
                    if (nested != null) {
                        nestedGroups.add(nested);
                    }
                }
            }
        }

        return nestedGroups;
    }

    /**
     * Checks if a map has a complex structure (not just primitive values).
     *
     * @param map the map to check
     * @return true if complex structure
     */
    private boolean hasComplexStructure(Map<String, Object> map) {
        for (Object value : map.values()) {
            if (value instanceof Map || value instanceof List) {
                return true;
            }
        }
        return false;
    }

    /**
     * Analyzes a single field and determines its type and validation needs.
     *
     * @param name  the field name
     * @param value the field value
     * @return PropertyField
     */
    private PropertyField analyzeField(String name, Object value) {
        if (value == null) {
            return PropertyField.builder()
                    .name(name)
                    .type("String")
                    .nullable(true)
                    .build();
        }

        String type = inferType(value);
        boolean nullable = value == null;

        List<String> validationAnnotations = new ArrayList<>();
        if (!nullable && type.equals("String")) {
            validationAnnotations.add("@NotBlank");
        } else if (!nullable) {
            validationAnnotations.add("@NotNull");
        }

        if (type.equals("Integer") || type.equals("Long")) {
            if (value instanceof Number) {
                Number num = (Number) value;
                if (num.intValue() >= 0) {
                    validationAnnotations.add("@Min(0)");
                }
            }
        }

        return PropertyField.builder()
                .name(name)
                .type(type)
                .nullable(nullable)
                .validationAnnotations(validationAnnotations)
                .defaultValue(value.toString())
                .build();
    }

    /**
     * Infers Java type from value.
     *
     * @param value the value
     * @return Java type name
     */
    private String inferType(Object value) {
        if (value == null) {
            return "String";
        }

        if (value instanceof Boolean) {
            return "Boolean";
        }
        if (value instanceof Integer) {
            return "Integer";
        }
        if (value instanceof Long) {
            return "Long";
        }
        if (value instanceof Double || value instanceof Float) {
            return "Double";
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                String elementType = inferType(list.get(0));
                return "List<" + elementType + ">";
            }
            return "List<String>";
        }
        if (value instanceof Map) {
            return "Map<String, Object>";
        }

        return "String";
    }

    /**
     * Property group representation.
     */
    @lombok.Data
    @lombok.Builder
    public static class PropertyGroup {
        private String prefix;
        private List<PropertyField> fields;
        private List<PropertyGroup> nestedGroups;
    }

    /**
     * Property field representation.
     */
    @lombok.Data
    @lombok.Builder
    public static class PropertyField {
        private String name;
        private String type;
        private boolean nullable;
        private List<String> validationAnnotations;
        private String defaultValue;
    }
}

