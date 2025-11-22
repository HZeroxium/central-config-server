package com.example.control.infrastructure.observability;

/**
 * Centralized metric name constants for config-control-service.
 * <p>
 * This class eliminates magic strings and ensures consistent metric naming
 * across the application.
 * All metric names follow the pattern:
 * {@code config_control.<module>.<operation>} or {@code <module>.<operation>}.
 * </p>
 * <p>
 * <b>Naming conventions:</b>
 * <ul>
 * <li>Use dot notation (e.g., {@code config_control.heartbeat.process})</li>
 * <li>Use lowercase with underscores for readability</li>
 * <li>Keep names stable to avoid breaking Prometheus queries</li>
 * <li>Group by module/domain (heartbeat, cleanup, application_service,
 * etc.)</li>
 * </ul>
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
public final class MetricsNames {

  private MetricsNames() {
    // Utility class - prevent instantiation
  }

  /**
   * Heartbeat processing metrics.
   */
  public static final class Heartbeat {
    private Heartbeat() {
    }

    /**
     * Heartbeat processing operation.
     * <p>
     * Metric name: {@code heartbeat.process}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatService#processHeartbeat}
     */
    public static final String PROCESS = "heartbeat.process";

    /**
     * Heartbeat batch processing operation.
     * <p>
     * Metric name: {@code heartbeat.batch.process}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatBatchService#processBatch}
     */
    public static final String BATCH_PROCESS = "heartbeat.batch.process";

    /**
     * Heartbeat ingestion (enqueue) operation.
     * <p>
     * Metric name: {@code heartbeat.ingestion}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatIngestionService#enqueue}
     */
    public static final String INGESTION = "heartbeat.ingestion";

    /**
     * Heartbeat batch ingestion (enqueue) operation time.
     * <p>
     * Metric name: {@code heartbeat.batch.ingestion.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatIngestionService#enqueue}
     */
    public static final String BATCH_INGESTION_TIME = "heartbeat.batch.ingestion.time";

    /**
     * Kafka batch processing time.
     * <p>
     * Metric name: {@code heartbeat.batch.kafka.process.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatBatchProcessor#processBatch}
     */
    public static final String BATCH_KAFKA_PROCESS_TIME = "heartbeat.batch.kafka.process.time";

    /**
     * Batch load instances time.
     * <p>
     * Metric name: {@code heartbeat.batch.load.instances.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatBatchService#loadInstancesBatch}
     */
    public static final String BATCH_LOAD_INSTANCES_TIME = "heartbeat.batch.load.instances.time";

    /**
     * Batch load application services time.
     * <p>
     * Metric name: {@code heartbeat.batch.load.appservices.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatBatchService#loadApplicationServicesBatch}
     */
    public static final String BATCH_LOAD_APPSERVICES_TIME = "heartbeat.batch.load.appservices.time";

    /**
     * Batch load config hashes time.
     * <p>
     * Metric name: {@code heartbeat.batch.load.config.hashes.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatBatchService#loadConfigHashesBatch}
     */
    public static final String BATCH_LOAD_CONFIG_HASHES_TIME = "heartbeat.batch.load.config.hashes.time";

    /**
     * Batch process heartbeats in memory time.
     * <p>
     * Metric name: {@code heartbeat.batch.process.inmemory.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatBatchService#processBatch}
     */
    public static final String BATCH_PROCESS_INMEMORY_TIME = "heartbeat.batch.process.inmemory.time";

    /**
     * Batch bus refresh time.
     * <p>
     * Metric name: {@code heartbeat.batch.refresh.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatBatchService#triggerBatchBusRefresh}
     */
    public static final String BATCH_REFRESH_TIME = "heartbeat.batch.refresh.time";

    /**
     * MongoDB bulk upsert instances time.
     * <p>
     * Metric name: {@code heartbeat.batch.mongodb.instances.upsert.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.command.ServiceInstanceCommandService#bulkUpsert}
     */
    public static final String BATCH_MONGODB_INSTANCES_UPSERT_TIME = "heartbeat.batch.mongodb.instances.upsert.time";

    /**
     * MongoDB bulk save application services time.
     * <p>
     * Metric name: {@code heartbeat.batch.mongodb.appservices.save.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.command.ApplicationServiceCommandService#bulkSave}
     */
    public static final String BATCH_MONGODB_APPSERVICES_SAVE_TIME = "heartbeat.batch.mongodb.appservices.save.time";

    /**
     * MongoDB bulk save drift events time.
     * <p>
     * Metric name: {@code heartbeat.batch.mongodb.drift.save.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.command.DriftEventCommandService#bulkSave}
     */
    public static final String BATCH_MONGODB_DRIFT_SAVE_TIME = "heartbeat.batch.mongodb.drift.save.time";

    /**
     * Batch drift event resolution time.
     * <p>
     * Metric name: {@code heartbeat.batch.drift.resolve.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.DriftEventService#resolveForInstance}
     */
    public static final String BATCH_DRIFT_RESOLVE_TIME = "heartbeat.batch.drift.resolve.time";

    /**
     * DLQ consumer operation.
     * <p>
     * Metric name: {@code heartbeat.dlq.consume}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatDlqConsumer#processDlqBatch}
     */
    public static final String DLQ_CONSUME = "heartbeat.dlq.consume";

    /**
     * DLQ consumer processing time.
     * <p>
     * Metric name: {@code heartbeat.dlq.consume.time}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.HeartbeatDlqConsumer#processDlqBatch}
     */
    public static final String DLQ_CONSUME_TIME = "heartbeat.dlq.consume.time";
  }

  /**
   * Thrift RPC metrics.
   */
  public static final class Thrift {
    private Thrift() {
    }

    /**
     * Thrift heartbeat handler operation.
     * <p>
     * Metric name: {@code config_control.thrift.heartbeat}
     * <p>
     * Used in:
     * {@link com.example.control.api.rpc.thrift.ThriftHeartbeatHandler#recordHeartbeat}
     */
    public static final String HEARTBEAT = "config_control.thrift.heartbeat";
  }

  /**
   * Cleanup service metrics.
   */
  public static final class Cleanup {
    private Cleanup() {
    }

    /**
     * Instances marked as unhealthy.
     * <p>
     * Metric name: {@code config_control.cleanup.unhealthy_instances_marked}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.ServiceInstanceCleanupService}
     */
    public static final String UNHEALTHY_INSTANCES_MARKED = "config_control.cleanup.unhealthy_instances_marked";

    /**
     * Unhealthy instances deleted.
     * <p>
     * Metric name: {@code config_control.cleanup.unhealthy_instances_deleted}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.ServiceInstanceCleanupService}
     */
    public static final String UNHEALTHY_INSTANCES_DELETED = "config_control.cleanup.unhealthy_instances_deleted";

    /**
     * Instances marked as stale.
     * <p>
     * Metric name: {@code config_control.cleanup.stale_instances_marked}
     * <p>
     * 
     * @deprecated Use {@link #UNHEALTHY_INSTANCES_MARKED} instead. Kept for backward compatibility.
     */
    @Deprecated
    public static final String STALE_INSTANCES_MARKED = "config_control.cleanup.stale_instances_marked";

    /**
     * Stale instances deleted.
     * <p>
     * Metric name: {@code config_control.cleanup.stale_instances_deleted}
     * <p>
     * 
     * @deprecated Use {@link #UNHEALTHY_INSTANCES_DELETED} instead. Kept for backward compatibility.
     */
    @Deprecated
    public static final String STALE_INSTANCES_DELETED = "config_control.cleanup.stale_instances_deleted";
  }

  /**
   * Application service metrics.
   */
  public static final class ApplicationService {
    private ApplicationService() {
    }

    /**
     * Application service save operation.
     * <p>
     * Metric name: {@code config_control.application_service.save}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.ApplicationServiceService#save}
     */
    public static final String SAVE = "config_control.application_service.save";

    /**
     * Application service ownership transfer operation.
     * <p>
     * Metric name: {@code config_control.application_service.transfer_ownership}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.ApplicationServiceService#transferOwnership}
     */
    public static final String TRANSFER_OWNERSHIP = "config_control.application_service.transfer_ownership";
  }

  /**
   * Approval workflow metrics.
   */
  public static final class Approval {
    private Approval() {
    }

    /**
     * Approval request creation.
     * <p>
     * Metric name: {@code config_control.approval.create_request}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.ApprovalService#createRequest}
     */
    public static final String CREATE_REQUEST = "config_control.approval.create_request";

    /**
     * Approval decision (approve).
     * <p>
     * Metric name: {@code config_control.approval.approve}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.ApprovalService#approve}
     */
    public static final String APPROVE = "config_control.approval.approve";

    /**
     * Approval decision (reject).
     * <p>
     * Metric name: {@code config_control.approval.reject}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.ApprovalService#reject}
     */
    public static final String REJECT = "config_control.approval.reject";
  }

  /**
   * Service instance metrics.
   */
  public static final class ServiceInstance {
    private ServiceInstance() {
    }

    /**
     * Service instance save operation.
     * <p>
     * Metric name: {@code config_control.service_instance.save}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.ServiceInstanceService#save}
     */
    public static final String SAVE = "config_control.service_instance.save";
  }

  /**
   * Drift event metrics.
   */
  public static final class DriftEvent {
    private DriftEvent() {
    }

    /**
     * Drift event save operation.
     * <p>
     * Metric name: {@code config_control.drift_event.save}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.DriftEventService#save}
     */
    public static final String SAVE = "config_control.drift_event.save";

    /**
     * Drift event resolution operation.
     * <p>
     * Metric name: {@code config_control.drift_event.resolve}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.DriftEventService#resolveForInstance}
     */
    public static final String RESOLVE = "config_control.drift_event.resolve";
  }

  /**
   * Service share metrics.
   */
  public static final class ServiceShare {
    private ServiceShare() {
    }

    /**
     * Service share grant operation.
     * <p>
     * Metric name: {@code config_control.service_share.grant}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.ServiceShareService#grantShare}
     */
    public static final String GRANT = "config_control.service_share.grant";

    /**
     * Service share revoke operation.
     * <p>
     * Metric name: {@code config_control.service_share.revoke}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.ServiceShareService#revokeShare}
     */
    public static final String REVOKE = "config_control.service_share.revoke";
  }
}
