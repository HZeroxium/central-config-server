package com.example.control.consulclient.config;

import com.example.control.consulclient.client.*;
import com.example.control.consulclient.core.ConsulConfig;
import com.example.control.consulclient.core.HttpTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


/**
 * Auto-configuration for the new Consul SDK.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ConsulConfig.class)
@RequiredArgsConstructor
public class ConsulClientAutoConfiguration {
    
    private final ConsulConfig consulConfig;
    
    @Bean
    @ConditionalOnMissingBean
    public HttpTransport httpTransport(ObjectMapper objectMapper) {
        log.info("Creating HttpTransport with Consul URL: {}", consulConfig.getConsulUrl());
        RestClient restClient = RestClient.create();
        return new HttpTransport(restClient, objectMapper, consulConfig);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public KVClient kvClient(HttpTransport httpTransport, ObjectMapper objectMapper) {
        log.info("Creating KVClient");
        return new KVClientImpl(httpTransport, objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SessionClient sessionClient(HttpTransport httpTransport, ObjectMapper objectMapper) {
        log.info("Creating SessionClient");
        return new SessionClientImpl(httpTransport, objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TxnClient txnClient(HttpTransport httpTransport, ObjectMapper objectMapper) {
        log.info("Creating TxnClient");
        return new TxnClientImpl(httpTransport, objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public HealthClient healthClient(HttpTransport httpTransport, ObjectMapper objectMapper) {
        log.info("Creating HealthClient");
        return new HealthClientImpl(httpTransport, objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CatalogClient catalogClient(HttpTransport httpTransport, ObjectMapper objectMapper) {
        log.info("Creating CatalogClient");
        return new CatalogClientImpl(httpTransport, objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public AgentClient agentClient(HttpTransport httpTransport, ObjectMapper objectMapper) {
        log.info("Creating AgentClient");
        return new AgentClientImpl(httpTransport, objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public StatusClient statusClient(HttpTransport httpTransport, ObjectMapper objectMapper) {
        log.info("Creating StatusClient");
        return new StatusClientImpl(httpTransport, objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public EventClient eventClient(HttpTransport httpTransport, ObjectMapper objectMapper) {
        log.info("Creating EventClient");
        return new EventClientImpl(httpTransport, objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ConsulClient consulClient(KVClient kvClient, SessionClient sessionClient, TxnClient txnClient,
                                   HealthClient healthClient, CatalogClient catalogClient, AgentClient agentClient,
                                   StatusClient statusClient, EventClient eventClient) {
        log.info("Creating main ConsulClient facade");
        return new ConsulClientImpl(kvClient, sessionClient, txnClient, healthClient, catalogClient, 
                                   agentClient, statusClient, eventClient);
    }
}
