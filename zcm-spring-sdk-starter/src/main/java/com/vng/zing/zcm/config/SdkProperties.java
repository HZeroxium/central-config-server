package com.vng.zing.zcm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zcm.sdk")
public class SdkProperties {

  private String serviceName;
  private String instanceId;

  private String configServerUrl; // e.g. http://config:8888
  private String controlUrl; // e.g. http://ctrl:8080

  private Discovery discovery = new Discovery();
  private Lb lb = new Lb();
  private Ping ping = new Ping();
  private Bus bus = new Bus();

  @Data
  public static class Discovery {
    private Provider provider = Provider.CONSUL;
    private Consul consul = new Consul();
  }

  public enum Provider {
    CONSUL, CONTROL
  }

  @Data
  public static class Consul {
    private String host = "localhost";
    private int port = 8500;
    private boolean heartbeatEnabled = true;
    private String heartbeatTtl = "10s"; // mapped to spring.cloud.consul.discovery.heartbeat.ttl
  }

  @Data
  public static class Lb {
    private String policy = "RR"; // Round-robin (for choose())
  }

  @Data
  public static class Ping {
    private boolean enabled = true;
    private String fixedDelay = "30000"; // ms
  }

  @Data
  public static class Bus {
    private boolean refreshEnabled = false;
    private String refreshTopic = "springCloudBus";
    private String kafkaBootstrapServers;
  }
}
