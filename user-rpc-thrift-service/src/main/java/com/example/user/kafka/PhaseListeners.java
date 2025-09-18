package com.example.user.kafka;

import com.example.kafka.model.SagaEvent;
import com.example.kafka.service.WorkerSettings;
import com.example.kafka.util.KafkaHeadersUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka Listeners for Worker service
 * Handles commands from orchestrator and produces events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PhaseListeners {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WorkerSettings workerSettings;
    private final ObjectMapper objectMapper;
    
    // Simple in-memory deduplication for learning purposes
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    /**
     * Phase 1 Worker: Validation
     */
    @KafkaListener(id = "P1", 
                   topics = "user.update.phase_1.command", 
                   groupId = "user-update-workers", 
                   containerFactory = "txFactory", 
                   concurrency = "3")
    public void onPhase1Command(@org.springframework.messaging.handler.annotation.Payload String command,
                               @org.springframework.messaging.handler.annotation.Header("sagaId") String sagaId,
                               @org.springframework.messaging.handler.annotation.Header(value = "eventId", required = false) byte[] eventId) {
        try {
            String eventIdStr = eventId != null ? new String(eventId, StandardCharsets.UTF_8) : null;
            
            // Idempotency check
            if (eventIdStr != null && !processedEventIds.add(eventIdStr)) {
                log.info("[P1] Duplicate command ignored for saga {} eventId {}", sagaId, eventIdStr);
                return;
            }
            
            log.info("[P1] Processing validation for saga {}", sagaId);
            
            // Simulate processing time
            maybeSleep(workerSettings.getSleepMs(1));
            
            // Simulate random failure
            if (randomFail(workerSettings.getFailRate(1))) {
                log.warn("[P1] Simulated failure for saga {}", sagaId);
                throw new RuntimeException("P1 validation failed");
            }
            
            // Send success event
            sendPhaseEvent("user.update.phase_1.event", sagaId, "UserUpdatePhase1Done", 
                          Map.of("phase", 1, "validation", "passed"));
            
            log.info("[P1] Validation completed for saga {}", sagaId);
            
        } catch (Exception e) {
            log.error("[P1] Error processing command for saga {}", sagaId, e);
            throw e; // Let error handler deal with retry/DLT
        }
    }

    /**
     * Phase 2 Worker: Enrichment
     */
    @KafkaListener(id = "P2", 
                   topics = "user.update.phase_2.command", 
                   groupId = "user-update-workers", 
                   containerFactory = "txFactory", 
                   concurrency = "3")
    public void onPhase2Command(@org.springframework.messaging.handler.annotation.Payload String command,
                               @org.springframework.messaging.handler.annotation.Header("sagaId") String sagaId) {
        try {
            log.info("[P2] Processing enrichment for saga {}", sagaId);
            
            maybeSleep(workerSettings.getSleepMs(2));
            
            if (randomFail(workerSettings.getFailRate(2))) {
                log.warn("[P2] Simulated failure for saga {}", sagaId);
                throw new RuntimeException("P2 enrichment failed");
            }
            
            sendPhaseEvent("user.update.phase_2.event", sagaId, "UserUpdatePhase2Done", 
                          Map.of("phase", 2, "enrichment", "completed"));
            
            log.info("[P2] Enrichment completed for saga {}", sagaId);
            
        } catch (Exception e) {
            log.error("[P2] Error processing command for saga {}", sagaId, e);
            throw e;
        }
    }

    /**
     * Phase 3 Worker: Persistence
     */
    @KafkaListener(id = "P3", 
                   topics = "user.update.phase_3.command", 
                   groupId = "user-update-workers", 
                   containerFactory = "txFactory", 
                   concurrency = "3")
    public void onPhase3Command(@org.springframework.messaging.handler.annotation.Payload String command,
                               @org.springframework.messaging.handler.annotation.Header("sagaId") String sagaId) {
        try {
            log.info("[P3] Processing persistence for saga {}", sagaId);
            
            maybeSleep(workerSettings.getSleepMs(3));
            
            if (randomFail(workerSettings.getFailRate(3))) {
                log.warn("[P3] Simulated failure for saga {}", sagaId);
                throw new RuntimeException("P3 persistence failed");
            }
            
            sendPhaseEvent("user.update.phase_3.event", sagaId, "UserUpdatePhase3Done", 
                          Map.of("phase", 3, "persistence", "completed"));
            
            log.info("[P3] Persistence completed for saga {}", sagaId);
            
        } catch (Exception e) {
            log.error("[P3] Error processing command for saga {}", sagaId, e);
            throw e;
        }
    }

    /**
     * Phase 4 Worker: Notification
     */
    @KafkaListener(id = "P4", 
                   topics = "user.update.phase_4.command", 
                   groupId = "user-update-workers", 
                   containerFactory = "txFactory", 
                   concurrency = "3")
    public void onPhase4Command(@org.springframework.messaging.handler.annotation.Payload String command,
                               @org.springframework.messaging.handler.annotation.Header("sagaId") String sagaId) {
        try {
            log.info("[P4] Processing notification for saga {}", sagaId);
            
            maybeSleep(workerSettings.getSleepMs(4));
            
            if (randomFail(workerSettings.getFailRate(4))) {
                log.warn("[P4] Simulated failure for saga {}", sagaId);
                throw new RuntimeException("P4 notification failed");
            }
            
            sendPhaseEvent("user.update.phase_4.event", sagaId, "UserUpdatePhase4Done", 
                          Map.of("phase", 4, "notification", "sent"));
            
            log.info("[P4] Notification completed for saga {}", sagaId);
            
        } catch (Exception e) {
            log.error("[P4] Error processing command for saga {}", sagaId, e);
            throw e;
        }
    }

    /**
     * DLT Listener for monitoring failed messages
     */
    @KafkaListener(topics = {
        "user.update.phase_1.command.DLT",
        "user.update.phase_2.command.DLT", 
        "user.update.phase_3.command.DLT",
        "user.update.phase_4.command.DLT"
    }, groupId = "user-update-dlt")
    public void onDltMessage(@org.springframework.messaging.handler.annotation.Payload String message,
                           @org.springframework.messaging.handler.annotation.Header(org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("[DLT] Received failed message on topic {}: {}", topic, message);
    }

    /**
     * Send phase completion event
     */
    private void sendPhaseEvent(String topic, String sagaId, String eventType, Map<String, Object> result) {
        try {
            SagaEvent event = SagaEvent.event(eventType, sagaId, "thrift-server-service", result);
            
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, sagaId, objectMapper.writeValueAsString(event));
            KafkaHeadersUtil.addStandardHeaders(record, sagaId, "thrift-server-service", eventType);
            
            kafkaTemplate.send(record);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {} for saga {}", eventType, sagaId, e);
            throw new RuntimeException("Failed to serialize event", e);
        } catch (Exception e) {
            log.error("Failed to send event {} for saga {}", eventType, sagaId, e);
            throw new RuntimeException("Failed to send event", e);
        }
    }

    /**
     * Simulate processing time
     */
    private void maybeSleep(long ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Simulate random failure
     */
    private boolean randomFail(double rate) {
        return Math.random() < rate;
    }
}
