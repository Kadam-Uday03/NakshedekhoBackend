package com.nakshedekho.security;

import java.util.regex.Pattern;

/**
 * Centralized input sanitization utility.
 *
 * All user-controlled strings pass through here before being persisted or used in logic.
 * This is a defense-in-depth layer — JPA parameterized queries already prevent SQL injection,
 * but sanitizing input also protects downstream processing (logging, email, PDF generation, etc.)
 *
 * What each method does:
 *   sanitizeText   — trims, collapses whitespace, removes null bytes and control characters
 *   sanitizeSlug   — strips everything except [a-z0-9-] (URL-safe slug)
 *   sanitizeSearch — strips LIKE wildcard characters to prevent query amplification
 *   sanitizeUrl    — allows only http/https/relative paths; blocks javascript: and data: URIs
 *   sanitizePhone  — allows only digits, spaces, +, -, (, )
 *   isValidCategory— whitelist check against allowed category values
 */
public final class InputSanitizer {

    private InputSanitizer() {}

    // ── Patterns ─────────────────────────────────────────────────────────────

    /** Control characters incl. null byte — common in injection payloads */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");

    /** Characters that could amplify LIKE queries (%_) */
    private static final Pattern LIKE_WILDCARDS = Pattern.compile("[%_\\\\]");

    /** Only safe characters for URL slugs */
    private static final Pattern SLUG_UNSAFE = Pattern.compile("[^a-z0-9-]");

    /** Only safe characters for phone numbers */
    private static final Pattern PHONE_UNSAFE = Pattern.compile("[^\\d\\s+\\-().]");

    /** Dangerous URI schemes — blocks javascript:, data:, vbscript: etc. */
    private static final Pattern DANGEROUS_URI = Pattern.compile(
            "^\\s*(javascript|data|vbscript|file|blob)\\s*:", Pattern.CASE_INSENSITIVE);

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * General-purpose text sanitization:
     * - Trims leading/trailing whitespace
     * - Collapses repeated internal whitespace to a single space
     * - Removes null bytes and ASCII control characters (common injection vectors)
     * Returns null if input is null.
     */
    public static String sanitizeText(String input) {
        if (input == null) return null;
        String cleaned = CONTROL_CHARS.matcher(input).replaceAll("");
        return cleaned.strip().replaceAll("\\s+", " ");
    }

    /**
     * Sanitizes a URL slug: lowercases, removes all characters outside [a-z0-9-],
     * and collapses consecutive dashes.
     */
    public static String sanitizeSlug(String input) {
        if (input == null || input.isBlank()) return null;
        return SLUG_UNSAFE.matcher(input.toLowerCase().strip())
                .replaceAll("")
                .replaceAll("-{2,}", "-")          // collapse --
                .replaceAll("^-|-$", "");           // strip leading/trailing -
    }

    /**
     * Sanitizes a search query before it is passed to a LIKE query.
     * Strips % _ and \ to prevent LIKE wildcard amplification attacks
     * (e.g., a query of "%" would match every row).
     * Also applies general text sanitization.
     */
    public static String sanitizeSearch(String input) {
        if (input == null) return null;
        String cleaned = sanitizeText(input);
        if (cleaned == null) return null;
        return LIKE_WILDCARDS.matcher(cleaned).replaceAll("");
    }

    /**
     * Validates that a URL is safe (http/https or a server-relative path).
     * Blocks javascript:, data:, vbscript:, file: etc.
     * Returns null if the URL is dangerous, the original otherwise.
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String trimmed = url.strip();
        if (DANGEROUS_URI.matcher(trimmed).find()) {
            return null;  // Caller should treat null as "URL rejected"
        }
        // Only allow http, https, or relative paths (starting with /)
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")
                && !trimmed.startsWith("/")) {
            return null;
        }
        return trimmed;
    }

    /**
     * Sanitizes phone numbers — strips everything except digits, spaces, +, -, (, ), .
     */
    public static String sanitizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        return PHONE_UNSAFE.matcher(phone.strip()).replaceAll("");
    }

    /**
     * Clamps an integer to [min, max].
     * Useful for pagination size/page parameters to prevent DoS via huge page sizes.
     */
    public static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
