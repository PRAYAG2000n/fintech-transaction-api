package com.fintech.security;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private static final int MAX_PER_MINUTE = 100;
    private static final int BURST = 15;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientId = resolveClient(request);
        Bucket bucket = buckets.computeIfAbsent(clientId, this::newBucket);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long wait = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(wait));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate limit exceeded, retry after " + wait + "s\"}");
            log.warn("rate limited {}", clientId);
        }
    }

    private Bucket newBucket(String clientId) {
        log.debug("new bucket for {}", clientId);
        return Bucket.builder()
            .addLimit(Bandwidth.classic(MAX_PER_MINUTE, Refill.intervally(MAX_PER_MINUTE, Duration.ofMinutes(1))))
            .addLimit(Bandwidth.classic(BURST, Refill.intervally(BURST, Duration.ofSeconds(1))))
            .build();
    }

    private String resolveClient(HttpServletRequest request) {
        String userId = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        if (userId != null) return "user:" + userId;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null) return "ip:" + forwarded.split(",")[0].trim();
        return "ip:" + request.getRemoteAddr();
    }
}
