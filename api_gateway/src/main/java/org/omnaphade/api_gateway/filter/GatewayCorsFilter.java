package org.omnaphade.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayCorsFilter extends OncePerRequestFilter {

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:Authorization,Content-Type,X-Trace-Id}")
    private String allowedHeaders;

    @Value("${app.cors.exposed-headers:X-Trace-Id}")
    private String exposedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age-seconds:3600}")
    private long maxAgeSeconds;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String origin = request.getHeader("Origin");

        if (!path.startsWith("/api/") || origin == null || origin.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Set<String> allowedOriginSet = parseCsv(allowedOrigins);
        if (!allowedOriginSet.contains(origin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CORS request");
            return;
        }

        Set<String> allowedMethodSet = parseCsvUpper(allowedMethods);
        Set<String> allowedHeaderSet = parseCsvLower(allowedHeaders);
        Set<String> exposedHeaderSet = parseCsv(exposedHeaders);

        response.setHeader("Vary", "Origin,Access-Control-Request-Method,Access-Control-Request-Headers");
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", String.valueOf(allowCredentials));
        response.setHeader("Access-Control-Expose-Headers", String.join(",", exposedHeaderSet));

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            String requestedMethod = request.getHeader("Access-Control-Request-Method");
            if (requestedMethod == null || !allowedMethodSet.contains(requestedMethod.toUpperCase(Locale.ROOT))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CORS request");
                return;
            }

            String requestedHeaders = request.getHeader("Access-Control-Request-Headers");
            if (requestedHeaders != null && !requestedHeaders.isBlank()) {
                Set<String> requestedHeaderSet = Arrays.stream(requestedHeaders.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
                if (!allowedHeaderSet.containsAll(requestedHeaderSet)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CORS request");
                    return;
                }
            }

            response.setHeader("Access-Control-Allow-Methods", String.join(",", allowedMethodSet));
            response.setHeader("Access-Control-Allow-Headers", String.join(",", allowedHeaderSet));
            response.setHeader("Access-Control-Max-Age", String.valueOf(maxAgeSeconds));
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Set<String> parseCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<String> parseCsvUpper(String csv) {
        return parseCsv(csv).stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Set<String> parseCsvLower(String csv) {
        return parseCsv(csv).stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
