package com.stockguard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding window rate limiter filter for sensitive endpoints.
 * Protects login, registration, and notification dispatch from brute force and flooding.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Value("${stockguard.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${stockguard.security.rate-limit.login-max:10}")
    private int loginMaxRequests;

    @Value("${stockguard.security.rate-limit.notifications-max:20}")
    private int notificationMaxRequests;

    private static final long WINDOW_MS = 60_000L; // 1 minute window

    private static class RequestCounter {
        final long windowStart;
        final AtomicInteger count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Only rate-limit POST actions on sensitive endpoints
        if ("POST".equalsIgnoreCase(method)) {
            if (uri.endsWith("/api/auth/login")) {
                if (isLimitExceeded(getClientIp(request), "LOGIN", loginMaxRequests)) {
                    sendRateLimitError(response, "Too many login attempts. Please wait 60 seconds.");
                    return;
                }
            } else if (uri.endsWith("/api/auth/register")) {
                if (isLimitExceeded(getClientIp(request), "REGISTER", loginMaxRequests)) {
                    sendRateLimitError(response, "Too many registration attempts. Please wait 60 seconds.");
                    return;
                }
            } else if (uri.endsWith("/api/notifications/send")) {
                if (isLimitExceeded(getClientIp(request), "NOTIF", notificationMaxRequests)) {
                    sendRateLimitError(response, "Too many notification requests. Please wait 60 seconds.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLimitExceeded(String clientIp, String action, int maxRequests) {
        long now = System.currentTimeMillis();
        String key = action + ":" + clientIp;

        requestCounts.compute(key, (k, counter) -> {
            if (counter == null || (now - counter.windowStart) > WINDOW_MS) {
                return new RequestCounter(now);
            }
            counter.count.incrementAndGet();
            return counter;
        });

        RequestCounter current = requestCounts.get(key);
        if (current == null) {
            return false;
        }
        boolean exceeded = current.count.get() > maxRequests;
        if (exceeded) {
            log.warn("Rate limit exceeded for IP {} on action {}. Count: {}", clientIp, action, current.count.get());
        }
        return exceeded;
    }

    private void sendRateLimitError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> errorBody = Map.of(
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", "Too Many Requests",
                "message", message
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }

    public static String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    public void resetLimits() {
        requestCounts.clear();
    }
}
