package com.example.multitenantapi.security;

import com.example.multitenantapi.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            Bucket bucket = buckets.computeIfAbsent(principal.getTenantId(), ignored -> newBucket());
            if (!bucket.tryConsume(1)) {
                response.setStatus(HTTP_TOO_MANY_REQUESTS);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiError err = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HTTP_TOO_MANY_REQUESTS)
                        .message("Rate limit exceeded for tenant")
                        .path(request.getRequestURI())
                        .build();
                response.getWriter().write(objectMapper.writeValueAsString(err));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
