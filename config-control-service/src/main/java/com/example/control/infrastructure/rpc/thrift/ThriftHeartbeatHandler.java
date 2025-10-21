package com.example.control.infrastructure.rpc.thrift;

import com.example.control.domain.object.HeartbeatPayload;
import com.example.control.application.service.HeartbeatService;
import com.example.control.domain.object.ServiceInstance;
import com.example.control.thrift.ConfigControlService;
import com.example.control.thrift.HeartbeatRequest;
import com.example.control.thrift.HeartbeatResponse;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThriftHeartbeatHandler implements ConfigControlService.Iface {
    
    private final HeartbeatService heartbeatService;
    
    @Override
    @Timed("config_control.thrift.heartbeat")
    public HeartbeatResponse recordHeartbeat(HeartbeatRequest request) {
        log.debug("Received Thrift heartbeat from {}:{}", 
            request.getServiceName(), request.getInstanceId());
        
        try {
            // Convert Thrift request to HeartbeatPayload
            HeartbeatPayload payload = convertToPayload(request);
            
            // Process heartbeat
            ServiceInstance instance = heartbeatService.processHeartbeat(payload);
            
            // Build response
            return new HeartbeatResponse()
                .setSuccess(true)
                .setMessage("Heartbeat processed successfully")
                .setTimestamp(System.currentTimeMillis());
                
        } catch (Exception e) {
            log.error("Failed to process Thrift heartbeat", e);
            return new HeartbeatResponse()
                .setSuccess(false)
                .setMessage("Failed: " + e.getMessage())
                .setTimestamp(System.currentTimeMillis());
        }
    }
    
    private HeartbeatPayload convertToPayload(HeartbeatRequest request) {
        return HeartbeatPayload.builder()
            .serviceName(request.getServiceName())
            .instanceId(request.getInstanceId())
            .configHash(request.getConfigHash())
            .host(request.getHost())
            .port(request.getPort())
            .environment(request.getEnvironment())
            .version(request.getVersion())
            .metadata(request.getMetadata())
            .build();
    }
}
