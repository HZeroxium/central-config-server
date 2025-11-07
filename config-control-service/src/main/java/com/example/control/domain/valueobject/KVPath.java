package com.example.control.domain.valueobject;

import lombok.NonNull;

import java.util.regex.Pattern;

/**
 * Value object for KV paths with validation.
 * <p>
 * Ensures paths are safe and normalized, preventing path traversal attacks
 * and enforcing naming conventions.
 * </p>
 */
public record KVPath(@NonNull String value) {
    private static final int MAX_PATH_LENGTH = 512;
    private static final Pattern VALID_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9._/-]+$");
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\.");

    /**
     * Create a validated KV path.
     *
     * @param path the path string
     * @return validated KVPath
     * @throws IllegalArgumentException if path is invalid
     */
    public static KVPath of(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        String normalized = normalize(path);

        if (normalized.length() > MAX_PATH_LENGTH) {
            throw new IllegalArgumentException("Path exceeds maximum length of " + MAX_PATH_LENGTH);
        }

        if (PATH_TRAVERSAL_PATTERN.matcher(normalized).find()) {
            throw new IllegalArgumentException("Path contains path traversal sequence (..)");
        }

        if (!VALID_PATH_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Path contains invalid characters. Allowed: a-z, A-Z, 0-9, ., _, /, -");
        }

        return new KVPath(normalized);
    }

    /**
     * Normalize a path by removing leading/trailing slashes and collapsing multiple slashes.
     *
     * @param path the raw path
     * @return normalized path
     */
    private static String normalize(String path) {
        String normalized = path.trim();
        // Remove leading slashes
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        // Collapse multiple slashes
        normalized = normalized.replaceAll("/+", "/");
        return normalized;
    }

    /**
     * Check if this path is empty (root).
     *
     * @return true if path is empty
     */
    public boolean isEmpty() {
        return value.isEmpty();
    }
}

