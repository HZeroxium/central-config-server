package com.example.control.application.external;

import com.example.control.api.exception.exceptions.ExternalServiceException;
import com.example.control.infrastructure.config.ConfigServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigServerClient {

    private final ConfigServerProperties properties;
    private final RestClient restClient = RestClient.create();

    public String getActuatorIndex() {
        return get("/actuator");
    }

    public String getActuatorPath(String path) {
        return get("/actuator/" + path);
    }

    public String getEnvironment(String application, String profiles) {
        String path = "/" + application + "/" + (profiles == null || profiles.isBlank() ? "default" : profiles);
        return get(path);
    }

    public String getEnvironmentWithLabel(String application, String profiles, String label) {
        String prof = (profiles == null || profiles.isBlank()) ? "default" : profiles;
        String path = "/" + application + "/" + prof + "/" + label;
        return get(path);
    }

    private String get(String path) {
        try {
            String url = normalize(properties.getUrl()) + path;
            return restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("ConfigServer call failed: {}", path, e);
            throw new ExternalServiceException("config-server", e.getMessage(), e);
        }
    }

    private String normalize(String base) {
        if (base.endsWith("/"))
            return base.substring(0, base.length() - 1);
        return base;
    }
}
