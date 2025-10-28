package com.example.control.application.command.driftevent;

import com.example.control.domain.object.DriftEvent;

import lombok.Builder;

/**
 * Command to create a drift event.
 * 
 * @param serviceName the service name
 * @param instanceId  the instance ID
 * @param serviceId   the service ID
 * @param environment the environment
 * @param description the drift description
 * @param status      the drift status
 * @param detectedBy  the user/system detecting the drift
 */
@Builder
public record CreateDriftEventCommand(
                String serviceName,
                String instanceId,
                String serviceId,
                String environment,
                String description,
                DriftEvent.DriftStatus status,
                String detectedBy) {
}
