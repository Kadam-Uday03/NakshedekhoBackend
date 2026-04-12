package com.nakshedekho.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-tier rate limiting filter — abuse protection for all API endpoints.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ TIER 1 — CRITICAL  (auth operations)                                   │
 * │   /api/auth/login             →  3 req / min  per IP   (brute-force)  │
 * │   /api/auth/forgot-password   →  3 req / 5min per IP   (reset abuse)  │
 * │   /api/auth/reset-password    →  5 req / min  per IP                  │
 * │                                                                         │
 * │ TIER 2 — STRICT   (account creation / OTP)                            │
 * │   /api/auth/register          →  5 req / min  per IP   (bot signup)   │
 * │   /api/auth/send-otp          →  5 req / min  per IP                  │
 * │   /api/auth/verify            →  5 req / min  per IP                  │
 * │   /api/auth/google            →  5 req / min  per IP                  │
 * │   /api/public/contact         →  3 req / 5min per IP   (spam form)    │
 * │                                                                         │
 * │ TIER 3 — STANDARD (general API)                                        │
 * │   /api/customer/**            → 60 req / min  per IP                  │
 * │   /api/manager/**             → 60 req / min  per IP                  │
 * │   /api/owner/**               → 60 req / min  per IP                  │
 * │   /api/blog/**                → 30 req / min  per IP                  │
 * │   /api/payment/**             → 20 req / min  per IP                  │
 * │   /api/files/**               → 30 req / min  per IP                  │
 * │                                                                         │
 * │ TIER 4 — PUBLIC   (scraping protection)                                │
 * │   /api/public/**              → 30 req / min  per IP   (catalogue)    │
 * │   /api/public/blogs/**        → 60 req / min  per IP   (blog reads)   │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Design decisions:
 * - Per (IP + path-prefix) buckets so limits are isolated across
 *   different endpoint groups
 * - ConcurrentHashMap is safe for concurrent filter invocations
 * - Retry-After header always set (RFC 6585)
 * - Suspicious IPs (>3 blocked requests) are logged at WARN level
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // ── Bucket stores — one per (IP : tier-key) ───────────────────────────
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Track consecutive 429s per IP for abuse logging
    private final Map<String, Integer> blockedCount = new ConcurrentHashMap<>();

    // ── Tier definitions ──────────────────────────────────────────────────

    private enum Tier {
        CRITICAL,   // 3/min  for login / forgot-password
        STRICT,     // 5/min  for register / OTP
        CONTACT,    // 3 per 5min for contact form (spam)
        PAYMENT,    // 20/min for payment operations
        BLOG,       // 30/min for blog / file operations
        STANDARD,   // 60/min for authenticated API
        PUBLIC      // 30/min for public catalogue/blog reads
    }

    // ── Bucket factories ─────────────────────────────────────────────────

    private Bucket newBucket(Tier tier) {
        Bandwidth limit = switch (tier) {
            case CRITICAL  -> Bandwidth.classic(3,  Refill.intervally(3,  Duration.ofMinutes(1)));
            case STRICT    -> Bandwidth.classic(5,  Refill.intervally(5,  Duration.ofMinutes(1)));
            case CONTACT   -> Bandwidth.classic(3,  Refill.intervally(3,  Duration.ofMinutes(5)));
            case PAYMENT   -> Bandwidth.classic(20, Refill.greedy(20,     Duration.ofMinutes(1)));
            case BLOG      -> Bandwidth.classic(30, Refill.greedy(30,     Duration.ofMinutes(1)));
            case STANDARD  -> Bandwidth.classic(60, Refill.greedy(60,     Duration.ofMinutes(1)));
            case PUBLIC    -> Bandwidth.classic(30, Refill.greedy(30,     Duration.ofMinutes(1)));
        };
        return Bucket.builder().addLimit(limit).build();
    }

    // ── URI → Tier routing ────────────────────────────────────────────────

    /**
     * Returns the Tier for a given URI, or null if the URI is not rate-limited
     * (e.g. static assets served directly by Spring).
     */
    private Tier resolveTier(String uri) {
        // Tier 1 — critical auth
        if (uri.equals("/api/auth/login"))           return Tier.CRITICAL;
        if (uri.equals("/api/auth/forgot-password")) return Tier.CRITICAL;

        // Tier 2 — strict account/otp
        if (uri.equals("/api/auth/register"))        return Tier.STRICT;
        if (uri.equals("/api/auth/send-otp"))        return Tier.STRICT;
        if (uri.equals("/api/auth/verify"))          return Tier.STRICT;
        if (uri.equals("/api/auth/google"))          return Tier.STRICT;
        if (uri.equals("/api/auth/reset-password"))  return Tier.STRICT;

        // Contact form spam protection
        if (uri.equals("/api/public/contact"))       return Tier.CONTACT;

        // Tier 3 — payment (higher sensitivity)
        if (uri.startsWith("/api/payment/"))         return Tier.PAYMENT;

        // Tier 3 — blog / files
        if (uri.startsWith("/api/blog/"))            return Tier.BLOG;
        if (uri.startsWith("/api/files/"))           return Tier.BLOG;

        // Tier 3 — authenticated user APIs
        if (uri.startsWith("/api/customer/"))        return Tier.STANDARD;
        if (uri.startsWith("/api/manager/"))         return Tier.STANDARD;
        if (uri.startsWith("/api/owner/"))           return Tier.STANDARD;

        // Tier 4 — public catalogue/blog scraping protection
        if (uri.startsWith("/api/public/"))          return Tier.PUBLIC;

        // Not an API path (static files, health checks) — no limit
        return null;
    }

    // ── Retry-After seconds per tier ─────────────────────────────────────

    private int retryAfterSeconds(Tier tier) {
        return switch (tier) {
            case CONTACT -> 300;   // 5 minutes
            default      -> 60;
        };
    }

    // ── Filter logic ──────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri       = request.getRequestURI();
        Tier   tier      = resolveTier(uri);

        // Not rate-limited — pass through immediately
        if (tier == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp  = getClientIP(request);
        // Bucket key: IP + tier "prefix" (not full URI, so /api/customer/** shares one bucket)
        String bucketKey = clientIp + ":" + tier.name();

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> newBucket(tier));

        if (bucket.tryConsume(1)) {
            // Reset blocked counter on success
            blockedCount.remove(clientIp);
            filterChain.doFilter(request, response);
        } else {
            // Track repeated violations for abuse logging
            int violations = blockedCount.compute(clientIp, (k, count) -> count == null ? 1 : count + 1);
            if (violations >= 5) {
                logger.warn("ABUSE: IP {} has been rate-limited {} times (latest: {} [{}])",
                        clientIp, violations, uri, tier.name());
            }

            int retryAfter = retryAfterSeconds(tier);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setHeader("X-RateLimit-Limit", tierLimit(tier));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.getWriter().write(
                    "{\"error\":\"Too many requests. Please wait " + retryAfter + " seconds before retrying.\"}");
        }
    }

    /** Human readable limit string for X-RateLimit-Limit header. */
    private String tierLimit(Tier tier) {
        return switch (tier) {
            case CRITICAL  -> "3 per minute";
            case STRICT    -> "5 per minute";
            case CONTACT   -> "3 per 5 minutes";
            case PAYMENT   -> "20 per minute";
            case BLOG      -> "30 per minute";
            case STANDARD  -> "60 per minute";
            case PUBLIC    -> "30 per minute";
        };
    }

    /** Extracts the real client IP, honouring reverse-proxy headers. */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // First IP in the chain is the original client
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
