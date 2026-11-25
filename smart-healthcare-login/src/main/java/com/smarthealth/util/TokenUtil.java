package com.smarthealth.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Signs and verifies short-lived HMAC-SHA256 handoff tokens shared between
 * the Java Servlets portal and Python (Streamlit / Python backend services).
 */
public final class TokenUtil {
    private static final String HMAC_ALG = "HmacSHA256";
    private static final String DEV_FALLBACK_SECRET = "smarthealth-dev-secret-change-me";
    private static final long DEFAULT_TTL_SECONDS = 300;

    private TokenUtil() {
    }

    /**
     * Resolves the shared HMAC secret from SMARTHEALTH_SHARED_SECRET or falls back to a dev default.
     */
    public static String getSharedSecret() {
        String env = System.getenv("SMARTHEALTH_SHARED_SECRET");
        return (env != null && !env.isBlank()) ? env : DEV_FALLBACK_SECRET;
    }

    /**
     * Creates a signed token with the default 5-minute TTL.
     */
    public static String createSignedToken(String subject, String name, String role, String mode, String code) {
        return createSignedToken(subject, name, role, mode, code, DEFAULT_TTL_SECONDS, getSharedSecret());
    }

    /**
     * Creates a signed token in the format base64url(jsonPayload).base64url(hmacSha256).
     */
    public static String createSignedToken(
            String subject,
            String name,
            String role,
            String mode,
            String code,
            long ttlSeconds,
            String secret
    ) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payloadJson = "{"
                + "\"sub\":\"" + escapeJson(subject) + "\","
                + "\"name\":\"" + escapeJson(name) + "\","
                + "\"role\":\"" + escapeJson(role) + "\","
                + "\"mode\":\"" + escapeJson(mode) + "\","
                + "\"code\":\"" + escapeJson(code) + "\","
                + "\"exp\":" + exp
                + "}";
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signatureB64 = sign(payloadB64, secret);
        return payloadB64 + "." + signatureB64;
    }

    /**
     * Verifies the token signature and expiration, returning the decoded JSON payload if valid.
     */
    public static Optional<String> verifyToken(String token, String secret) {
        if (token == null || secret == null) {
            return Optional.empty();
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot >= token.length() - 1 || token.indexOf('.', dot + 1) != -1) {
            return Optional.empty();
        }
        String payloadB64 = token.substring(0, dot);
        String providedSig = token.substring(dot + 1);
        String expectedSig = sign(payloadB64, secret);

        if (!MessageDigest.isEqual(
                expectedSig.getBytes(StandardCharsets.UTF_8),
                providedSig.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        try {
            String json = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            long exp = extractExp(json);
            if (Instant.now().getEpochSecond() > exp) {
                return Optional.empty();
            }
            return Optional.of(json);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Escapes a string value for embedding inside a JSON string literal.
     */
    public static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private static String sign(String payloadB64, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] raw = mac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Failed to sign HMAC-SHA256 token", ex);
        }
    }

    private static long extractExp(String json) {
        int idx = json.lastIndexOf("\"exp\":");
        if (idx == -1) {
            throw new IllegalArgumentException("Missing exp claim");
        }
        int start = idx + "\"exp\":".length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }
}
