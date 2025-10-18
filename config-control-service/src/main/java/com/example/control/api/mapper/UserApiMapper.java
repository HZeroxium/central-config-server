package com.example.control.api.mapper;

import com.example.control.api.dto.UserDtos;
import com.example.control.config.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between User domain objects and API DTOs.
 * <p>
 * This mapper provides static methods for converting between the domain model
 * and the API layer, ensuring clean separation of concerns.
 * </p>
 */
public class UserApiMapper {

    /**
     * Convert UserContext to MeResponse DTO.
     *
     * @param userContext the user context
     * @return the me response DTO
     */
    public static UserDtos.MeResponse toMeResponse(UserContext userContext) {
        return new UserDtos.MeResponse(
                userContext.getUserId(),
                userContext.getUsername(),
                userContext.getEmail(),
                userContext.getFirstName(),
                userContext.getLastName(),
                userContext.getManagerId(),
                userContext.getTeamIds(),
                userContext.getRoles(),
                userContext.getLastLoginAt(),
                userContext.getCreatedAt()
        );
    }

    /**
     * Convert UserContext to ProfileResponse DTO.
     *
     * @param userContext the user context
     * @return the profile response DTO
     */
    public static UserDtos.ProfileResponse toProfileResponse(UserContext userContext) {
        return new UserDtos.ProfileResponse(
                userContext.getUserId(),
                userContext.getUsername(),
                userContext.getEmail(),
                userContext.getFirstName(),
                userContext.getLastName(),
                userContext.getManagerId(),
                userContext.getTeamIds(),
                userContext.getRoles(),
                userContext.getLastLoginAt(),
                userContext.getCreatedAt(),
                userContext.getUpdatedAt()
        );
    }

    /**
     * Convert list of UserContext to ProfileResponse DTOs.
     *
     * @param userContexts the list of user contexts
     * @return the list of profile response DTOs
     */
    public static List<UserDtos.ProfileResponse> toProfileResponseList(List<UserContext> userContexts) {
        return userContexts.stream()
                .map(UserApiMapper::toProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert paginated UserContext to ListResponse DTO.
     *
     * @param page the paginated user contexts
     * @return the list response DTO
     */
    public static UserDtos.ListResponse toListResponse(Page<UserContext> page) {
        List<UserDtos.ProfileResponse> content = toProfileResponseList(page.getContent());
        
        return new UserDtos.ListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    /**
     * Convert ListRequest DTO to Pageable.
     *
     * @param request the list request DTO
     * @return the pageable object
     */
    public static Pageable toPageable(UserDtos.ListRequest request) {
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        String sort = request.sort() != null ? request.sort() : "username,asc";
        
        return org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(sort.split(",")));
    }
}
