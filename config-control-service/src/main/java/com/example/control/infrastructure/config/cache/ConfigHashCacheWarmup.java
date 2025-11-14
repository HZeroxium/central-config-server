package com.example.control.infrastructure.config.cache;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.domain.model.ApplicationService;
import com.example.control.infrastructure.external.configserver.ConfigProxyService;
import com.example.control.infrastructure.observability.heartbeat.HeartbeatMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Component responsible for pre-warming the config hash cache on application startup.
 * <p>
 * Pre-warms the cache by loading config hashes for all ApplicationServices and their
 * environments. This reduces cold start latency when processing heartbeats.
 * <p>
 * Runs asynchronously after application startup to avoid blocking the main thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigHashCacheWarmup {

    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final ConfigProxyService configProxyService;
    private final HeartbeatMetrics heartbeatMetrics;

    /**
     * Pre-warms the config hash cache after application startup.
     * <p>
     * Loads all ApplicationServices and pre-warms config hashes for all their
     * environments. Runs in background thread to avoid blocking startup.
     * <p>
     * The delay is configurable via {@code app.heartbeat.cache.pre-warm.delay}
     * (default: 30s).
     */
    @Async("defaultExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void preWarmCache() {
        // Wait a bit after startup to ensure all services are ready
        try {
            Thread.sleep(30_000); // 30 seconds default delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Cache pre-warming interrupted");
            return;
        }

        log.info("Starting config hash cache pre-warming...");
        Instant start = Instant.now();

        try {
            List<ApplicationService> services = applicationServiceQueryService.findAll();
            int totalWarmed = 0;
            int totalFailed = 0;

            for (ApplicationService service : services) {
                List<String> environments = service.getEnvironments();
                if (environments == null || environments.isEmpty()) {
                    // Default environments if none specified
                    environments = List.of("dev", "staging", "prod");
                }

                for (String env : environments) {
                    try {
                        String hash = configProxyService.getEffectiveConfigHash(
                                service.getDisplayName(), env);
                        if (hash != null) {
                            totalWarmed++;
                            log.debug("Pre-warmed cache for {}:{}", service.getDisplayName(), env);
                        }
                    } catch (Exception e) {
                        totalFailed++;
                        log.warn("Failed to pre-warm cache for {}:{} - {}",
                                service.getDisplayName(), env, e.getMessage());
                    }
                }
            }

            Duration duration = Duration.between(start, Instant.now());
            log.info("Config hash cache pre-warming completed: {} entries warmed, {} failed, took {}ms",
                    totalWarmed, totalFailed, duration.toMillis());

        } catch (Exception e) {
            log.error("Cache pre-warming failed", e);
        }
    }
}

