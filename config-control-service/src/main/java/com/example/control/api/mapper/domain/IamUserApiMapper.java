package com.example.control.api.mapper.domain;

import com.example.control.api.dto.common.PageDtos;
import com.example.control.api.dto.domain.IamUserDtos;
import com.example.control.domain.object.IamUser;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * API mapper for IAM user operations.
 */
@Component
public class IamUserApiMapper {

    /**
     * Convert IamUser domain object to Response DTO.
     *
     * @param domain the domain object
     * @return the response DTO
     */
    public IamUserDtos.Response toResponse(IamUser domain) {
        if (domain == null) {
            return null;
        }

        return IamUserDtos.Response.builder()
                .userId(domain.getUserId() != null ? domain.getUserId().userId() : null)
                .username(domain.getUsername())
                .email(domain.getEmail())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .teamIds(domain.getTeamIds())
                .managerId(domain.getManagerId())
                .roles(domain.getRoles())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .syncedAt(domain.getSyncedAt())
                .build();
    }

    /**
     * Convert Page<IamUser> to IamUserPageResponse.
     *
     * @param page the Spring Page containing IamUser entities
     * @return the domain-specific page response
     */
    public IamUserDtos.IamUserPageResponse toPageResponse(Page<IamUser> page) {
        List<IamUserDtos.Response> items = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        
        return IamUserDtos.IamUserPageResponse.builder()
                .items(items)
                .metadata(PageDtos.PageMetadata.from(page))
                .build();
    }
}
