package com.example.rest.kafka;

import com.example.kafka.model.SagaEvent;
import com.example.kafka.service.SagaStateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Saga Orchestration
 * Provides endpoints to start, monitor, and control user update sagas
 */
@RestController
@RequestMapping("/saga/user-update")
@RequiredArgsConstructor
@Slf4j
public class OrchestratorController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SagaStateRepository sagaStateRepository;
    private final ObjectMapper objectMapper;

    /**
     * Start a new user update saga
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody Map<String, Object> body) {
        try {
            String sagaId = (String) body.getOrDefault("sagaId", UUID.randomUUID().toString());
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) body.get("user");

            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "User data is required"));
            }

            // Initialize saga state
            sagaStateRepository.init(sagaId);
            log.info("Starting saga {} for user {}", sagaId, user.get("id"));

            // Create phase 1 command
            SagaEvent command = SagaEvent.command(
                "UserUpdatePhase1Command",
                sagaId,
                sagaId,
                Map.of("user", user, "expectedVersion", 1)
            );

            // Send command in transaction
            kafkaTemplate.executeInTransaction(kt -> {
                try {
                    var record = new org.apache.kafka.clients.producer.ProducerRecord<>(
                        "user.update.phase_1.command", sagaId, objectMapper.writeValueAsString(command));
                    
                    com.example.kafka.util.KafkaHeadersUtil.addStandardHeaders(
                        record, sagaId, sagaId, command.getCausationId(), 
                        command.getEventId(), "thrift-client-service", command.getType());
                    
                    kt.send(record);
                    return true;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize command", e);
                }
            });

            log.info("Phase 1 command sent for saga {}", sagaId);
            return ResponseEntity.accepted()
                .body(Map.of("sagaId", sagaId, "status", "STARTED", "phase", 1));

        } catch (Exception e) {
            log.error("Failed to start saga", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start saga: " + e.getMessage()));
        }
    }

    /**
     * Get saga status
     */
    @GetMapping("/{sagaId}")
    public ResponseEntity<Map<String, Object>> getSagaStatus(@PathVariable String sagaId) {
        var state = sagaStateRepository.get(sagaId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "sagaId", state.getSagaId(),
            "status", state.getStatus(),
            "currentPhase", state.getCurrentPhase(),
            "startedAt", state.getStartedAt(),
            "updatedAt", state.getUpdatedAt(),
            "lastError", state.getLastError(),
            "metadata", state.getMetadata() != null ? state.getMetadata() : Map.of()
        ));
    }

    /**
     * Cancel a saga
     */
    @PostMapping("/{sagaId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSaga(@PathVariable String sagaId) {
        sagaStateRepository.update(sagaId, "CANCELLED", "manual cancel", null);
        log.info("Saga {} cancelled manually", sagaId);
        return getSagaStatus(sagaId);
    }

    /**
     * Get all sagas (for monitoring)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSagas() {
        return ResponseEntity.ok(Map.of("sagas", sagaStateRepository.getAll()));
    }

    /**
     * Manual trigger for a specific phase (for testing)
     */
    @PostMapping("/{sagaId}/trigger/{phase}")
    public ResponseEntity<Map<String, Object>> triggerPhase(
            @PathVariable String sagaId, 
            @PathVariable int phase) {
        try {
            if (phase < 1 || phase > 4) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Phase must be between 1 and 4"));
            }

            String commandType = "UserUpdatePhase" + phase + "Command";
            SagaEvent command = SagaEvent.command(commandType, sagaId, sagaId, Map.of());

            kafkaTemplate.executeInTransaction(kt -> {
                try {
                    var record = new org.apache.kafka.clients.producer.ProducerRecord<>(
                        "user.update.phase_" + phase + ".command", sagaId, objectMapper.writeValueAsString(command));
                    
                    com.example.kafka.util.KafkaHeadersUtil.addStandardHeaders(
                        record, sagaId, sagaId, command.getCausationId(), 
                        command.getEventId(), "thrift-client-service", command.getType());
                    
                    kt.send(record);
                    return true;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize command", e);
                }
            });

            log.info("Manual trigger sent for saga {} phase {}", sagaId, phase);
            return ResponseEntity.ok(Map.of("sagaId", sagaId, "phase", phase, "status", "TRIGGERED"));

        } catch (Exception e) {
            log.error("Failed to trigger phase {} for saga {}", phase, sagaId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to trigger phase: " + e.getMessage()));
        }
    }
}
