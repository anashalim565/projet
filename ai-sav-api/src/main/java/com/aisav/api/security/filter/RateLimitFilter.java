package com.aisav.api.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/api/auth/")
                || "/actuator/health".equals(path);
    }

    private Bucket getBucket(String userId) {
        return buckets.computeIfAbsent(userId, ignored ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(60)
                                .refillGreedy(60, Duration.ofMinutes(1))
                                .build())
                        .build()
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            chain.doFilter(req, res);
            return;
        }

        Bucket bucket = getBucket(authentication.getName());

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
            return;
        }

        res.setStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value());        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"error\":\"Too many requests\",\"retryAfter\":60}");
    }
}