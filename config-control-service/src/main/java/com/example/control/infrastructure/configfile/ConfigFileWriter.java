package com.example.control.infrastructure.configfile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Handles filesystem operations for config file generation.
 * <p>
 * Creates directories and writes files with proper error handling
 * and path validation.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigFileWriter {

    private final ConfigFileGeneratorProperties properties;

    /**
     * Writes a config file for a service.
     *
     * @param serviceId  service identifier
     * @param fileName   file name (e.g., "application.yml")
     * @param content    file content
     * @param skipIfExists whether to skip if file already exists
     * @return true if file was written, false if skipped
     * @throws IOException if file cannot be written
     */
    public boolean writeFile(String serviceId, String fileName, String content, boolean skipIfExists) throws IOException {
        Path serviceDir = getServiceDirectory(serviceId);
        Path filePath = serviceDir.resolve(fileName);

        // Create service directory if it doesn't exist
        if (!Files.exists(serviceDir)) {
            Files.createDirectories(serviceDir);
            log.debug("Created service directory: {}", serviceDir);
        }

        // Check if file exists and should be skipped
        if (skipIfExists && Files.exists(filePath)) {
            log.debug("Skipping existing file: {}", filePath);
            return false;
        }

        // Validate file name for security (prevent directory traversal)
        validateFileName(fileName);

        // Write file
        Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        log.debug("Written config file: {}", filePath);
        return true;
    }

    /**
     * Gets the service directory path.
     *
     * @param serviceId service identifier
     * @return service directory path
     */
    public Path getServiceDirectory(String serviceId) {
        // Validate service ID for security
        validateServiceId(serviceId);

        Path basePath = Paths.get(properties.getBasePath()).toAbsolutePath().normalize();
        Path serviceDir = basePath.resolve(serviceId).normalize();

        // Ensure service directory is within base path (prevent directory traversal)
        if (!serviceDir.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid service ID: " + serviceId);
        }

        return serviceDir;
    }

    /**
     * Checks if a file exists for a service.
     *
     * @param serviceId service identifier
     * @param fileName  file name
     * @return true if file exists
     */
    public boolean fileExists(String serviceId, String fileName) {
        try {
            Path serviceDir = getServiceDirectory(serviceId);
            Path filePath = serviceDir.resolve(fileName);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.warn("Error checking file existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates file name to prevent directory traversal attacks.
     *
     * @param fileName file name
     * @throws IllegalArgumentException if file name is invalid
     */
    private void validateFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
    }

    /**
     * Validates service ID for filesystem safety.
     *
     * @param serviceId service identifier
     * @throws IllegalArgumentException if service ID is invalid
     */
    private void validateServiceId(String serviceId) {
        if (serviceId == null || serviceId.isEmpty()) {
            throw new IllegalArgumentException("Service ID cannot be null or empty");
        }
        if (serviceId.contains("..") || serviceId.contains("/") || serviceId.contains("\\")) {
            throw new IllegalArgumentException("Invalid service ID: " + serviceId);
        }
        // Allow only alphanumeric, hyphen, underscore
        if (!serviceId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Service ID contains invalid characters: " + serviceId);
        }
    }

    /**
     * Gets the base path.
     *
     * @return base path
     */
    public Path getBasePath() {
        return Paths.get(properties.getBasePath()).toAbsolutePath().normalize();
    }

    /**
     * Ensures base path exists and is writable.
     *
     * @throws IOException if base path cannot be created or is not writable
     */
    public void ensureBasePathExists() throws IOException {
        Path basePath = getBasePath();
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
            log.info("Created base config directory: {}", basePath);
        }
        if (!Files.isWritable(basePath)) {
            throw new IOException("Base path is not writable: " + basePath);
        }
    }
}

