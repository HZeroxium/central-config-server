package com.example.control.domain.service;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.domain.exception.ConfigFileNotFoundException;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Maps service ID and profile to GitHub file path.
 * <p>
 * Uses displayName from ApplicationService to construct the file path,
 * following the pattern: {@code {displayName}/application-{profile}.yml}
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigFilePathMapper {

    private static final Pattern VALID_PROFILE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final String FILE_NAME_TEMPLATE = "application-%s.yml";

    private final ApplicationServiceQueryService applicationServiceQueryService;

    /**
     * Maps service ID and profile to GitHub file path.
     * <p>
     * The path is constructed using the displayName from ApplicationService:
     * {@code {displayName}/application-{profile}.yml}
     * </p>
     *
     * @param serviceId the service identifier
     * @param profile   the environment profile (e.g., "dev", "staging", "prod")
     * @return GitHub file path relative to repository root
     * @throws ConfigFileNotFoundException if service not found
     * @throws IllegalArgumentException     if profile is invalid
     */
    public String mapToGitHubPath(String serviceId, String profile) {
        log.debug("Mapping serviceId={}, profile={} to GitHub path", serviceId, profile);

        // Validate profile
        if (profile == null || profile.isBlank()) {
            throw new IllegalArgumentException("Profile cannot be null or blank");
        }

        if (!VALID_PROFILE_PATTERN.matcher(profile).matches()) {
            throw new IllegalArgumentException("Invalid profile format: " + profile);
        }

        // Resolve ApplicationService by serviceId
        ApplicationService service = applicationServiceQueryService
                .findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> {
                    log.warn("ApplicationService not found for serviceId: {}", serviceId);
                    return new ConfigFileNotFoundException(
                            String.format("Service not found: %s", serviceId));
                });

        // Extract displayName
        String displayName = service.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            throw new ConfigFileNotFoundException(
                    String.format("Service %s has no displayName", serviceId));
        }

        // Construct path: {displayName}/application-{profile}.yml
        String fileName = String.format(FILE_NAME_TEMPLATE, profile);
        String path = displayName + "/" + fileName;

        log.debug("Mapped serviceId={}, profile={} to path: {}", serviceId, profile, path);
        return path;
    }
}

