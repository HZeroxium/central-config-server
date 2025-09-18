package com.example.kafka.constants;

/**
 * Centralized constants for Kafka configuration and topics
 */
public final class KafkaConstants {
    
    // Topic names
    public static final String TOPIC_USER_UPDATE_PHASE_1_COMMAND = "user.update.phase_1.command";
    public static final String TOPIC_USER_UPDATE_PHASE_2_COMMAND = "user.update.phase_2.command";
    public static final String TOPIC_USER_UPDATE_PHASE_3_COMMAND = "user.update.phase_3.command";
    public static final String TOPIC_USER_UPDATE_PHASE_4_COMMAND = "user.update.phase_4.command";
    
    public static final String TOPIC_USER_UPDATE_PHASE_1_EVENT = "user.update.phase_1.event";
    public static final String TOPIC_USER_UPDATE_PHASE_2_EVENT = "user.update.phase_2.event";
    public static final String TOPIC_USER_UPDATE_PHASE_3_EVENT = "user.update.phase_3.event";
    public static final String TOPIC_USER_UPDATE_PHASE_4_EVENT = "user.update.phase_4.event";
    
    public static final String TOPIC_USER_UPDATE_PHASE_1_COMMAND_DLT = "user.update.phase_1.command.DLT";
    public static final String TOPIC_USER_UPDATE_PHASE_2_COMMAND_DLT = "user.update.phase_2.command.DLT";
    public static final String TOPIC_USER_UPDATE_PHASE_3_COMMAND_DLT = "user.update.phase_3.command.DLT";
    public static final String TOPIC_USER_UPDATE_PHASE_4_COMMAND_DLT = "user.update.phase_4.command.DLT";
    
    // Consumer groups
    public static final String GROUP_USER_UPDATE_WORKERS = "user-update-workers";
    public static final String GROUP_USER_UPDATE_ORCHESTRATOR = "user-update-orchestrator";
    public static final String GROUP_USER_UPDATE_DLT = "user-update-dlt";
    
    // Container factories
    public static final String CONTAINER_FACTORY_TX = "txFactory";
    public static final String CONTAINER_FACTORY_BATCH = "batchFactory";
    
    // Concurrency settings
    public static final int CONCURRENCY_PHASE_1 = 3;
    public static final int CONCURRENCY_PHASE_2 = 3;
    public static final int CONCURRENCY_PHASE_3 = 3;
    public static final int CONCURRENCY_PHASE_4 = 3;
    
    // Transaction prefixes
    public static final String TX_PREFIX_WORKER = "worker-tx-";
    public static final String TX_PREFIX_ORCHESTRATOR = "orchestrator-tx-";
    
    // Header names
    public static final String HEADER_SAGA_ID = "sagaId";
    public static final String HEADER_CORRELATION_ID = "correlationId";
    public static final String HEADER_CAUSATION_ID = "causationId";
    public static final String HEADER_EVENT_ID = "eventId";
    public static final String HEADER_SOURCE = "source";
    public static final String HEADER_TYPE = "type";
    
    // Event types
    public static final String EVENT_TYPE_PHASE_1_STARTED = "Phase1Started";
    public static final String EVENT_TYPE_PHASE_1_COMPLETED = "Phase1Completed";
    public static final String EVENT_TYPE_PHASE_1_FAILED = "Phase1Failed";
    
    public static final String EVENT_TYPE_PHASE_2_STARTED = "Phase2Started";
    public static final String EVENT_TYPE_PHASE_2_COMPLETED = "Phase2Completed";
    public static final String EVENT_TYPE_PHASE_2_FAILED = "Phase2Failed";
    
    public static final String EVENT_TYPE_PHASE_3_STARTED = "Phase3Started";
    public static final String EVENT_TYPE_PHASE_3_COMPLETED = "Phase3Completed";
    public static final String EVENT_TYPE_PHASE_3_FAILED = "Phase3Failed";
    
    public static final String EVENT_TYPE_PHASE_4_STARTED = "Phase4Started";
    public static final String EVENT_TYPE_PHASE_4_COMPLETED = "Phase4Completed";
    public static final String EVENT_TYPE_PHASE_4_FAILED = "Phase4Failed";
    
    // Command types
    public static final String COMMAND_TYPE_START_SAGA = "StartSaga";
    public static final String COMMAND_TYPE_PHASE_1_COMMAND = "Phase1Command";
    public static final String COMMAND_TYPE_PHASE_2_COMMAND = "Phase2Command";
    public static final String COMMAND_TYPE_PHASE_3_COMMAND = "Phase3Command";
    public static final String COMMAND_TYPE_PHASE_4_COMMAND = "Phase4Command";
    
    // Sources
    public static final String SOURCE_ORCHESTRATOR = "orchestrator";
    public static final String SOURCE_WORKER = "worker";
    
    // Saga states
    public static final String SAGA_STATE_STARTED = "STARTED";
    public static final String SAGA_STATE_PHASE_1_IN_PROGRESS = "PHASE_1_IN_PROGRESS";
    public static final String SAGA_STATE_PHASE_1_COMPLETED = "PHASE_1_COMPLETED";
    public static final String SAGA_STATE_PHASE_2_IN_PROGRESS = "PHASE_2_IN_PROGRESS";
    public static final String SAGA_STATE_PHASE_2_COMPLETED = "PHASE_2_COMPLETED";
    public static final String SAGA_STATE_PHASE_3_IN_PROGRESS = "PHASE_3_IN_PROGRESS";
    public static final String SAGA_STATE_PHASE_3_COMPLETED = "PHASE_3_COMPLETED";
    public static final String SAGA_STATE_PHASE_4_IN_PROGRESS = "PHASE_4_IN_PROGRESS";
    public static final String SAGA_STATE_PHASE_4_COMPLETED = "PHASE_4_COMPLETED";
    public static final String SAGA_STATE_COMPLETED = "COMPLETED";
    public static final String SAGA_STATE_FAILED = "FAILED";
    public static final String SAGA_STATE_COMPENSATING = "COMPENSATING";
    
    // Error messages
    public static final String ERROR_MSG_PHASE_1_FAILED = "Phase 1 processing failed";
    public static final String ERROR_MSG_PHASE_2_FAILED = "Phase 2 processing failed";
    public static final String ERROR_MSG_PHASE_3_FAILED = "Phase 3 processing failed";
    public static final String ERROR_MSG_PHASE_4_FAILED = "Phase 4 processing failed";
    
    // Default values
    public static final int DEFAULT_SLEEP_MS = 2000;
    public static final double DEFAULT_FAIL_RATE = 0.1;
    
    private KafkaConstants() {
        // Utility class
    }
}
