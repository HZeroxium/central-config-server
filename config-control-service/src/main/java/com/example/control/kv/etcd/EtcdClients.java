package com.example.control.kv.etcd;

import com.example.control.kv.KvProperties;
import io.etcd.jetcd.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Provides a managed set of jetcd clients (KV, Lease, Watch, Lock, Maintenance).
 *
 * <p>Key improvements over a minimal wrapper:</p>
 * <ul>
 *   <li>Supports keepalive and connect timeouts via {@link Client.Builder}.</li>
 *   <li>Supports optional etcd key namespace via {@link Client.Builder#namespace(ByteSequence)}.</li>
 *   <li>Exposes {@link Maintenance} client and a robust health check using {@code statusMember}.</li>
 * </ul>
 *
 * <p>References:</p>
 * <ul>
 *   <li>jetcd deprecated list (use builder() / isPrefix): 0.7.x docs. </li>
 *   <li>jetcd ClientBuilder keepalive/connectTimeout: available since 0.5.x.</li>
 *   <li>Maintenance.statusMember(String): preferred over URI overload.</li>
 * </ul>
 */
@Slf4j
@Getter
public final class EtcdClients implements AutoCloseable {

  private final Client client;
  private final KV kv;
  private final Lease lease;
  private final Watch watch;
  private final Lock lock;
  private final Maintenance maintenance;
  private final List<URI> endpoints;

  /**
   * Backward-compatible constructor.
   *
   * @param endpoints etcd endpoints (e.g. http://localhost:2379)
   * @param connectTimeout connection timeout
   */
  public EtcdClients(List<URI> endpoints, Duration connectTimeout) {
    this(buildClient(
        endpoints,
        connectTimeout,
        /*keepaliveTime*/ null,
        /*namespace*/ null
    ), endpoints);
  }

  /**
   * Preferred constructor using {@link KvProperties.Etcd}.
   *
   * @param etcdProps etcd properties (endpoints, timeouts, namespace)
   */
  public EtcdClients(KvProperties.Etcd etcdProps) {
    this(buildClient(
            Objects.requireNonNull(etcdProps.getEndpoints(), "endpoints"),
            Objects.requireNonNull(etcdProps.getConnectTimeout(), "connectTimeout"),
            etcdProps.getKeepaliveTime(),
            emptyToNull(etcdProps.getNamespace())
        ),
        etcdProps.getEndpoints()
    );
  }

  private EtcdClients(Client client, List<URI> endpoints) {
    this.client = client;
    this.endpoints = endpoints;
    this.kv = client.getKVClient();
    this.lease = client.getLeaseClient();
    this.watch = client.getWatchClient();
    this.lock = client.getLockClient();
    this.maintenance = client.getMaintenanceClient();
    log.debug("Initialized etcd clients successfully");
  }

  private static Client buildClient(List<URI> endpoints,
                                    Duration connectTimeout,
                                    Duration keepaliveTime,
                                    String namespace) {
    String[] eps = endpoints.stream().map(URI::toString).toArray(String[]::new);
    ClientBuilder b = Client.builder()
        .endpoints(eps)
        .connectTimeout(connectTimeout); // jetcd builder supports this
    if (keepaliveTime != null && !keepaliveTime.isNegative() && !keepaliveTime.isZero()) {
      b = b.keepaliveTime(keepaliveTime); // supported in jetcd builder
    }
    if (namespace != null && !namespace.isBlank()) {
      b = b.namespace(ByteSequence.from(namespace, StandardCharsets.UTF_8));
    }
    return b.build();
  }

  private static String emptyToNull(String s) {
    return (s == null || s.isBlank()) ? null : s;
  }

  /**
   * Basic cluster health check using Maintenance API.
   *
   * @return true if at least one endpoint reports status successfully
   */
  public boolean isHealthy() {
    try {
      if (endpoints.isEmpty()) return false;
      // Prefer statusMember(String) per jetcd deprecations.
      maintenance.statusMember(endpoints.get(0).toString()).get();
      return true;
    } catch (Exception e) {
      log.debug("etcd health check failed", e);
      return false;
    }
  }

  @Override
  public void close() {
    try {
      client.close();
      log.debug("Closed etcd client");
    } catch (Exception e) {
      log.warn("Error closing etcd client", e);
    }
  }
}
