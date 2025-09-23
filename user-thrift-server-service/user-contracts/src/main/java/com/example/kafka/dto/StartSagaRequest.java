package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for starting a new saga
 */
public record StartSagaRequest(
    @JsonProperty("userId") @NotBlank String userId,
    @JsonProperty("userName") @NotBlank String userName,
    @JsonProperty("userEmail") @NotBlank String userEmail,
    @JsonProperty("userPhone") String userPhone,
    @JsonProperty("userAddress") String userAddress,
    @JsonProperty("correlationId") String correlationId
) {
    public StartSagaRequest {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("userName cannot be null or empty");
        }
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("userEmail cannot be null or empty");
        }
    }
}
