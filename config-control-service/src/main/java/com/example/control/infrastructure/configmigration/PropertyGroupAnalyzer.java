package com.example.control.infrastructure.configmigration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Service for analyzing property groups in configuration files
 * and determining optimal @ConfigurationProperties structure.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class PropertyGroupAnalyzer {

    private final Yaml yaml;

    public PropertyGroupAnalyzer() {
        this.yaml = new Yaml();
    }

    /**
     * Analyzes YAML content and identifies property groups by common prefixes.
     *
     * @param yamlContent the YAML content
     * @return map of prefix to property group details
     */
    public Map<String, PropertyGroupInfo> analyzeGroups(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            if (yamlMap == null) {
                return Collections.emptyMap();
            }

            Map<String, PropertyGroupInfo> groups = new LinkedHashMap<>();

            // Analyze top-level keys
            for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Skip Spring Boot internal properties
                if (isSpringBootInternal(key)) {
                    continue;
                }

                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) value;
                    PropertyGroupInfo groupInfo = analyzeGroup(key, map);
                    if (groupInfo != null) {
                        groups.put(key, groupInfo);
                    }
                }
            }

            return groups;

        } catch (Exception e) {
            log.error("Failed to analyze property groups", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Analyzes a single property group.
     *
     * @param prefix the prefix
     * @param map    the property map
     * @return PropertyGroupInfo
     */
    @SuppressWarnings("unchecked")
    private PropertyGroupInfo analyzeGroup(String prefix, Map<String, Object> map) {
        if (map.isEmpty()) {
            return null;
        }

        List<FieldInfo> fields = new ArrayList<>();
        List<PropertyGroupInfo> nestedGroups = new ArrayList<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            if (fieldValue instanceof Map) {
                Map<String, Object> nestedMap = (Map<String, Object>) fieldValue;
                if (!nestedMap.isEmpty()) {
                    String nestedPrefix = prefix + "." + fieldName;
                    PropertyGroupInfo nested = analyzeGroup(nestedPrefix, nestedMap);
                    if (nested != null) {
                        nestedGroups.add(nested);
                    }
                }
            } else {
                FieldInfo field = createFieldInfo(fieldName, fieldValue);
                fields.add(field);
            }
        }

        if (fields.isEmpty() && nestedGroups.isEmpty()) {
            return null;
        }

        return PropertyGroupInfo.builder()
                .prefix(prefix)
                .className(toClassName(prefix))
                .fields(fields)
                .nestedGroups(nestedGroups)
                .build();
    }

    /**
     * Creates field information from name and value.
     *
     * @param name  field name
     * @param value field value
     * @return FieldInfo
     */
    private FieldInfo createFieldInfo(String name, Object value) {
        String type = inferJavaType(value);
        boolean nullable = value == null;
        String defaultValue = value != null ? value.toString() : null;

        return FieldInfo.builder()
                .name(toCamelCase(name))
                .originalName(name)
                .type(type)
                .nullable(nullable)
                .defaultValue(defaultValue)
                .build();
    }

    /**
     * Infers Java type from value.
     *
     * @param value the value
     * @return Java type name
     */
    private String inferJavaType(Object value) {
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
                String elementType = inferJavaType(list.get(0));
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
     * Converts prefix to class name (e.g., "app.database" -> "DatabaseProperties").
     *
     * @param prefix the prefix
     * @return class name
     */
    private String toClassName(String prefix) {
        String[] parts = prefix.split("\\.");
        String lastPart = parts[parts.length - 1];
        return capitalize(toCamelCase(lastPart)) + "Properties";
    }

    /**
     * Converts kebab-case or snake_case to camelCase.
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
            if (c == '-' || c == '_') {
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

    /**
     * Checks if a key is a Spring Boot internal property.
     *
     * @param key the key
     * @return true if internal
     */
    private boolean isSpringBootInternal(String key) {
        return key.startsWith("spring.") ||
               key.startsWith("server.") ||
               key.startsWith("management.") ||
               key.startsWith("logging.") ||
               key.startsWith("info.");
    }

    /**
     * Property group information.
     */
    @lombok.Data
    @lombok.Builder
    public static class PropertyGroupInfo {
        private String prefix;
        private String className;
        private List<FieldInfo> fields;
        private List<PropertyGroupInfo> nestedGroups;
    }

    /**
     * Field information.
     */
    @lombok.Data
    @lombok.Builder
    public static class FieldInfo {
        private String name;
        private String originalName;
        private String type;
        private boolean nullable;
        private String defaultValue;
    }
}

