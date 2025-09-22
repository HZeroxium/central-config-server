package com.example.rest.user.config;

import com.example.user.thrift.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for async Thrift clients
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ThriftClientProperties.class)
@RequiredArgsConstructor
public class AsyncThriftClientConfig {

  private final ThriftClientProperties thriftClientProperties;

  @Bean
  public UserService.Client userServiceClient() throws TTransportException {
    log.info("Creating UserService Thrift client for {}:{}",
        thriftClientProperties.getHost(), thriftClientProperties.getPort());

    // Create a lazy-connecting client - don't open connection during bean creation
    TTransport transport = new TSocket(
        thriftClientProperties.getHost(),
        thriftClientProperties.getPort(),
        thriftClientProperties.getTimeout());

    TBinaryProtocol protocol = new TBinaryProtocol(transport);
    UserService.Client client = new UserService.Client(protocol);

    log.info("UserService Thrift client created successfully (lazy connection)");
    return client;
  }
}
