package com.example.control.application.command.serviceshare;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

import com.example.control.domain.object.ServiceShare;

/**
 * Command to grant share permissions for a service.
 * 
 * @param serviceId    the service ID to share
 * @param grantToType  the type of grantee (TEAM or USER)
 * @param grantToId    the grantee ID
 * @param permissions  the permissions to grant
 * @param environments optional environment filter
 * @param expiresAt    optional expiration time
 * @param grantedBy    the user granting the share
 */
@Builder
public record GrantShareCommand(
                String serviceId,
                ServiceShare.GranteeType grantToType,
                String grantToId,
                List<ServiceShare.SharePermission> permissions,
                List<String> environments,
                Instant expiresAt,
                String grantedBy) {
}
