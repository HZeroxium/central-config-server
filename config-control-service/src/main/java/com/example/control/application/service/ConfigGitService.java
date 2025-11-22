package com.example.control.application.service;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.ServiceCredentialQueryService;
import com.example.control.domain.exception.ConfigConflictException;
import com.example.control.domain.exception.ConfigFileNotFoundException;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.ServiceCredential;
import com.example.control.domain.service.ConfigFilePathMapper;
import com.example.control.domain.service.YAMLValidator;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.infrastructure.cache.GitHubContentCache;
import com.example.control.infrastructure.config.security.DomainPermissionEvaluator;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.infrastructure.config.security.UserContextExtractor;
import com.example.control.infrastructure.configfile.ServiceConfigTemplateGenerator;
import com.example.control.infrastructure.external.github.GitHubProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHContent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic service for GitHub config file operations.
 * <p>
 * Orchestrates config file operations with permission checks, validation,
 * caching, and error handling.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfigGitService {

    private final GitHubProxyService gitHubProxy;
    private final ConfigFilePathMapper pathMapper;
    private final YAMLValidator yamlValidator;
    private final GitHubContentCache contentCache;
    private final ApplicationServiceQueryService applicationServiceQueryService;
    private final DomainPermissionEvaluator permissionEvaluator;
    private final CommitMessageBuilder commitMessageBuilder;
    private final ServiceConfigTemplateGenerator templateGenerator;
    private final ServiceCredentialQueryService serviceCredentialQueryService;

    /**
     * Get config file content from GitHub.
     *
     * @param serviceId the service identifier
     * @param profile   the environment profile
     * @return config file response with content, SHA, and metadata
     * @throws ConfigFileNotFoundException if file not found
     */
    public ConfigFileResponse getConfigFile(String serviceId, String profile) {
        log.info("Getting config file for serviceId={}, profile={}", serviceId, profile);

        // Resolve path
        String path = pathMapper.mapToGitHubPath(serviceId, profile);

        // Check permissions
        UserContext userContext = UserContextExtractor.extract();
        ApplicationService service = applicationServiceQueryService
                .findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new ConfigFileNotFoundException(serviceId, profile));

        if (!permissionEvaluator.canViewService(userContext, service)) {
            log.warn("User {} attempted to view config file for service {} without permission",
                    userContext.getUserId(), serviceId);
            throw new ConfigFileNotFoundException(serviceId, profile);
        }

        // Try cache first
        String cachedContent = contentCache.get(path);
        if (cachedContent != null) {
            log.debug("Returning cached content for path: {}", path);
            // Get SHA from GitHub for cached content (needed for optimistic locking)
            try {
                String sha = gitHubProxy.getFileSha(path);
                return ConfigFileResponse.builder()
                        .content(cachedContent)
                        .sha(sha)
                        .path(path)
                        .build();
            } catch (Exception e) {
                log.warn("Failed to get SHA for cached content, fetching fresh", e);
                // Fall through to fetch fresh
            }
        }

        // Get from GitHub
        GHContent content = gitHubProxy.getFileContent(path);
        String fileContent = decodeContent(content);

        // Cache the content
        contentCache.put(path, fileContent);

        return ConfigFileResponse.builder()
                .content(fileContent)
                .sha(content.getSha())
                .path(path)
                .lastModified(Instant.now()) // GHContent doesn't provide lastModifiedDate, use current time
                .build();
    }

    /**
     * Update config file in GitHub.
     *
     * @param serviceId     the service identifier
     * @param profile       the environment profile
     * @param content       the new file content
     * @param customMessage optional custom commit message
     * @param expectedSha   expected SHA for optimistic locking (optional)
     * @return commit response with SHA and metadata
     * @throws ConfigConflictException if optimistic locking fails
     */
    @Transactional
    public CommitResponse updateConfigFile(String serviceId, String profile, String content,
                                          String customMessage, String expectedSha) {
        log.info("Updating config file for serviceId={}, profile={}", serviceId, profile);

        // Resolve path
        String path = pathMapper.mapToGitHubPath(serviceId, profile);

        // Check permissions
        UserContext userContext = UserContextExtractor.extract();
        ApplicationService service = applicationServiceQueryService
                .findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new ConfigFileNotFoundException(serviceId, profile));

        if (!permissionEvaluator.canEditService(userContext, service)) {
            log.warn("User {} attempted to edit config file for service {} without permission",
                    userContext.getUserId(), serviceId);
            throw new ConfigFileNotFoundException(serviceId, profile);
        }

        // Validate YAML syntax
        YAMLValidator.ValidationResult validation = yamlValidator.validate(content);
        if (!validation.isSuccess()) {
            log.warn("YAML validation failed for serviceId={}, profile={}: {}",
                    serviceId, profile, validation.getErrorMessage());
            throw new IllegalArgumentException("Invalid YAML syntax: " + validation.getErrorMessage());
        }

        // Get current SHA if not provided (for optimistic locking check)
        String currentSha = expectedSha;
        if (currentSha == null) {
            try {
                currentSha = gitHubProxy.getFileSha(path);
            } catch (ConfigFileNotFoundException e) {
                // File doesn't exist yet, that's okay for new files
                log.debug("File doesn't exist yet, will create new file: {}", path);
                currentSha = null;
            }
        }

        // Build commit message
        String commitMessage = commitMessageBuilder.build(
                service.getDisplayName(), profile, userContext, customMessage);

        // Update file via GitHub API
        GHCommit commit = gitHubProxy.updateFile(path, content, commitMessage, currentSha);

        // Evict cache
        contentCache.evict(path);

        log.info("Successfully updated config file for serviceId={}, profile={}, commit={}",
                serviceId, profile, commit.getSHA1());

        try {
            Date commitDate = commit.getCommitDate();
            Instant timestamp = commitDate != null 
                    ? commitDate.toInstant() 
                    : Instant.now();

            return CommitResponse.builder()
                    .sha(commit.getSHA1())
                    .message(commit.getCommitShortInfo().getMessage())
                    .author(commit.getCommitShortInfo().getAuthor().getName())
                    .timestamp(timestamp)
                    .url(commit.getHtmlUrl().toString())
                    .build();
        } catch (IOException e) {
            log.error("Failed to extract commit information", e);
            throw new RuntimeException("Failed to extract commit information: " + e.getMessage(), e);
        }
    }

    /**
     * Get commit history for a config file.
     *
     * @param serviceId the service identifier
     * @param profile   the environment profile
     * @return list of commits (most recent first)
     */
    public List<CommitResponse> getCommitHistory(String serviceId, String profile) {
        log.info("Getting commit history for serviceId={}, profile={}", serviceId, profile);

        // Resolve path
        String path = pathMapper.mapToGitHubPath(serviceId, profile);

        // Check permissions
        UserContext userContext = UserContextExtractor.extract();
        ApplicationService service = applicationServiceQueryService
                .findById(ApplicationServiceId.of(serviceId))
                .orElseThrow(() -> new ConfigFileNotFoundException(serviceId, profile));

        if (!permissionEvaluator.canViewService(userContext, service)) {
            log.warn("User {} attempted to view commit history for service {} without permission",
                    userContext.getUserId(), serviceId);
            throw new ConfigFileNotFoundException(serviceId, profile);
        }

        // Get commits from GitHub
        List<GHCommit> commits = gitHubProxy.getFileCommits(path);

        return commits.stream()
                .map((GHCommit commit) -> {
                    try {
                        // Extract commit SHA (should always be available)
                        String sha = commit.getSHA1();
                        if (sha == null) {
                            log.warn("Commit SHA is null, skipping commit");
                            return null;
                        }

                        // Extract commit date with fallback
                        Instant timestamp;
                        try {
                            Date commitDate = commit.getCommitDate();
                            timestamp = commitDate != null 
                                    ? commitDate.toInstant() 
                                    : Instant.now();
                        } catch (Exception e) {
                            log.warn("Failed to get commit date for commit {}, using current time", sha, e);
                            timestamp = Instant.now();
                        }

                        // Extract commit message and author with fallback
                        String message = "";
                        String author = "";
                        try {
                            var commitInfo = commit.getCommitShortInfo();
                            if (commitInfo != null) {
                                message = commitInfo.getMessage();
                                if (commitInfo.getAuthor() != null) {
                                    author = commitInfo.getAuthor().getName();
                                    if (author == null || author.isEmpty()) {
                                        author = commitInfo.getAuthor().getEmail();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to get commit info for commit {}", sha, e);
                        }

                        // Extract commit URL with fallback
                        String url = "";
                        try {
                            url = commit.getHtmlUrl() != null ? commit.getHtmlUrl().toString() : "";
                        } catch (Exception e) {
                            log.warn("Failed to get commit URL for commit {}", sha, e);
                        }

                        return CommitResponse.builder()
                                .sha(sha)
                                .message(message != null ? message : "")
                                .author(author != null ? author : "")
                                .timestamp(timestamp)
                                .url(url)
                                .build();
                    } catch (Exception e) {
                        log.error("Failed to extract commit information for commit {}", 
                                commit.getSHA1(), e);
                        // Return minimal response with current timestamp
                        return CommitResponse.builder()
                                .sha(commit.getSHA1())
                                .message("")
                                .author("")
                                .timestamp(Instant.now())
                                .url("")
                                .build();
                    }
                })
                .filter(response -> response != null) // Filter out null responses
                .collect(Collectors.toList());
    }

    /**
     * Initialize service directory in GitHub repository with template config file.
     * <p>
     * Creates the service directory structure and initial application.yml template file
     * for the default profile (dev). This is called automatically after service approval.
     * </p>
     * <p>
     * <strong>Business Logic:</strong>
     * <ul>
     * <li>Skips if file already exists (idempotent operation)</li>
     * <li>Creates only for default profile (dev) initially</li>
     * <li>User can create additional profiles later via PUT /api/config-git/{serviceId}/{profile}</li>
     * <li>Includes ZCM SDK configuration with placeholders</li>
     * <li>Includes client credentials section (commented out until activated)</li>
     * </ul>
     * </p>
     *
     * @param serviceId   the service identifier
     * @param displayName the service display name
     * @param environments list of environments (for reference in template)
     */
    @Transactional
    public void initializeServiceDirectory(String serviceId, String displayName, List<String> environments) {
        log.info("Initializing Git directory for service: {} (displayName: {})", serviceId, displayName);

        // Default profile for initial template
        String defaultProfile = "dev";
        String path = pathMapper.mapToGitHubPath(serviceId, defaultProfile);

        // Check if file already exists (skip if exists)
        if (gitHubProxy.fileExists(path)) {
            log.info("Config file already exists for service: {} (profile: {}), skipping initialization", 
                    serviceId, defaultProfile);
            return;
        }

        // Try to get clientId from ServiceCredential if available
        String clientId = null;
        try {
            Optional<ServiceCredential> credentialOpt = serviceCredentialQueryService
                    .findByServiceId(ApplicationServiceId.of(serviceId));
            if (credentialOpt.isPresent()) {
                clientId = credentialOpt.get().getKeycloakClientId();
                log.debug("Found clientId for service {}: {}", serviceId, clientId);
            }
        } catch (Exception e) {
            log.debug("Could not retrieve clientId for service {} (credentials may not exist yet): {}", 
                    serviceId, e.getMessage());
        }

        // Generate template content
        String templateContent = templateGenerator.generateApplicationYmlTemplate(
                serviceId, displayName, environments, clientId);

        // Build commit message
        String commitMessage = String.format(
                "Initialize config directory for %s (service: %s)\n\n" +
                "Created initial application.yml template with ZCM SDK configuration.\n" +
                "Service was approved and ownership transferred.",
                displayName, serviceId);

        try {
            // Create file in GitHub
            GHCommit commit = gitHubProxy.createFile(path, templateContent, commitMessage);

            log.info("Successfully initialized Git directory for service: {} (profile: {}), commit: {}",
                    serviceId, defaultProfile, commit.getSHA1());

            // Evict cache for this path
            contentCache.evict(path);
        } catch (com.example.control.domain.exception.ConfigConflictException e) {
            // File was created between check and creation (race condition)
            log.warn("Config file was created concurrently for service: {} (profile: {}), skipping initialization",
                    serviceId, defaultProfile);
        } catch (Exception e) {
            // Log error but don't fail - approval should succeed even if Git fails
            log.error("Failed to initialize Git directory for service: {} (profile: {}): {}",
                    serviceId, defaultProfile, e.getMessage(), e);
            throw e; // Re-throw to allow caller to handle gracefully
        }
    }

    /**
     * Decode content from GHContent.
     * <p>
     * GitHub API returns content in different formats:
     * - For text files (< 1MB): plain text directly in content field
     * - For large files or binary: base64-encoded content
     * <p>
     * This method handles both cases by:
     * 1. Attempting to decode as base64 first (if it looks like base64)
     * 2. If decode fails, treating content as plain text
     * <p>
     * Note: getContent() is deprecated but is the only available method in kohsuke/github-api.
     *
     * @param content the GitHub content object
     * @return decoded content as string
     */
    @SuppressWarnings("deprecation")
    private String decodeContent(GHContent content) {
        try {
            String rawContent = content.getContent();
            if (rawContent == null || rawContent.isEmpty()) {
                log.warn("GitHub content is null or empty");
                return "";
            }

            // Try to decode as base64 if content looks like base64
            // Base64 contains only A-Z, a-z, 0-9, +, /, = and whitespace
            String trimmedContent = rawContent.trim();
            if (trimmedContent.matches("^[A-Za-z0-9+/=\\s]+$") && trimmedContent.length() > 0) {
                try {
                    // Attempt base64 decode
                    byte[] decoded = Base64.getDecoder().decode(trimmedContent);
                    String result = new String(decoded, StandardCharsets.UTF_8);
                    log.debug("Successfully decoded content as base64 (length: {})", result.length());
                    return result;
                } catch (IllegalArgumentException e) {
                    // Content looks like base64 but decode failed - might be plain text
                    log.debug("Content looks like base64 but decode failed, treating as plain text: {}", 
                            e.getMessage());
                    // Fall through to return as plain text
                }
            }

            // Content is likely plain text (or base64 decode failed)
            // GitHub API may return plain text for small text files
            log.debug("Treating content as plain text (length: {})", rawContent.length());
            return rawContent;
            
        } catch (Exception e) {
            log.error("Failed to decode GitHub content", e);
            throw new RuntimeException("Failed to decode file content: " + e.getMessage(), e);
        }
    }

    /**
     * Config file response DTO.
     */
    @lombok.Data
    @lombok.Builder
    public static class ConfigFileResponse {
        private String content;
        private String sha;
        private String path;
        private java.time.Instant lastModified;
    }

    /**
     * Commit response DTO.
     */
    @lombok.Data
    @lombok.Builder
    public static class CommitResponse {
        private String sha;
        private String message;
        private String author;
        private java.time.Instant timestamp;
        private String url;
    }
}

