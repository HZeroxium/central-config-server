package com.example.control.kv.config;

import com.example.control.application.ConsulClient;
import com.example.control.kv.KvProperties;
import com.example.control.kv.KvStore;
import com.example.control.kv.consul.ConsulKvStore;
import com.example.control.kv.etcd.EtcdClients;
import com.example.control.kv.etcd.EtcdKvStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(KvProperties.class)
public class KvAutoConfiguration {

  @Bean
  @ConditionalOnProperty(name = "kv.backend", havingValue = "consul", matchIfMissing = true)
  public KvStore consulKvStore(ConsulClient consulClient) {
    log.info("Configuring Consul KV Store backend");
    return new ConsulKvStore(consulClient);
  }

  @Bean
  @ConditionalOnProperty(name = "kv.backend", havingValue = "etcd")
  public EtcdClients etcdClients(KvProperties kvProperties) {
    log.info("Configuring etcd client with endpoints: {}", kvProperties.getEtcd().getEndpoints());
    return new EtcdClients(
        kvProperties.getEtcd().getEndpoints(),
        kvProperties.getEtcd().getConnectTimeout()
    );
  }

  @Bean
  @ConditionalOnProperty(name = "kv.backend", havingValue = "etcd")
  public KvStore etcdKvStore(KvProperties kvProperties, EtcdClients etcdClients) {
    log.info("Configuring etcd KV Store backend");
    return new EtcdKvStore(kvProperties.getEtcd(), etcdClients);
  }

  // ConsulClient is already configured as a component, no need to create it here
}
