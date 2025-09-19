package com.example.user.kafka;

import com.example.kafka.dto.AssignmentsResponse;
import com.example.kafka.dto.ListenerActionResponse;
import com.example.kafka.dto.ListenerStatusesResponse;
import com.example.kafka.dto.SimpleStatusResponse;
import com.example.kafka.dto.WorkerSettingsRequest;
import com.example.kafka.dto.WorkerSettingsResponse;
import com.example.kafka.service.WorkerSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * REST Controller for Worker control and monitoring
 * Provides endpoints to control worker behavior and monitor status
 */
@RestController
@RequestMapping("/control")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Worker Control", description = "Control endpoints for Worker service")
public class WorkerControlController {

    private final WorkerSettings workerSettings;
    private final KafkaListenerEndpointRegistry registry;

    /**
     * Set fail rate for a specific phase
     */
    @PostMapping("/fail-rate")
    @Operation(summary = "Set fail rate of a phase", responses = {
            @ApiResponse(responseCode = "200", description = "Updated", content = @Content(schema = @Schema(implementation = SimpleStatusResponse.class)))
    })
    public SimpleStatusResponse setFailRate(@RequestParam int phase, @RequestParam double rate) {
        if (phase < 1 || phase > 4) {
            throw new IllegalArgumentException("Phase must be between 1 and 4");
        }

        workerSettings.setFailRate(phase, rate);
        log.info("Set fail rate for phase {} to {}", phase, rate);

        return new SimpleStatusResponse("updated", "phase=" + phase + ", failRate=" + rate);
    }

    /**
     * Set sleep time for a specific phase
     */
    @PostMapping("/sleep")
    @Operation(summary = "Set sleep (ms) of a phase", responses = {
            @ApiResponse(responseCode = "200", description = "Updated", content = @Content(schema = @Schema(implementation = SimpleStatusResponse.class)))
    })
    public SimpleStatusResponse setSleep(@RequestParam int phase, @RequestParam long ms) {
        if (phase < 1 || phase > 4) {
            throw new IllegalArgumentException("Phase must be between 1 and 4");
        }

        workerSettings.setSleepMs(phase, ms);
        log.info("Set sleep time for phase {} to {}ms", phase, ms);

        return new SimpleStatusResponse("updated", "phase=" + phase + ", sleepMs=" + ms);
    }

    /**
     * Pause a specific listener
     */
    @PostMapping("/pause/{listenerId}")
    @Operation(summary = "Pause a listener", responses = {
            @ApiResponse(responseCode = "200", description = "Paused", content = @Content(schema = @Schema(implementation = ListenerActionResponse.class)))
    })
    public ListenerActionResponse pauseListener(@PathVariable String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            return new ListenerActionResponse(listenerId, "not found");
        }

        container.pause();
        log.info("Paused listener {}", listenerId);

