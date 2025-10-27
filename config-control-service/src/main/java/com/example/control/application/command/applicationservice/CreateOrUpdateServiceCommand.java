package com.example.control.application.command.applicationservice;

import lombok.Builder;

import java.util.List;
import java.util.Map;

import com.example.control.domain.object.ApplicationService;

/**
 * Command to create or update an application service.
 * 
 * @param id the service ID (null for new services)
 * @param displayName the display name
 * @param ownerTeamId the owner team ID
 * @param environments list of environments
 * @param tags list of tags
 * @param repoUrl repository URL
 * @param lifecycle service lifecycle status
 * @param attributes additional attributes
 * @param createdBy the user creating/updating the service
 */
@Builder
public record CreateOrUpdateServiceCommand(
        String id,
        String displayName,
        String ownerTeamId,
        List<String> environments,
        List<String> tags,
        String repoUrl,
        ApplicationService.ServiceLifecycle lifecycle,
        Map<String, String> attributes,
        String createdBy
) {}
