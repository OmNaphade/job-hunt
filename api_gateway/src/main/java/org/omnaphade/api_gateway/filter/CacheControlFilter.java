package org.omnaphade.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds a short-lived {@code Cache-Control} header to responses from the fixed public-GET allowlist in
 * {@link HttpCachePaths}. Explicitly ordered after {@link JwtAuthFilter} (see its Javadoc): this filter
 * must never even run for a request auth already rejected, since Spring Cloud Gateway MVC streams and
 * commits the proxied backend response as it's routed — by the time {@code filterChain.doFilter} returns
 * for a successful request, it's too late to add headers, so this filter sets the header *before*
 * delegating and relies on chain order (not response state) to keep rejected/error requests uncached.
 *
 * <p>{@code max-age} is intentionally short: this sits in front of each service's own Redis query cache,
 * which already absorbs the bulk of repeat-query load. The HTTP-level cache mainly saves gateway round
 * trips for identical client requests (e.g. re-rendering the same page of results), paired with
 * {@link ScopedEtagFilter} for full revalidation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CacheControlFilter extends OncePerRequestFilter {

    @Value("${app.http-cache.max-age-seconds:60}")
    private long maxAgeSeconds;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        boolean cacheableRoute = "GET".equalsIgnoreCase(request.getMethod())
                && HttpCachePaths.isCacheable(request.getRequestURI());

        if (cacheableRoute) {
            response.setHeader("Cache-Control", "public, max-age=" + maxAgeSeconds + ", must-revalidate");
            response.setHeader("Vary", "Authorization");
        }

        filterChain.doFilter(request, response);

        int status = response.getStatus();
        boolean okOrNotModified = (status >= 200 && status < 300) || status == 304;
        if (cacheableRoute && !okOrNotModified && !response.isCommitted()) {
            response.setHeader("Cache-Control", "no-store");
        }
    }
}
