package org.omnaphade.api_gateway.filter;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The fixed allowlist of confirmed-public, non-{@code @PreAuthorize} GET routes that
 * {@link CacheControlFilter} and {@link ScopedEtagFilter} are allowed to add caching headers to. Kept as
 * exact/precise regexes (not broad prefix wildcards) so per-user routes that happen to share a path
 * prefix — e.g. {@code /api/jobs/saved} (JOB_SEEKER-only) sitting under {@code /api/jobs/*} — are never
 * accidentally swept in.
 */
final class HttpCachePaths {

    private static final List<Pattern> CACHEABLE_PATTERNS = List.of(
            Pattern.compile("^/api/jobs$"),
            Pattern.compile("^/api/jobs/search$"),
            Pattern.compile("^/api/jobs/\\d+$"),
            Pattern.compile("^/api/jobs/company/\\d+$"),
            Pattern.compile("^/api/companies$"),
            Pattern.compile("^/api/companies/\\d+$"),
            Pattern.compile("^/api/companies/\\d+/recruiters$"),
            Pattern.compile("^/api/users/skills$")
    );

    private HttpCachePaths() {
    }

    static boolean isCacheable(String requestUri) {
        return CACHEABLE_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(requestUri).matches());
    }

}
