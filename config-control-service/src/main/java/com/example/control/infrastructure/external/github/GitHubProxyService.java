package com.example.control.infrastructure.external.github;

import com.example.control.infrastructure.config.github.GitHubConfigProperties;
import com.example.control.infrastructure.resilience.ResilienceDecoratorsFactory;

import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Low-level GitHub API wrapper service.
 * <p>
 * Provides direct access to GitHub API operations with resilience patterns
 * (circuit breaker, retry, bulkhead) and error mapping.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubProxyService {

    private static final String SERVICE_NAME = "github";

    private final GitHubConfigProperties configProperties;
    private final ResilienceDecoratorsFactory resilienceDecoratorsFactory;

    /**
     * Get GitHub API client instance.
     * <p>
     * Creates a new GitHub client with OAuth token authentication.
     * The client is not cached to avoid stale connections.
     * </p>
     *
     * @return GitHub API client
     * @throws IOException if GitHub client creation fails
     */
    public GitHub getGitHub() throws IOException {
        return new GitHubBuilder()
                .withOAuthToken(configProperties.getToken())
                .build();
    }

    /**
     * Get GitHub repository instance.
     *
     * @return repository instance
     * @throws IOException if repository access fails
     */
    public GHRepository getRepository() throws IOException {
        GitHub github = getGitHub();
        return github.getRepository(configProperties.getOwner() + "/" + configProperties.getRepo());
    }

    /**
     * Get file content from GitHub repository.
     * <p>
     * Uses resilience patterns (circuit breaker, retry, bulkhead) for fault tolerance.
     * </p>
     *
     * @param path the file path relative to repository root
     * @return file content with metadata
     * @throws RuntimeException mapped from GitHub exceptions
     */
    public GHContent getFileContent(String path) {
        log.debug("Getting file content from GitHub: {}", path);

        Supplier<GHContent> supplier = () -> {
            try {
                GHRepository repo = getRepository();
                return repo.getFileContent(path, configProperties.getBranch());
            } catch (IOException e) {
                throw GitHubExceptionMapper.mapException(path, e);
            }
        };

        Function<Throwable, GHContent> fallback = throwable -> {
            log.error("Failed to get file content from GitHub: {}", path, throwable);
            throw GitHubExceptionMapper.mapException(path, (Exception) throwable);
        };

        return resilienceDecoratorsFactory.decorateSupplier(SERVICE_NAME, supplier, fallback).get();
    }

    /**
     * Get current file SHA for optimistic locking.
     *
     * @param path the file path
     * @return current SHA, or null if file doesn't exist
     */
    public String getFileSha(String path) {
        log.debug("Getting file SHA from GitHub: {}", path);

        try {
            GHContent content = getFileContent(path);
            return content.getSha();
        } catch (Exception e) {
            // If file doesn't exist, return null
            if (e instanceof com.example.control.domain.exception.ConfigFileNotFoundException) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Update file in GitHub repository.
     * <p>
     * Uses optimistic locking via SHA. If expectedSha is provided and doesn't match
     * current SHA, a ConfigConflictException will be thrown.
     * </p>
     *
     * @param path        the file path relative to repository root
     * @param content     the new file content
     * @param message     the commit message
     * @param expectedSha the expected SHA for optimistic locking (optional)
     * @return commit information
     * @throws RuntimeException mapped from GitHub exceptions
     */
    public GHCommit updateFile(String path, String content, String message, String expectedSha) {
        log.debug("Updating file in GitHub: {} (expected SHA: {})", path, expectedSha);

        Supplier<GHCommit> supplier = () -> {
            try {
                GHRepository repo = getRepository();
                GHContent fileContent = repo.getFileContent(path, configProperties.getBranch());

                // Check optimistic locking if expected SHA provided
                if (expectedSha != null && !expectedSha.equals(fileContent.getSha())) {
                    throw new com.example.control.domain.exception.ConfigConflictException(
                            String.format("File %s was modified. Expected SHA: %s, Actual SHA: %s",
                                    path, expectedSha, fileContent.getSha()),
                            expectedSha, fileContent.getSha());
                }

                // Update file
                fileContent.update(content, message, configProperties.getBranch());
                
                // Get the commit that was created
                return repo.queryCommits()
                        .path(path)
                        .pageSize(1)
                        .list()
                        .iterator()
                        .next();
            } catch (IOException e) {
                throw GitHubExceptionMapper.mapException(path, e);
            }
        };

        // For non-idempotent operations, we still want circuit breaker and bulkhead
        // but we'll handle exceptions manually instead of using fallback
        try {
            // Get circuit breaker and bulkhead
            var circuitBreaker = resilienceDecoratorsFactory.getCircuitBreaker(SERVICE_NAME);
            var bulkhead = resilienceDecoratorsFactory.getBulkhead(SERVICE_NAME);
            
            // Execute with circuit breaker and bulkhead (fixed argument order)
            return circuitBreaker.executeSupplier(
                Bulkhead.decorateSupplier(bulkhead, supplier)
            );
        } catch (RuntimeException e) {
            // Re-throw domain exceptions as-is
            throw e;
        } catch (Exception e) {
            throw GitHubExceptionMapper.mapException(path, (Exception) e);
        }
    }

    /**
     * Get commit history for a file.
     *
     * @param path the file path
     * @return list of commits (most recent first)
     */
    public List<GHCommit> getFileCommits(String path) {
        log.debug("Getting commit history from GitHub: {}", path);

        Supplier<List<GHCommit>> supplier = () -> {
            try {
                GHRepository repo = getRepository();
                return repo.queryCommits()
                        .path(path)
                        .pageSize(50) // Limit to 50 most recent commits
                        .list()
                        .toList();
            } catch (IOException e) {
                throw GitHubExceptionMapper.mapException(path, e);
            }
        };

        Function<Throwable, List<GHCommit>> fallback = throwable -> {
            log.error("Failed to get commit history from GitHub: {}", path, throwable);
            throw GitHubExceptionMapper.mapException(path, (Exception) throwable);
        };

        return resilienceDecoratorsFactory.decorateSupplier(SERVICE_NAME, supplier, fallback).get();
    }

    /**
     * Check if a file exists in GitHub repository.
     * <p>
     * Uses resilience patterns (circuit breaker, bulkhead) for fault tolerance.
     * Returns false if file doesn't exist (doesn't throw exception).
     * </p>
     *
     * @param path the file path relative to repository root
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String path) {
        log.debug("Checking if file exists in GitHub: {}", path);

        Supplier<Boolean> supplier = () -> {
            try {
                GHRepository repo = getRepository();
                repo.getFileContent(path, configProperties.getBranch());
                return true;
            } catch (IOException e) {
                // If file doesn't exist, return false (don't throw exception)
                if (e instanceof org.kohsuke.github.GHFileNotFoundException) {
                    return false;
                }
                // For other IO exceptions, check if it's a ConfigFileNotFoundException
                RuntimeException mapped = GitHubExceptionMapper.mapException(path, e);
                if (mapped instanceof com.example.control.domain.exception.ConfigFileNotFoundException) {
                    return false;
                }
                // Re-throw other exceptions
                throw mapped;
            }
        };

        Function<Throwable, Boolean> fallback = throwable -> {
            log.error("Failed to check file existence in GitHub: {}", path, throwable);
            // For fallback, assume file doesn't exist to be safe
            return false;
        };

        return resilienceDecoratorsFactory.decorateSupplier(SERVICE_NAME, supplier, fallback).get();
    }

    /**
     * Create a new file in GitHub repository.
     * <p>
     * Uses resilience patterns (circuit breaker, bulkhead) for fault tolerance.
     * If file already exists, throws ConfigConflictException.
     * </p>
     *
     * @param path    the file path relative to repository root
     * @param content the file content
     * @param message the commit message
     * @return commit information
     * @throws RuntimeException mapped from GitHub exceptions
     * @throws com.example.control.domain.exception.ConfigConflictException if file already exists
     */
    public GHCommit createFile(String path, String content, String message) {
        log.debug("Creating file in GitHub: {}", path);

        Supplier<GHCommit> supplier = () -> {
            try {
                GHRepository repo = getRepository();
                
                // Check if file already exists
                try {
                    GHContent existingContent = repo.getFileContent(path, configProperties.getBranch());
                    // File exists, throw conflict exception
                    throw new com.example.control.domain.exception.ConfigConflictException(
                            String.format("File %s already exists in repository", path),
                            existingContent.getSha(), existingContent.getSha());
                } catch (org.kohsuke.github.GHFileNotFoundException e) {
                    // File doesn't exist, proceed with creation
                    log.debug("File {} doesn't exist, proceeding with creation", path);
                }

                // Create new file
                repo.createContent()
                        .content(content)
                        .message(message)
                        .path(path)
                        .branch(configProperties.getBranch())
                        .commit();

                // Get the commit that was created
                return repo.queryCommits()
                        .path(path)
                        .pageSize(1)
                        .list()
                        .iterator()
                        .next();
            } catch (IOException e) {
                throw GitHubExceptionMapper.mapException(path, e);
            }
        };

        // For non-idempotent operations, we still want circuit breaker and bulkhead
        // but we'll handle exceptions manually instead of using fallback
        try {
            // Get circuit breaker and bulkhead
            var circuitBreaker = resilienceDecoratorsFactory.getCircuitBreaker(SERVICE_NAME);
            var bulkhead = resilienceDecoratorsFactory.getBulkhead(SERVICE_NAME);
            
            // Execute with circuit breaker and bulkhead
            return circuitBreaker.executeSupplier(
                    Bulkhead.decorateSupplier(bulkhead, supplier)
            );
        } catch (RuntimeException e) {
            // Re-throw domain exceptions as-is
            throw e;
        } catch (Exception e) {
            throw GitHubExceptionMapper.mapException(path, (Exception) e);
        }
    }
}

