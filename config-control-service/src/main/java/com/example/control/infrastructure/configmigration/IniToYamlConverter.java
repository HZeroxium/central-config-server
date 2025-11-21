package com.example.control.infrastructure.configmigration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for converting INI format files to YAML format.
 * <p>
 * Supports INI sections [section] and key=value pairs.
 * Converts sections to nested YAML structures.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class IniToYamlConverter {

    private final Yaml yaml;

    public IniToYamlConverter() {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
    }

    /**
     * Converts INI file content to YAML format.
     * Supports sections [section] and key=value pairs.
     *
     * @param iniContent the INI file content
     * @return YAML content as string
     * @throws IllegalArgumentException if INI content is invalid
     */
    public String convert(String iniContent) {
        if (iniContent == null || iniContent.isBlank()) {
            throw new IllegalArgumentException("INI content cannot be null or empty");
        }

        try {
            INIConfiguration iniConfig = new INIConfiguration();
            try {
                iniConfig.read(new StringReader(iniContent));
            } catch (java.io.IOException e) {
                log.error("IO error while reading INI content", e);
                throw new IllegalArgumentException("IO error when reading INI content: " + e.getMessage(), e);
            }

            Map<String, Object> yamlMap = new LinkedHashMap<>();

            // Process global section (keys without section)
            for (Iterator<String> it = iniConfig.getKeys(); it.hasNext(); ) {
                String key = it.next();
                if (!key.contains(".")) {
                    // Global key
                    String value = iniConfig.getString(key);
                    yamlMap.put(key, convertValue(value));
                }
            }

            // Process sections
            for (String sectionName : iniConfig.getSections()) {
                Map<String, Object> sectionMap = new LinkedHashMap<>();
                Iterator<String> keysIterator = iniConfig.getKeys(sectionName);
                while (keysIterator.hasNext()) {
                    String key = keysIterator.next();
                    String value = iniConfig.getSection(sectionName).getString(key);
                    sectionMap.put(key, convertValue(value));
                }
                yamlMap.put(sectionName, sectionMap);
            }

            StringWriter writer = new StringWriter();
            yaml.dump(yamlMap, writer);
            return writer.toString();

        } catch (ConfigurationException e) {
            log.error("Failed to parse INI content", e);
            throw new IllegalArgumentException("Invalid INI format: " + e.getMessage(), e);
        }
    }

    /**
     * Converts INI file to YAML with Spring Boot structure.
     * Groups properties by prefix for @ConfigurationProperties.
     *
     * @param iniContent the INI file content
     * @return map with Spring Boot structure
     */
    public Map<String, Object> convertToSpringBootStructure(String iniContent) {
        String yamlContent = convert(iniContent);
        Yaml yamlParser = new Yaml();
        return yamlParser.load(yamlContent);
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

