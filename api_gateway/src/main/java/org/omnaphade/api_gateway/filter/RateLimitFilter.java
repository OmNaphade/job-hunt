package org.omnaphade.api_gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.omnaphade.api_gateway.config.RateLimiterConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> STRICT_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    private final RateLimiterRegistry rateLimiterRegistry;
    private final MeterRegistry meterRegistry;

    public RateLimitFilter(RateLimiterRegistry rateLimiterRegistry, MeterRegistry meterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucket = STRICT_PATHS.contains(path)
                ? RateLimiterConfiguration.LOGIN_BUCKET
                : RateLimiterConfiguration.DEFAULT_BUCKET;
        String clientKey = resolveClientKey(request);

        RateLimiter limiter = rateLimiterRegistry.rateLimiter(bucket + ":" + clientKey, bucket);

        if (limiter.acquirePermission()) {
            filterChain.doFilter(request, response);
            return;
        }

        Counter.builder("gateway.rate_limit.rejected")
                .description("Requests rejected by the gateway rate limiter")
                .tag("bucket", bucket)
                .register(meterRegistry)
                .increment();

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(limiter.getRateLimiterConfig().getLimitRefreshPeriod().toSeconds()));
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"message\":\"Too many requests, please try again later\",\"status\":429,\"timestamp\":\"%s\"}",
                LocalDateTime.now()));
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
