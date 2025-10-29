package com.example.control.api.mapper.domain;

import com.example.control.api.dto.domain.UserDtos;
import com.example.control.infrastructure.config.security.UserContext;

/**
 * Mapper for User API operations.
 * <p>
 * Provides mapping between UserContext and API DTOs with
 * proper JSON serialization.
 * </p>
 */
public class UserApiMapper {

    /**
     * Map UserContext to MeResponse DTO.
     *
     * @param userContext the user context
     * @return the me response DTO
     */
    public UserDtos.MeResponse toMeResponse(UserContext userContext) {
        return new UserDtos.MeResponse(
                userContext.getUserId(),
                userContext.getUsername(),
                userContext.getEmail(),
                userContext.getFirstName(),
                userContext.getLastName(),
                userContext.getTeamIds(),
                userContext.getRoles(),
                userContext.getManagerId());
    }
}