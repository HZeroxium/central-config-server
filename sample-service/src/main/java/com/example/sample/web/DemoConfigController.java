package com.example.sample.web;

import com.example.sample.config.DemoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for demo configuration visualization.
 * 
 * Exposes demo-specific configuration properties for the frontend dashboard.
 * Includes instance metadata for identification.
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoConfigController {

    private final DemoProperties demoProperties;
    private final Environment environment;

    /**
     * Get demo configuration for visualization.
     * 
     * @return map containing demo config properties and instance metadata
     */
    @GetMapping("/config")
    public Map<String, Object> getDemoConfig() {
        Map<String, Object> response = new HashMap<>();
        
        // Demo properties
        Map<String, Object> demo = new HashMap<>();
        
        // Theme
        Map<String, String> theme = new HashMap<>();
        theme.put("primaryColor", demoProperties.getTheme().getPrimaryColor());
        theme.put("secondaryColor", demoProperties.getTheme().getSecondaryColor());
        theme.put("backgroundColor", demoProperties.getTheme().getBackgroundColor());
        theme.put("textColor", demoProperties.getTheme().getTextColor());
        demo.put("theme", theme);
        
        // Banner
        Map<String, Object> banner = new HashMap<>();
        banner.put("text", demoProperties.getBanner().getText());
        banner.put("show", demoProperties.getBanner().isShow());
        banner.put("logoUrl", demoProperties.getBanner().getLogoUrl());
        demo.put("banner", banner);
        
        // Metrics
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("retryCount", demoProperties.getMetrics().getRetryCount());
        metrics.put("cacheTtl", demoProperties.getMetrics().getCacheTtl());
        demo.put("metrics", metrics);
        
        // Status
        Map<String, String> status = new HashMap<>();
        status.put("badge", demoProperties.getStatus().getBadge());
        status.put("badgeColor", demoProperties.getStatus().getBadgeColor());
        demo.put("status", status);
        
        response.put("demo", demo);
        
        // Instance metadata
        Map<String, Object> instance = new HashMap<>();
        instance.put("instanceId", getInstanceId());
        instance.put("host", getHost());
        instance.put("port", environment.getProperty("server.port", Integer.class, 8080));
        instance.put("profile", getActiveProfile());
        instance.put("serviceName", environment.getProperty("spring.application.name", "sample-service"));
        response.put("instance", instance);
        
        // Timestamp
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
    
    private String getInstanceId() {
        String instanceId = environment.getProperty("zcm.sdk.instance.id");
        if (instanceId != null && !instanceId.isBlank()) {
            return instanceId;
        }
        
        String consulInstanceId = environment.getProperty("spring.cloud.consul.discovery.instance-id");
        if (consulInstanceId != null && !consulInstanceId.isBlank()) {
            int port = environment.getProperty("server.port", Integer.class, 8080);
            return consulInstanceId.replace("${server.port}", String.valueOf(port));
        }
        
        return getHost() + ":" + environment.getProperty("server.port", Integer.class, 8080);
    }
    
    private String getHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? profiles[0] : "default";
    }
}

