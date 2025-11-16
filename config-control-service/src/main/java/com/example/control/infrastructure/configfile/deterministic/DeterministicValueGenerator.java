package com.example.control.infrastructure.configfile.deterministic;

import com.example.control.infrastructure.configfile.ConfigFileGeneratorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Generates deterministic values based on service ID and seed.
 * <p>
 * Uses hash-based generation to ensure same input always produces same output,
 * making it suitable for load testing and reproducible test data.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class DeterministicValueGenerator {

    private static final HexFormat HEX_FORMAT = HexFormat.of().withLowerCase();
    private static final int PORT_MIN = 8000;
    private static final int PORT_MAX = 8999;
    private static final int TIMEOUT_MIN = 1000;
    private static final int TIMEOUT_MAX = 10000;

    private final ConfigFileGeneratorProperties properties;

    /**
     * Gets the seed for deterministic generation.
     */
    private long getSeed() {
        return properties.getDeterministicSeed();
    }


    /**
     * Generates a deterministic hexadecimal string of specified length.
     *
     * @param serviceId service identifier
     * @param length    desired length of hex string
     * @return deterministic hex string
     */
    public String generateHex(String serviceId, int length) {
        String input = serviceId + ":" + getSeed();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String hex = HEX_FORMAT.formatHex(hash);
            return hex.substring(0, Math.min(length, hex.length()));
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash code
            long hash = (serviceId.hashCode() * 31L + getSeed()) & 0xFFFFFFFFL;
            return String.format("%0" + length + "x", hash).substring(0, Math.min(length, 16));
        }
    }

    /**
     * Generates a deterministic port number in the range [PORT_MIN, PORT_MAX].
     *
     * @param serviceId service identifier
     * @return deterministic port number
     */
    public int generatePort(String serviceId) {
        long hash = computeHash(serviceId + ":port");
        int range = PORT_MAX - PORT_MIN + 1;
        return PORT_MIN + (int) (Math.abs(hash) % range);
    }

    /**
     * Generates a deterministic timeout value in milliseconds.
     *
     * @param serviceId service identifier
     * @return deterministic timeout in milliseconds
     */
    public int generateTimeout(String serviceId) {
        long hash = computeHash(serviceId + ":timeout");
        int range = TIMEOUT_MAX - TIMEOUT_MIN + 1;
        return TIMEOUT_MIN + (int) (Math.abs(hash) % range);
    }

    /**
     * Generates a deterministic delay value in milliseconds.
     *
     * @param serviceId service identifier
     * @return deterministic delay in milliseconds
     */
    public int generateDelay(String serviceId) {
        long hash = computeHash(serviceId + ":delay");
        return 50 + (int) (Math.abs(hash) % 200); // 50-250ms
    }

    /**
     * Generates a deterministic boolean value.
     *
     * @param serviceId service identifier
     * @param key       additional key for variation
     * @return deterministic boolean
     */
    public boolean generateBoolean(String serviceId, String key) {
        long hash = computeHash(serviceId + ":" + key);
        return (hash % 2) == 0;
    }

    /**
     * Generates a deterministic integer in a range.
     *
     * @param serviceId service identifier
     * @param key       additional key for variation
     * @param min       minimum value (inclusive)
     * @param max       maximum value (inclusive)
     * @return deterministic integer in range
     */
    public int generateInt(String serviceId, String key, int min, int max) {
        long hash = computeHash(serviceId + ":" + key);
        int range = max - min + 1;
        return min + (int) (Math.abs(hash) % range);
    }

    /**
     * Generates a deterministic string from a list of options.
     *
     * @param serviceId service identifier
     * @param key       additional key for variation
     * @param options   list of options to choose from
     * @return deterministic option from list
     */
    public String generateOption(String serviceId, String key, String[] options) {
        if (options == null || options.length == 0) {
            return "";
        }
        long hash = computeHash(serviceId + ":" + key);
        int index = (int) (Math.abs(hash) % options.length);
        return options[index];
    }

    /**
     * Computes a hash value from input string.
     *
     * @param input input string
     * @return hash value
     */
    private long computeHash(String input) {
        long hash = 0;
        for (int i = 0; i < input.length(); i++) {
            hash = hash * 31 + input.charAt(i);
        }
        return hash ^ getSeed();
    }
}

