package com.goaltracker.security;

import com.goaltracker.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiting filter for authentication endpoints using Bucket4j.
 * <ul>
 *     <li>/api/auth/login → 10 req/min</li>
 *     <li>/api/auth/register → 5 req/min</li>
 *     <li>/api/auth/forgot-password → 3 req/min</li>
 *     <li>/auth/login-custom → 10 req/min (Thymeleaf form)</li>
 *     <li>/auth/register → 5 req/min (Thymeleaf form)</li>
 *     <li>/auth/forgot-password → 3 req/min (Thymeleaf form)</li>
 * </ul>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int LOGIN_LIMIT = 10;
    private static final int REGISTER_LIMIT = 5;
    private static final int FORGOT_PASSWORD_LIMIT = 3;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> forgotPasswordBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        // Only rate limit POST requests
        if (!"POST".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        Bucket bucket = resolveBucket(path, clientIp);

        if (bucket != null && !bucket.tryConsume(1)) {
            log.warn("Rate limit aşıldı: IP={}, path={}", clientIp, path);
            throw new RateLimitExceededException(60);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !isRateLimitedPath(path);
    }

    private boolean isRateLimitedPath(String path) {
        return path.equals("/api/auth/login")
            || path.equals("/api/auth/register")
            || path.equals("/api/auth/forgot-password")
            || path.equals("/auth/login-custom")
            || path.equals("/auth/register")
            || path.equals("/auth/forgot-password");
    }

    private Bucket resolveBucket(String path, String clientIp) {
        if (path.contains("login")) {
            return loginBuckets.computeIfAbsent(clientIp, k -> createBucket(LOGIN_LIMIT));
        } else if (path.contains("register")) {
            return registerBuckets.computeIfAbsent(clientIp, k -> createBucket(REGISTER_LIMIT));
        } else if (path.contains("forgot-password")) {
            return forgotPasswordBuckets.computeIfAbsent(clientIp, k -> createBucket(FORGOT_PASSWORD_LIMIT));
        }
        return null;
    }

    private Bucket createBucket(int capacity) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, WINDOW)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

