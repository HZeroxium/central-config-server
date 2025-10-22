package com.example.control.api.mapper.domain;

import com.example.control.api.dto.domain.IamTeamDtos;
import com.example.control.domain.object.IamTeam;
import org.springframework.stereotype.Component;

/**
 * API mapper for IAM team operations.
 */
@Component
public class IamTeamApiMapper {

    /**
     * Convert IamTeam domain object to Response DTO.
     *
     * @param domain the domain object
     * @return the response DTO
     */
    public IamTeamDtos.Response toResponse(IamTeam domain) {
        if (domain == null) {
            return null;
        }

        return IamTeamDtos.Response.builder()
                .teamId(domain.getTeamId() != null ? domain.getTeamId().teamId() : null)
                .displayName(domain.getDisplayName())
                .members(domain.getMembers())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .syncedAt(domain.getSyncedAt())
                .build();
    }
}
