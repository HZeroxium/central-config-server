package com.example.control.application.command.serviceshare;

import lombok.Builder;

/**
 * Command to revoke a service share.
 * 
 * @param shareId the share ID to revoke
 * @param revokedBy the user revoking the share
 */
@Builder
public record RevokeShareCommand(
        String shareId,
        String revokedBy
) {}
