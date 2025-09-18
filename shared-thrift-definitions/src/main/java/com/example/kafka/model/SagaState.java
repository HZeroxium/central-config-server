package com.example.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Saga state tracking for orchestrator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {
    
    private String sagaId;
    private String status; // RUNNING, DONE, FAILED, TIMEOUT, CANCELLED
    private int currentPhase;
    private Instant startedAt;
    private Instant updatedAt;
    private String lastError;
    private Map<String, Object> metadata;
    
    public static SagaState init(String sagaId) {
        return SagaState.builder()
                .sagaId(sagaId)
                .status("RUNNING")
                .currentPhase(0)
                .startedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
    
    public void updatePhase(int phase) {
        this.currentPhase = phase;
        this.updatedAt = Instant.now();
    }
    
    public void markDone() {
        this.status = "DONE";
        this.updatedAt = Instant.now();
    }
    
    public void markFailed(String error) {
        this.status = "FAILED";
        this.lastError = error;
        this.updatedAt = Instant.now();
    }
    
    public void markCancelled() {
        this.status = "CANCELLED";
        this.updatedAt = Instant.now();
    }
}
