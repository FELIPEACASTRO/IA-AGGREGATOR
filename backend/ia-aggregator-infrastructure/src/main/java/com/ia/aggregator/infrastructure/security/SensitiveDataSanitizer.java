package com.ia.aggregator.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class SensitiveDataSanitizer {

    private static final Pattern SECRET_PATTERN = Pattern.compile("(?i)(bearer\\s+[a-z0-9._\\-]+|sk-[a-z0-9\\-]+|sk-ant-[a-z0-9\\-]+|xai-[a-z0-9\\-]+|AIza[0-9A-Za-z_\\-]+)");

    private SensitiveDataSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return SECRET_PATTERN.matcher(input).replaceAll("[REDACTED]");
    }

    public static String fingerprint(String input) {
        if (input == null || input.isBlank()) {
            return "empty";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            return "unavailable";
        }
    }
}
