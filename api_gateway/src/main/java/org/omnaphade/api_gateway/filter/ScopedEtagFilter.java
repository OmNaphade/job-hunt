package org.omnaphade.api_gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Spring's battle-tested {@link ShallowEtagHeaderFilter} (buffers the response, hashes it into a weak
 * ETag, and answers a matching {@code If-None-Match} with {@code 304 Not Modified}), scoped down to the
 * same public-GET allowlist as {@link CacheControlFilter} via {@link HttpCachePaths}. It already only
 * activates for 2xx GET responses internally, so scoping here is purely to avoid the buffering overhead
 * on routes that will never benefit (writes, per-user/auth-sensitive reads). Ordered after
 * {@link JwtAuthFilter} for the same reason as {@link CacheControlFilter} — see its Javadoc.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 11)
public class ScopedEtagFilter extends ShallowEtagHeaderFilter {

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !HttpCachePaths.isCacheable(request.getRequestURI());
    }
}
