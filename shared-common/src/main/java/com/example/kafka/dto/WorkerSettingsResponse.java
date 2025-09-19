package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response DTO for worker settings
 */
public record WorkerSettingsResponse(
    @JsonProperty("phase1SleepMs") int phase1SleepMs,
    @JsonProperty("phase2SleepMs") int phase2SleepMs,
    @JsonProperty("phase3SleepMs") int phase3SleepMs,
    @JsonProperty("phase4SleepMs") int phase4SleepMs,
    @JsonProperty("phase1FailRate") double phase1FailRate,
    @JsonProperty("phase2FailRate") double phase2FailRate,
    @JsonProperty("phase3FailRate") double phase3FailRate,
    @JsonProperty("phase4FailRate") double phase4FailRate,
    @JsonProperty("containerStatus") Map<String, Object> containerStatus
) {}
