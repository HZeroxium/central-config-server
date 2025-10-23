package com.example.control.api.mapper.domain;

import com.example.control.api.dto.common.PageDtos;
import com.example.control.api.dto.domain.IamTeamDtos;
import com.example.control.domain.object.IamTeam;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

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

    /**
     * Convert Page<IamTeam> to IamTeamPageResponse.
     *
     * @param page the Spring Page containing IamTeam entities
     * @return the domain-specific page response
     */
    public IamTeamDtos.IamTeamPageResponse toPageResponse(Page<IamTeam> page) {
        List<IamTeamDtos.Response> items = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        
        return IamTeamDtos.IamTeamPageResponse.builder()
                .items(items)
                .metadata(PageDtos.PageMetadata.from(page))
                .build();
    }
}
