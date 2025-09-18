package com.example.user.kafka;

import com.example.kafka.service.WorkerSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * REST Controller for Worker control and monitoring
 * Provides endpoints to control worker behavior and monitor status
 */
@RestController
@RequestMapping("/control")
@RequiredArgsConstructor
@Slf4j
public class WorkerControlController {

    private final WorkerSettings workerSettings;
    private final KafkaListenerEndpointRegistry registry;

    /**
     * Set fail rate for a specific phase
     */
    @PostMapping("/fail-rate")
    public Map<String, Object> setFailRate(@RequestParam int phase, @RequestParam double rate) {
        if (phase < 1 || phase > 4) {
            throw new IllegalArgumentException("Phase must be between 1 and 4");
        }
        
        workerSettings.setFailRate(phase, rate);
        log.info("Set fail rate for phase {} to {}", phase, rate);
        
        return Map.of("phase", phase, "failRate", rate, "status", "updated");
    }

    /**
     * Set sleep time for a specific phase
     */
    @PostMapping("/sleep")
    public Map<String, Object> setSleep(@RequestParam int phase, @RequestParam long ms) {
        if (phase < 1 || phase > 4) {
            throw new IllegalArgumentException("Phase must be between 1 and 4");
        }
        
        workerSettings.setSleepMs(phase, ms);
        log.info("Set sleep time for phase {} to {}ms", phase, ms);
        
        return Map.of("phase", phase, "sleepMs", ms, "status", "updated");
    }

    /**
     * Pause a specific listener
     */
    @PostMapping("/pause/{listenerId}")
    public Map<String, Object> pauseListener(@PathVariable String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            return Map.of("listenerId", listenerId, "status", "not found");
        }
        
        container.pause();
        log.info("Paused listener {}", listenerId);
        
        return Map.of("listenerId", listenerId, "status", "paused");
    }

    /**
     * Resume a specific listener
     */
    @PostMapping("/resume/{listenerId}")
    public Map<String, Object> resumeListener(@PathVariable String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            return Map.of("listenerId", listenerId, "status", "not found");
        }
        
        container.resume();
        log.info("Resumed listener {}", listenerId);
        
        return Map.of("listenerId", listenerId, "status", "resumed");
    }

    /**
     * Get listener assignments
     */
    @GetMapping("/assignments/{listenerId}")
    public Map<String, Object> getAssignments(@PathVariable String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            return Map.of("listenerId", listenerId, "status", "not found");
        }
        
        return Map.of(
            "listenerId", listenerId,
            "assignments", container.getAssignedPartitions() != null ? container.getAssignedPartitions() : Set.of(),
            "running", container.isRunning(),
            "paused", container.isPauseRequested()
        );
    }

    /**
     * Get current worker settings
     */
    @GetMapping("/settings")
    public Map<String, Object> getSettings() {
        return Map.of(
            "failRates", workerSettings.getAllFailRates(),
            "sleepMs", workerSettings.getAllSleepMs()
        );
    }

    /**
     * Reset all settings to defaults
     */
    @PostMapping("/reset")
    public Map<String, Object> resetSettings() {
        workerSettings.reset();
        log.info("Reset all worker settings to defaults");
        
        return Map.of("status", "reset", "message", "All settings reset to defaults");
    }

    /**
     * Get listener status for all phases
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = Map.of(
            "P1", getListenerStatus("P1"),
            "P2", getListenerStatus("P2"),
            "P3", getListenerStatus("P3"),
            "P4", getListenerStatus("P4")
        );
        
        return Map.of("listeners", status, "settings", getSettings());
    }

    private Map<String, Object> getListenerStatus(String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            return Map.of("status", "not found");
        }
        
        return Map.of(
            "running", container.isRunning(),
            "paused", container.isPauseRequested(),
            "assignments", container.getAssignedPartitions() != null ? container.getAssignedPartitions() : Set.of()
        );
    }
}
