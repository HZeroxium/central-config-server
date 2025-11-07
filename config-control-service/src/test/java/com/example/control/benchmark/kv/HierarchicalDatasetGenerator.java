package com.example.control.benchmark.kv;

import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generates hierarchical key-value datasets for benchmarking.
 * <p>
 * Creates a tree structure of keys with configurable fanout, depth, and keys-per-leaf.
 * Tracks all generated keys for cleanup purposes.
 * </p>
 */
@RequiredArgsConstructor
public class HierarchicalDatasetGenerator {

    private final BenchmarkConfig config;
    private final Random random = new SecureRandom();
    private final Set<String> generatedKeys = new HashSet<>();
    private final List<String> prefixes = new ArrayList<>();

    /**
     * Generate the complete dataset.
     *
     * @return dataset with all keys and prefixes
     */
    public Dataset generate() {
        generatedKeys.clear();
        prefixes.clear();

        List<String> keys = new ArrayList<>();
        List<String> levels = new ArrayList<>();
        levels.add(config.prefix());
        prefixes.add(config.prefix());

        // Build hierarchical prefixes
        for (int d = 0; d < config.depth(); d++) {
            List<String> next = new ArrayList<>();
            for (String p : levels) {
                for (int i = 0; i < config.fanout(); i++) {
                    String segment = generateToken(config.keyLenMin(), config.keyLenMax());
                    String newPrefix = p + "/" + segment;
                    next.add(newPrefix);
                    prefixes.add(newPrefix);
                }
            }
            levels = next;
        }

        // Generate leaf keys
        for (String leaf : levels) {
            for (int k = 0; k < config.keysPerLeaf(); k++) {
                String key = leaf + "/" + generateToken(config.keyLenMin(), config.keyLenMax());
                keys.add(key);
                generatedKeys.add(key);
            }
        }

        return new Dataset(keys, prefixes);
    }

    /**
     * Generate a random token.
     *
     * @param min minimum length
     * @param max maximum length
     * @return random token
     */
    private String generateToken(int min, int max) {
        int len = random.nextInt(max - min + 1) + min;
        char[] alphabet = "abcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
        char[] out = new char[len];
        for (int i = 0; i < len; i++) {
            out[i] = alphabet[random.nextInt(alphabet.length)];
        }
        return new String(out);
    }

    /**
     * Generate a random value.
     *
     * @return value bytes
     */
    public byte[] generateValue() {
        byte[] buf = new byte[config.valueSize()];
        if (config.valueMode() == BenchmarkConfig.ValueMode.BINARY) {
            random.nextBytes(buf);
            return buf;
        } else {
            // Text mode: readable ASCII
            char[] alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 _-:./".toCharArray();
            char[] ch = new char[Math.max(1, config.valueSize())];
            for (int i = 0; i < ch.length; i++) {
                ch[i] = alpha[random.nextInt(alpha.length)];
            }
            return new String(ch).getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Get all generated keys.
     *
     * @return set of all generated keys
     */
    public Set<String> getGeneratedKeys() {
        return new HashSet<>(generatedKeys);
    }

    /**
     * Get all prefixes.
     *
     * @return list of all prefixes
     */
    public List<String> getPrefixes() {
        return new ArrayList<>(prefixes);
    }

    /**
     * Dataset structure.
     */
    public record Dataset(
            List<String> keys,
            List<String> prefixes
    ) {
    }
}

