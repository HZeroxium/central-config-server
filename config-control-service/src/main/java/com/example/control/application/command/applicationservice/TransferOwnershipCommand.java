package com.example.control.application.command.applicationservice;

import lombok.Builder;

/**
 * Command to transfer ownership of an application service.
 * 
 * @param serviceId the service ID to transfer
 * @param newTeamId the new team ID to assign
 * @param transferredBy the user initiating the transfer
 */
@Builder
public record TransferOwnershipCommand(
        String serviceId,
        String newTeamId,
        String transferredBy
) {}