        return new ListenerActionResponse(listenerId, "paused");
    }

    /**
     * Resume a specific listener
     */
    @PostMapping("/resume/{listenerId}")
    @Operation(summary = "Resume a listener", responses = {
            @ApiResponse(responseCode = "200", description = "Resumed", content = @Content(schema = @Schema(implementation = ListenerActionResponse.class)))
    })
    public ListenerActionResponse resumeListener(@PathVariable String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            return new ListenerActionResponse(listenerId, "not found");
        }

        container.resume();
        log.info("Resumed listener {}", listenerId);

        return new ListenerActionResponse(listenerId, "resumed");
    }

    /**
     * Get listener assignments
     */
    @GetMapping("/assignments/{listenerId}")
    @Operation(summary = "Get listener assignments and state", responses = {
            @ApiResponse(responseCode = "200", description = "Assignments", content = @Content(schema = @Schema(implementation = AssignmentsResponse.class)))
    })
    public AssignmentsResponse getAssignments(@PathVariable String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            return new AssignmentsResponse(listenerId, java.util.List.of(), false, false);
        }

        java.util.List<String> assignmentStrings = java.util.Collections.emptyList();
        if (container.getAssignedPartitions() != null) {
            assignmentStrings = container.getAssignedPartitions().stream()
                    .map(tp -> tp.topic() + ":" + tp.partition())
                    .toList();
        }

        return new AssignmentsResponse(
                listenerId,
                assignmentStrings,
                container.isRunning(),
                container.isPauseRequested());
    }

    /**
     * Get current worker settings
     */
    @GetMapping("/settings")
    @Operation(summary = "Get current worker settings", responses = {
            @ApiResponse(responseCode = "200", description = "Settings", content = @Content(schema = @Schema(implementation = WorkerSettingsResponse.class)))
    })
    public WorkerSettingsResponse getSettings() {
        MessageListenerContainer container = registry.getListenerContainer("P1");

        boolean isRunning = container != null && container.isRunning();
        boolean isPaused = container != null && container.isPauseRequested();

        int assignmentsCount = 0;
        if (container != null) {
            Collection<TopicPartition> assigned = container.getAssignedPartitions();
            if (assigned != null) {
                assignmentsCount = assigned.size();
            }
        }

        Map<String, Object> containerStatus = Map.of(
                "running", isRunning,
                "paused", isPaused,
                "assignments", assignmentsCount);

        return new WorkerSettingsResponse(
                (int) workerSettings.getSleepMs(1),
                (int) workerSettings.getSleepMs(2),
                (int) workerSettings.getSleepMs(3),
                (int) workerSettings.getSleepMs(4),
                workerSettings.getFailRate(1),
                workerSettings.getFailRate(2),
                workerSettings.getFailRate(3),
                workerSettings.getFailRate(4),
                containerStatus);
    }

    /**
     * Update worker settings using DTO
     */
    @PostMapping("/settings")
    @Operation(summary = "Update worker settings", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = WorkerSettingsRequest.class))), responses = {
            @ApiResponse(responseCode = "200", description = "Updated", content = @Content(schema = @Schema(implementation = WorkerSettingsResponse.class)))
    })
    public WorkerSettingsResponse updateSettings(
            @org.springframework.web.bind.annotation.RequestBody WorkerSettingsRequest request) {
        if (request.phase1SleepMs() != null) {
            workerSettings.setSleepMs(1, request.phase1SleepMs());
        }
        if (request.phase2SleepMs() != null) {
            workerSettings.setSleepMs(2, request.phase2SleepMs());
        }
        if (request.phase3SleepMs() != null) {
            workerSettings.setSleepMs(3, request.phase3SleepMs());
        }
        if (request.phase4SleepMs() != null) {
            workerSettings.setSleepMs(4, request.phase4SleepMs());
        }

        if (request.phase1FailRate() != null) {
            workerSettings.setFailRate(1, request.phase1FailRate());
        }
        if (request.phase2FailRate() != null) {
            workerSettings.setFailRate(2, request.phase2FailRate());
        }
        if (request.phase3FailRate() != null) {
            workerSettings.setFailRate(3, request.phase3FailRate());
        }
        if (request.phase4FailRate() != null) {
            workerSettings.setFailRate(4, request.phase4FailRate());
        }

        log.info("Updated worker settings: {}", request);
        return getSettings();
    }

    /**
     * Reset all settings to defaults
     */
    @PostMapping("/reset")
    @Operation(summary = "Reset settings to defaults", responses = {
            @ApiResponse(responseCode = "200", description = "Reset", content = @Content(schema = @Schema(implementation = SimpleStatusResponse.class)))
    })
    public SimpleStatusResponse resetSettings() {
        workerSettings.reset();
        log.info("Reset all worker settings to defaults");

        return new SimpleStatusResponse("reset", "All settings reset to defaults");
    }

    /**
     * Get listener status for all phases
     */
    @GetMapping("/status")
    @Operation(summary = "Get listeners status and current settings", responses = {
            @ApiResponse(responseCode = "200", description = "Statuses", content = @Content(schema = @Schema(implementation = ListenerStatusesResponse.class)))
    })
    public ListenerStatusesResponse getStatus() {
        Map<String, Object> p1 = getListenerStatus("P1");
        Map<String, Object> p2 = getListenerStatus("P2");
        Map<String, Object> p3 = getListenerStatus("P3");
        Map<String, Object> p4 = getListenerStatus("P4");
        Map<String, java.util.Map<String, Object>> listeners = Map.of(
                "P1", (java.util.Map<String, Object>) p1,
                "P2", (java.util.Map<String, Object>) p2,
                "P3", (java.util.Map<String, Object>) p3,
                "P4", (java.util.Map<String, Object>) p4);

        return new ListenerStatusesResponse(listeners, getSettings());
    }

    private Map<String, Object> getListenerStatus(String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            return Map.of("status", "not found");
        }

        java.util.List<String> assignmentStrings = java.util.Collections.emptyList();
        if (container.getAssignedPartitions() != null) {
            assignmentStrings = container.getAssignedPartitions().stream()
                    .map(tp -> tp.topic() + ":" + tp.partition())
                    .toList();
        }

        return Map.of(
                "running", container.isRunning(),
                "paused", container.isPauseRequested(),
                "assignments", assignmentStrings);
    }
}
