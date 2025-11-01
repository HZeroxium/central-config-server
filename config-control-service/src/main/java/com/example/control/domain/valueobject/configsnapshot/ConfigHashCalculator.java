package com.example.control.domain.valueobject.configsnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utility class for computing SHA-256 hashes from canonical configuration strings.
 * <p>
 * Used to ensure consistent hashing across systems for configuration drift detection.
 * <p>
 * This implementation returns a lowercase hexadecimal string representation.
 */
public final class ConfigHashCalculator {

    /**
     * Private constructor to prevent instantiation (utility class).
     */
    private ConfigHashCalculator() {
    }

    /**
     * Computes a SHA-256 hash for the provided canonical configuration string.
     *
     * @param canonical the canonical string (from {@link ConfigSnapshot#toCanonicalString()})
     * @return SHA-256 hash as a lowercase hex string, or {@code null} if an error occurs
     */
    public static String hash(String canonical) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            // Should not happen in standard JVM; fallback for safety
            return null;
        }
    }
}
