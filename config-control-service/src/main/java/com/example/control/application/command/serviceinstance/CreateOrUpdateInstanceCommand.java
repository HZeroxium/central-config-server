package com.example.control.application.command.serviceinstance;

import com.example.control.domain.object.ServiceInstance;

import lombok.Builder;


/**
 * Command to create or update a service instance.
 * 
 * @param instanceId the instance ID
 * @param serviceName the service name
 * @param serviceId the service ID
 * @param host the host
 * @param port the port
 * @param environment the environment
 * @param version the version
 * @param status the instance status
 * @param hasDrift whether drift is detected
 * @param expectedHash expected config hash
 * @param lastAppliedHash last applied config hash
 * @param createdBy the user creating/updating the instance
 */
@Builder
public record CreateOrUpdateInstanceCommand(
        String instanceId,
        String serviceName,
        String serviceId,
        String host,
        Integer port,
        String environment,
        String version,
        ServiceInstance.InstanceStatus status,
        Boolean hasDrift,
        String expectedHash,
        String lastAppliedHash,
        String createdBy
) {}
