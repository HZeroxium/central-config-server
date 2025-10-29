package com.example.keycloak.util;

import java.util.regex.Pattern;

/**
 * Utility class for phone number normalization and validation.
 * Follows E.164 international format standard.
 */
public class PhoneNumberUtil {
    
    // Strict E.164: +[country][number] (max 15 digits total)
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    // Vietnamese national format: leading 0 + 9 digits, allowing spaces/dashes between digits
    // Examples: 0987654321, 0987 654 321, 0987-654-321
    private static final Pattern VN_NATIONAL_PATTERN = Pattern.compile("^0(?:\\d[\\s-]?){8}\\d$");

    // Accept lenient detection: E.164 with optional separators, or VN national with separators
    private static final Pattern LENIENT_DETECTION_PATTERN = Pattern.compile(
            "^(?:\\+?[0-9][0-9\\s-]{7,16})$"
    );
    
    /**
     * Check if the input string looks like a phone number.
     * 
     * @param input the input string to check
     * @return true if input matches phone number pattern
     */
    public static boolean isPhone(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        String trimmed = input.trim();
        // Quick lenient check first
        if (!LENIENT_DETECTION_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        // Remove common separators for precise checks
        String compact = trimmed.replaceAll("[\\s-]", "");
        return E164_PATTERN.matcher(ensurePlusPrefix(compact)).matches()
                || VN_NATIONAL_PATTERN.matcher(compact).matches();
    }
    
    /**
     * Normalize phone number to E.164 format.
     * E.164 format: +[country code][national number]
     * 
     * @param input the input phone number
     * @return normalized phone number in E.164 format
     */
    public static String normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        String raw = input.trim();
        // Strip spaces/dashes/parentheses
        String compact = raw.replaceAll("[\\s()-]", "");

        // Case 1: already E.164 (+...)
        if (compact.startsWith("+")) {
            return E164_PATTERN.matcher(compact).matches() ? compact : raw;
        }

        // Case 2: Vietnamese national starting with 0 and 10 digits
        if (VN_NATIONAL_PATTERN.matcher(raw.replaceAll("[\\s-]", "")).matches()) {
            String digits = compact.replaceFirst("^0", "");
            String normalized = "+84" + digits;
            return E164_PATTERN.matcher(normalized).matches() ? normalized : raw;
        }

        // Case 3: Starts with 84 (no plus) and then 9 digits
        if (compact.matches("^84\\d{9}$")) {
            String normalized = "+" + compact;
            return E164_PATTERN.matcher(normalized).matches() ? normalized : raw;
        }

        // Fallback: if it's all digits and looks like a plausible local VN number (9-10 digits)
        if (compact.matches("^\\d{9,10}$")) {
            // Assume VN if 10 digits and starts with 0
            if (compact.length() == 10 && compact.startsWith("0")) {
                String normalized = "+84" + compact.substring(1);
                return E164_PATTERN.matcher(normalized).matches() ? normalized : raw;
            }
        }

        // Not recognized; return original input
        return raw;
    }
    
    /**
     * Validate if the phone number is in proper E.164 format.
     * 
     * @param phone the phone number to validate
     * @return true if valid E.164 format
     */
    public static boolean isValidE164(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return E164_PATTERN.matcher(phone.trim()).matches();
    }

    private static String ensurePlusPrefix(String input) {
        return input.startsWith("+") ? input : "+" + input;
    }
}
