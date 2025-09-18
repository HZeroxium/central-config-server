package com.example.kafka.service;

import com.example.kafka.model.SagaState;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for Saga state tracking
 * In production, this would be backed by a database
 */
@Component
public class SagaStateRepository {
    
    private final Map<String, SagaState> sagaStates = new ConcurrentHashMap<>();
    
    public void init(String sagaId) {
        sagaStates.put(sagaId, SagaState.init(sagaId));
    }
    
    public SagaState get(String sagaId) {
        return sagaStates.get(sagaId);
    }
    
    public void update(String sagaId, String status, String error, Map<String, Object> metadata) {
        SagaState state = sagaStates.get(sagaId);
        if (state != null) {
            if ("FAILED".equals(status)) {
                state.markFailed(error);
            } else if ("CANCELLED".equals(status)) {
                state.markCancelled();
            } else if ("DONE".equals(status)) {
                state.markDone();
            } else {
                state.setStatus(status);
                state.setUpdatedAt(java.time.Instant.now());
            }
            
            if (metadata != null) {
                state.setMetadata(metadata);
            }
        }
    }
    
    public void updatePhase(String sagaId, int phase) {
        SagaState state = sagaStates.get(sagaId);
        if (state != null) {
            state.updatePhase(phase);
        }
    }
    
    public Map<String, SagaState> getAll() {
        return Map.copyOf(sagaStates);
    }
}
