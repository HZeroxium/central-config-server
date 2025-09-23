package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for updating worker settings
 */
public record WorkerSettingsRequest(
    @JsonProperty("phase1SleepMs") @Min(0) Integer phase1SleepMs,
    @JsonProperty("phase2SleepMs") @Min(0) Integer phase2SleepMs,
    @JsonProperty("phase3SleepMs") @Min(0) Integer phase3SleepMs,
    @JsonProperty("phase4SleepMs") @Min(0) Integer phase4SleepMs,
    @JsonProperty("phase1FailRate") @DecimalMin("0.0") @DecimalMax("1.0") Double phase1FailRate,
    @JsonProperty("phase2FailRate") @DecimalMin("0.0") @DecimalMax("1.0") Double phase2FailRate,
    @JsonProperty("phase3FailRate") @DecimalMin("0.0") @DecimalMax("1.0") Double phase3FailRate,
    @JsonProperty("phase4FailRate") @DecimalMin("0.0") @DecimalMax("1.0") Double phase4FailRate
) {}
