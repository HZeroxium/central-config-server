package com.example.control.api.http.mapper.infra;

import com.example.control.api.http.dto.infra.ConfigGitDtos;
import com.example.control.application.service.ConfigGitService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between GitHub API objects and DTOs.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Component
public class ConfigGitApiMapper {

    /**
     * Map ConfigGitService.ConfigFileResponse to API DTO.
     *
     * @param response the service response
     * @return API DTO
     */
    public ConfigGitDtos.ConfigFileResponse toConfigFileResponse(ConfigGitService.ConfigFileResponse response) {
        if (response == null) {
            return null;
        }

        return ConfigGitDtos.ConfigFileResponse.builder()
                .content(response.getContent())
                .sha(response.getSha())
                .path(response.getPath())
                .lastModified(response.getLastModified())
                .build();
    }

    /**
     * Map ConfigGitService.CommitResponse to API DTO.
     *
     * @param response the service response
     * @return API DTO
     */
    public ConfigGitDtos.CommitResponse toCommitResponse(ConfigGitService.CommitResponse response) {
        if (response == null) {
            return null;
        }

        return ConfigGitDtos.CommitResponse.builder()
                .sha(response.getSha())
                .message(response.getMessage())
                .author(response.getAuthor())
                .timestamp(response.getTimestamp())
                .url(response.getUrl())
                .build();
    }

    /**
     * Map list of ConfigGitService.CommitResponse to API DTOs.
     *
     * @param responses the service responses
     * @return list of API DTOs
     */
    public List<ConfigGitDtos.CommitResponse> toCommitResponseList(List<ConfigGitService.CommitResponse> responses) {
        if (responses == null) {
            return List.of();
        }

        return responses.stream()
                .map(this::toCommitResponse)
                .collect(Collectors.toList());
    }
}

