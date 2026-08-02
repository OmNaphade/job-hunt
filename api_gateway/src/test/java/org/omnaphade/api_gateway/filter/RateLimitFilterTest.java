package org.omnaphade.api_gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnaphade.api_gateway.config.RateLimiterConfiguration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SimpleMeterRegistry meterRegistry;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimiterConfig strictConfig = RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofSeconds(60))
                .timeoutDuration(Duration.ZERO)
                .build();
        RateLimiterConfig relaxedConfig = RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofSeconds(60))
                .timeoutDuration(Duration.ZERO)
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(relaxedConfig);
        registry.addConfiguration(RateLimiterConfiguration.LOGIN_BUCKET, strictConfig);
        registry.addConfiguration(RateLimiterConfiguration.DEFAULT_BUCKET, relaxedConfig);

        meterRegistry = new SimpleMeterRegistry();
        filter = new RateLimitFilter(registry, meterRegistry);
    }

    @Test
    void nonApiPath_bypassesRateLimiting() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void withinLimit_requestsPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/jobs");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void exceedingLimit_returns429AndBlocksChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/jobs");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilterInternal(request, response, filterChain); // 1st - allowed
        filter.doFilterInternal(request, response, filterChain); // 2nd - allowed
        filter.doFilterInternal(request, response, filterChain); // 3rd - blocked

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setHeader(eq("Retry-After"), any());
        assertThat(body.toString()).contains("\"status\":429");

        double rejected = meterRegistry.get("gateway.rate_limit.rejected")
                .tag("bucket", RateLimiterConfiguration.DEFAULT_BUCKET)
                .counter()
                .count();
        assertThat(rejected).isEqualTo(1.0);
    }

    @Test
    void differentClientIps_haveIndependentLimits() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        StringWriter body = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(body));

        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        when(request.getRemoteAddr()).thenReturn("10.0.0.4");
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void xForwardedForHeader_isUsedOverRemoteAddr() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/jobs");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response).setStatus(429);
        verify(request, never()).getRemoteAddr();
    }
}
