package com.example.multitenantapi.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Two independent token buckets:
 * <ul>
 *   <li>per tenant for authenticated traffic, so one noisy tenant cannot starve the others;</li>
 *   <li>per client IP for the public {@code /api/auth/**} endpoints, to slow down credential stuffing.</li>
 * </ul>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    private final Map<Long, Bucket> tenantBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ApiErrorWriter errorWriter;
    private final long tenantRequestsPerMinute;
    private final long authRequestsPerMinute;

    public RateLimitFilter(
            ApiErrorWriter errorWriter,
            @Value("${app.rate-limit.tenant-requests-per-minute:100}") long tenantRequestsPerMinute,
            @Value("${app.rate-limit.auth-requests-per-minute:20}") long authRequestsPerMinute
    ) {
        this.errorWriter = errorWriter;
        this.tenantRequestsPerMinute = tenantRequestsPerMinute;
        this.authRequestsPerMinute = authRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith(AUTH_PATH_PREFIX)) {
            Bucket bucket = authBuckets.computeIfAbsent(request.getRemoteAddr(), ignored -> newBucket(authRequestsPerMinute));
            if (!bucket.tryConsume(1)) {
                errorWriter.write(request, response, HTTP_TOO_MANY_REQUESTS, "Too many authentication attempts, try again later");
                return;
            }
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                Bucket bucket = tenantBuckets.computeIfAbsent(principal.getTenantId(), ignored -> newBucket(tenantRequestsPerMinute));
                if (!bucket.tryConsume(1)) {
                    errorWriter.write(request, response, HTTP_TOO_MANY_REQUESTS, "Rate limit exceeded for tenant");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private static Bucket newBucket(long perMinute) {
        Bandwidth limit = Bandwidth.classic(perMinute, Refill.intervally(perMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
