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
     * Instances marked as stale.
     * <p>
     * Metric name: {@code config_control.cleanup.stale_instances_marked}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.ServiceInstanceCleanupService}
     */
    public static final String STALE_INSTANCES_MARKED = "config_control.cleanup.stale_instances_marked";

    /**
     * Stale instances deleted.
     * <p>
     * Metric name: {@code config_control.cleanup.stale_instances_deleted}
     * <p>
     * Used in:
     * {@link com.example.control.application.service.infra.ServiceInstanceCleanupService}
     */
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
