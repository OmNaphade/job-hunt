package org.omnaphade.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds a short-lived {@code Cache-Control} header to responses from the fixed public-GET allowlist in
 * {@link HttpCachePaths}. Header assignment happens *after* {@code filterChain.doFilter} returns, so it
 * naturally only fires for successful (2xx) responses — an auth failure earlier in the chain (e.g.
 * {@code JwtAuthFilter} rejecting a missing/invalid token) leaves the response status outside 2xx and this
 * filter is a no-op, regardless of where it sits in filter-chain order relative to auth.
 *
 * <p>{@code max-age} is intentionally short: this sits in front of each service's own Redis query cache,
 * which already absorbs the bulk of repeat-query load. The HTTP-level cache mainly saves gateway round
 * trips for identical client requests (e.g. re-rendering the same page of results), paired with
 * {@link ScopedEtagFilter} for full revalidation.
 */
@Component
public class CacheControlFilter extends OncePerRequestFilter {

    @Value("${app.http-cache.max-age-seconds:60}")
    private long maxAgeSeconds;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        boolean successfulGet = "GET".equalsIgnoreCase(request.getMethod())
                && response.getStatus() >= 200 && response.getStatus() < 300;
        if (successfulGet && HttpCachePaths.isCacheable(request.getRequestURI())) {
            response.setHeader("Cache-Control", "public, max-age=" + maxAgeSeconds + ", must-revalidate");
            response.setHeader("Vary", "Authorization");
        }
    }
}
