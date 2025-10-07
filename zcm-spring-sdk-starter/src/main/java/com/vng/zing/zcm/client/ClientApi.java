package com.vng.zing.zcm.client;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

public interface ClientApi {

  /** Get value from current environment by key */
  String get(String key);

  /** Return all key/value pairs by prefix (simply for debugging) */
  Map<String, Object> getAll(String prefix);

  /** Hash of current configuration (used for drift) */
  String configHash();

  /** Canonical snapshot as a generic map for diagnostics */
  Map<String, Object> configSnapshotMap();

  /** List of healthy instances (by current Discovery provider) */
  List<ServiceInstance> instances(String serviceName);

  /** Choose one instance by LB policy (RR). Return null if no instance. */
  ServiceInstance choose(String serviceName);

  /** WebClient LB to call `http://{service}/path` */
  WebClient http();

  /** Send ping immediately (sync, swallow errors) */
  void pingNow();
}
