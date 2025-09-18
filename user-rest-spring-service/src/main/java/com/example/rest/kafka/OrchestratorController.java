package com.example.rest.kafka;

import com.example.kafka.constants.KafkaConstants;
import com.example.kafka.dto.PhaseCommand;
import com.example.kafka.dto.SagaListResponse;
import com.example.kafka.dto.SagaStatusResponse;
import com.example.kafka.dto.StartSagaRequest;
import com.example.kafka.dto.TriggerPhaseResponse;
import com.example.kafka.model.SagaEvent;
import com.example.kafka.service.SagaStateRepository;
import com.example.kafka.util.KafkaHeadersUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
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
@Tag(name = "User Update Saga", description = "Orchestrator endpoints for User Update Saga")
public class OrchestratorController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SagaStateRepository sagaStateRepository;
    private final ObjectMapper objectMapper;

    /**
     * Start a new user update saga
     */
    @PostMapping("/start")
    @Operation(
        summary = "Start a new saga",
        requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = StartSagaRequest.class))),
        responses = {
            @ApiResponse(responseCode = "202", description = "Saga started", content = @Content(schema = @Schema(implementation = SagaStatusResponse.class))),
            @ApiResponse(responseCode = "500", description = "Server error")
        }
    )
    public ResponseEntity<SagaStatusResponse> start(@org.springframework.web.bind.annotation.RequestBody StartSagaRequest request) {
        try {
            String sagaId = UUID.randomUUID().toString();
            String correlationId = request.correlationId() != null ? request.correlationId() : sagaId;

            // Initialize saga state
            sagaStateRepository.init(sagaId);
            log.info("Starting saga {} for user {}", sagaId, request.userId());

            // Create phase 1 command using DTO
            PhaseCommand command = new PhaseCommand(
                sagaId,
                "phase1",
                KafkaConstants.COMMAND_TYPE_PHASE_1_COMMAND,
                Map.of("user", Map.of(
                    "id", request.userId(),
                    "name", request.userName(),
                    "email", request.userEmail(),
                    "phone", request.userPhone(),
                    "address", request.userAddress()
                ), "expectedVersion", 1),
                correlationId,
                correlationId,
                null, // eventId
                KafkaConstants.SOURCE_ORCHESTRATOR,
                System.currentTimeMillis()
            );

            // Send command in transaction
            kafkaTemplate.executeInTransaction(kt -> {
                try {
                    var record = new ProducerRecord<>(
                        KafkaConstants.TOPIC_USER_UPDATE_PHASE_1_COMMAND, sagaId, objectMapper.writeValueAsString(command));
                    
                    KafkaHeadersUtil.addStandardHeaders(
                        record, command.sagaId(), command.correlationId(), command.causationId(), 
                        command.eventId(), command.source(), command.commandType());
                    
                    kt.send(record);
                    return true;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize command", e);
                }
            });

            log.info("Phase 1 command sent for saga {}", sagaId);
            
            // Return response using DTO
            SagaStatusResponse response = new SagaStatusResponse(
                sagaId,
                KafkaConstants.SAGA_STATE_STARTED,
                "phase1",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                List.of(),
                correlationId
            );
            
            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to start saga", e);
            SagaStatusResponse errorResponse = new SagaStatusResponse(
                null,
                "ERROR",
                null,
                null,
                null,
                null,
                null,
                "Failed to start saga: " + e.getMessage(),
                List.of(),
                null
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get saga status
     */
    @GetMapping("/{sagaId}")
    @Operation(summary = "Get saga status", responses = {
        @ApiResponse(responseCode = "200", description = "Saga status", content = @Content(schema = @Schema(implementation = SagaStatusResponse.class))),
        @ApiResponse(responseCode = "404", description = "Saga not found")
    })
    public ResponseEntity<SagaStatusResponse> getSagaStatus(@PathVariable String sagaId) {
        var state = sagaStateRepository.get(sagaId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        
        SagaStatusResponse response = new SagaStatusResponse(
            state.getSagaId(),
            state.getStatus(),
            String.valueOf(state.getCurrentPhase()),
            state.getStartedAt() != null ? state.getStartedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
            state.getUpdatedAt() != null ? state.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
            null, // completedAt
            null, // failedAt
            state.getLastError(),
            List.of(), // phaseHistory
            null // correlationId
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a saga
     */
    @PostMapping("/{sagaId}/cancel")
    @Operation(summary = "Cancel saga", responses = {
        @ApiResponse(responseCode = "200", description = "Cancelled", content = @Content(schema = @Schema(implementation = SagaStatusResponse.class)))
    })
    public ResponseEntity<SagaStatusResponse> cancelSaga(@PathVariable String sagaId) {
        sagaStateRepository.update(sagaId, "CANCELLED", "manual cancel", null);
        log.info("Saga {} cancelled manually", sagaId);
        return getSagaStatus(sagaId);
    }

    /**
     * Get all sagas (for monitoring)
     */
    @GetMapping
    @Operation(summary = "List sagas", responses = {
        @ApiResponse(responseCode = "200", description = "List of sagas", content = @Content(schema = @Schema(implementation = SagaListResponse.class)))
    })
    public ResponseEntity<SagaListResponse> getAllSagas() {
        return ResponseEntity.ok(new SagaListResponse(sagaStateRepository.getAll()));
    }

    /**
     * Manual trigger for a specific phase (for testing)
     */
    @PostMapping("/{sagaId}/trigger/{phase}")
    @Operation(summary = "Trigger a specific phase", responses = {
        @ApiResponse(responseCode = "200", description = "Triggered", content = @Content(schema = @Schema(implementation = TriggerPhaseResponse.class)))
    })
    public ResponseEntity<TriggerPhaseResponse> triggerPhase(
            @PathVariable String sagaId, 
            @PathVariable int phase) {
        try {
            if (phase < 1 || phase > 4) {
                return ResponseEntity.badRequest()
                    .body(new TriggerPhaseResponse(sagaId, phase, "INVALID_PHASE"));
            }

            String commandType = "UserUpdatePhase" + phase + "Command";
            SagaEvent command = SagaEvent.command(commandType, sagaId, sagaId, Map.of());

            kafkaTemplate.executeInTransaction(kt -> {
                try {
                    var record = new ProducerRecord<>(
                        "user.update.phase_" + phase + ".command", sagaId, objectMapper.writeValueAsString(command));
                    
                    KafkaHeadersUtil.addStandardHeaders(
                        record, sagaId, sagaId, command.getCausationId(), 
                        command.getEventId(), "thrift-client-service", command.getType());
                    
                    kt.send(record);
                    return true;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize command", e);
                }
            });

            log.info("Manual trigger sent for saga {} phase {}", sagaId, phase);
            return ResponseEntity.ok(new TriggerPhaseResponse(sagaId, phase, "TRIGGERED"));

        } catch (Exception e) {
            log.error("Failed to trigger phase {} for saga {}", phase, sagaId, e);
            return ResponseEntity.internalServerError()
                .body(new TriggerPhaseResponse(sagaId, phase, "ERROR"));
        }
    }
}
