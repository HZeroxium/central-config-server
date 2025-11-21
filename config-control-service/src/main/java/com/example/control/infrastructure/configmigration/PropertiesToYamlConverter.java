package com.example.control.infrastructure.configmigration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Service for converting Properties format files to YAML format.
 * <p>
 * Converts flat key=value properties to hierarchical YAML structure
 * by splitting keys on dots (.).
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class PropertiesToYamlConverter {

    private final Yaml yaml;

    public PropertiesToYamlConverter() {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
    }

    /**
     * Converts Properties file content to YAML format.
     * Converts flat key=value pairs to hierarchical YAML structure.
     *
     * @param propertiesContent the Properties file content
     * @return YAML content as string
     * @throws IllegalArgumentException if Properties content is invalid
     */
    public String convert(String propertiesContent) {
        if (propertiesContent == null || propertiesContent.isBlank()) {
            throw new IllegalArgumentException("Properties content cannot be null or empty");
        }

        try {
            Properties props = new Properties();
            props.load(new StringReader(propertiesContent));

            Map<String, Object> yamlMap = new LinkedHashMap<>();

            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                putNestedValue(yamlMap, key, convertValue(value));
            }

            StringWriter writer = new StringWriter();
            yaml.dump(yamlMap, writer);
            return writer.toString();

        } catch (Exception e) {
            log.error("Failed to parse Properties content", e);
            throw new IllegalArgumentException("Invalid Properties format: " + e.getMessage(), e);
        }
    }

    /**
     * Converts Properties file to YAML with Spring Boot structure.
     *
     * @param propertiesContent the Properties file content
     * @return map with Spring Boot structure
     */
    public Map<String, Object> convertToSpringBootStructure(String propertiesContent) {
        String yamlContent = convert(propertiesContent);
        Yaml yamlParser = new Yaml();
        return yamlParser.load(yamlContent);
    }

    /**
     * Puts a value into a nested map structure based on dot-separated key.
     *
     * @param map   the target map
     * @param key   the dot-separated key (e.g., "spring.datasource.url")
     * @param value the value to put
     */
    @SuppressWarnings("unchecked")
    private void putNestedValue(Map<String, Object> map, String key, Object value) {
        String[] parts = key.split("\\.", -1);
        Map<String, Object> current = map;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }

            Object next = current.get(part);
            if (next == null) {
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(part, newMap);
                current = newMap;
            } else if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                // Conflict: key exists but is not a map, create new map
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(part, newMap);
                current = newMap;
            }
        }

        String lastPart = parts[parts.length - 1];
        if (!lastPart.isEmpty()) {
            current.put(lastPart, value);
        }
    }

    /**
     * Converts a string value to appropriate type (Integer, Boolean, or String).
     *
     * @param value the string value
     * @return converted value
     */
    private Object convertValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        // Try boolean
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }

        // Try integer
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            // Not an integer, continue
        }

        // Try long
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            // Not a long, continue
        }

        // Try double
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            // Not a double, return as string
        }

        return value;
    }
}

