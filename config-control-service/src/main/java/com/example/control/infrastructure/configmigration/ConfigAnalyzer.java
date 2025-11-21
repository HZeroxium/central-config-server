package com.example.control.infrastructure.configmigration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Service for analyzing existing configuration files and extracting
 * information needed for SDK integration.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ConfigAnalyzer {

    private final Yaml yaml;

    public ConfigAnalyzer() {
        this.yaml = new Yaml();
    }

    /**
     * Analyzes YAML configuration and extracts relevant information.
     *
     * @param yamlContent the YAML content
     * @return analysis result
     */
    public ConfigAnalysisResult analyze(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            throw new IllegalArgumentException("YAML content cannot be null or empty");
        }

        try {
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            if (yamlMap == null) {
                yamlMap = new LinkedHashMap<>();
            }

            ConfigAnalysisResult result = ConfigAnalysisResult.builder()
                    .serviceName(extractServiceName(yamlMap))
                    .configServerUrl(extractConfigServerUrl(yamlMap))
                    .consulHost(extractConsulHost(yamlMap))
                    .consulPort(extractConsulPort(yamlMap))
                    .kafkaBootstrapServers(extractKafkaBootstrapServers(yamlMap))
                    .hasFeatureFlags(hasFeatureFlags(yamlMap))
                    .hasKvStore(hasKvStore(yamlMap))
                    .hasDatabase(hasDatabase(yamlMap))
                    .hasRedis(hasRedis(yamlMap))
                    .suggestions(generateSuggestions(yamlMap))
                    .build();

            return result;

        } catch (Exception e) {
            log.error("Failed to analyze configuration", e);
            throw new RuntimeException("Failed to analyze configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts service name from configuration.
     *
     * @param yamlMap the YAML map
     * @return service name or null
     */
    @SuppressWarnings("unchecked")
    private String extractServiceName(Map<String, Object> yamlMap) {
        Object spring = yamlMap.get("spring");
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            Object application = springMap.get("application");
            if (application instanceof Map) {
                Map<String, Object> appMap = (Map<String, Object>) application;
                Object name = appMap.get("name");
                if (name != null) {
                    return name.toString();
                }
            }
        }
        return null;
    }

    /**
     * Extracts Config Server URL from configuration.
     *
     * @param yamlMap the YAML map
     * @return Config Server URL or null
     */
    @SuppressWarnings("unchecked")
    private String extractConfigServerUrl(Map<String, Object> yamlMap) {
        Object spring = yamlMap.get("spring");
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            Object cloud = springMap.get("cloud");
            if (cloud instanceof Map) {
                Map<String, Object> cloudMap = (Map<String, Object>) cloud;
                Object config = cloudMap.get("config");
                if (config instanceof Map) {
                    Map<String, Object> configMap = (Map<String, Object>) config;
                    Object uri = configMap.get("uri");
                    if (uri != null) {
                        return uri.toString();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extracts Consul host from configuration.
     *
     * @param yamlMap the YAML map
     * @return Consul host or null
     */
    @SuppressWarnings("unchecked")
    private String extractConsulHost(Map<String, Object> yamlMap) {
        Object spring = yamlMap.get("spring");
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            Object cloud = springMap.get("cloud");
            if (cloud instanceof Map) {
                Map<String, Object> cloudMap = (Map<String, Object>) cloud;
                Object consul = cloudMap.get("consul");
                if (consul instanceof Map) {
                    Map<String, Object> consulMap = (Map<String, Object>) consul;
                    Object host = consulMap.get("host");
                    if (host != null) {
                        return host.toString();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extracts Consul port from configuration.
     *
     * @param yamlMap the YAML map
     * @return Consul port or null
     */
    @SuppressWarnings("unchecked")
    private Integer extractConsulPort(Map<String, Object> yamlMap) {
        Object spring = yamlMap.get("spring");
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            Object cloud = springMap.get("cloud");
            if (cloud instanceof Map) {
                Map<String, Object> cloudMap = (Map<String, Object>) cloud;
                Object consul = cloudMap.get("consul");
                if (consul instanceof Map) {
                    Map<String, Object> consulMap = (Map<String, Object>) consul;
                    Object port = consulMap.get("port");
                    if (port instanceof Number) {
                        return ((Number) port).intValue();
                    } else if (port != null) {
                        try {
                            return Integer.parseInt(port.toString());
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extracts Kafka bootstrap servers from configuration.
     *
     * @param yamlMap the YAML map
     * @return Kafka bootstrap servers or null
     */
    @SuppressWarnings("unchecked")
    private String extractKafkaBootstrapServers(Map<String, Object> yamlMap) {
        Object spring = yamlMap.get("spring");
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            Object kafka = springMap.get("kafka");
            if (kafka instanceof Map) {
                Map<String, Object> kafkaMap = (Map<String, Object>) kafka;
                Object bootstrapServers = kafkaMap.get("bootstrap-servers");
                if (bootstrapServers != null) {
                    return bootstrapServers.toString();
                }
            }
        }
        return null;
    }

    /**
     * Checks if configuration has feature flags.
     *
     * @param yamlMap the YAML map
     * @return true if feature flags detected
     */
    private boolean hasFeatureFlags(Map<String, Object> yamlMap) {
        // Check for common feature flag patterns
        return yamlMap.containsKey("feature-flags") ||
               yamlMap.containsKey("featureFlags") ||
               yamlMap.containsKey("unleash");
    }

    /**
     * Checks if configuration has KV store.
     *
     * @param yamlMap the YAML map
     * @return true if KV store detected
     */
    private boolean hasKvStore(Map<String, Object> yamlMap) {
        return yamlMap.containsKey("kv") ||
               yamlMap.containsKey("consul") && yamlMap.get("consul") instanceof Map;
    }

    /**
     * Checks if configuration has database.
     *
     * @param yamlMap the YAML map
     * @return true if database detected
     */
    @SuppressWarnings("unchecked")
    private boolean hasDatabase(Map<String, Object> yamlMap) {
        Object spring = yamlMap.get("spring");
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            return springMap.containsKey("datasource") ||
                   springMap.containsKey("data");
        }
        return false;
    }

    /**
     * Checks if configuration has Redis.
     *
     * @param yamlMap the YAML map
     * @return true if Redis detected
     */
    @SuppressWarnings("unchecked")
    private boolean hasRedis(Map<String, Object> yamlMap) {
        Object spring = yamlMap.get("spring");
        if (spring instanceof Map) {
            Map<String, Object> springMap = (Map<String, Object>) spring;
            Object data = springMap.get("data");
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                return dataMap.containsKey("redis");
            }
        }
        return yamlMap.containsKey("redis");
    }

    /**
     * Generates suggestions based on configuration analysis.
     *
     * @param yamlMap the YAML map
     * @return list of suggestions
     */
    private List<String> generateSuggestions(Map<String, Object> yamlMap) {
        List<String> suggestions = new ArrayList<>();

        if (hasDatabase(yamlMap)) {
            suggestions.add("Consider using @ConfigurationProperties for database configuration");
        }

        if (hasRedis(yamlMap)) {
            suggestions.add("Consider enabling SDK KV store feature for Redis configuration");
        }

        if (hasFeatureFlags(yamlMap)) {
            suggestions.add("Consider enabling SDK Feature Flags integration");
        }

        if (extractKafkaBootstrapServers(yamlMap) != null) {
            suggestions.add("Kafka detected - SDK bus refresh will use existing Kafka configuration");
        }

        return suggestions;
    }

    /**
     * Configuration analysis result.
     */
    @lombok.Data
    @lombok.Builder
    public static class ConfigAnalysisResult {
        private String serviceName;
        private String configServerUrl;
        private String consulHost;
        private Integer consulPort;
        private String kafkaBootstrapServers;
        private boolean hasFeatureFlags;
        private boolean hasKvStore;
        private boolean hasDatabase;
        private boolean hasRedis;
        private List<String> suggestions;
    }
}

