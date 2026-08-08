package org.omnaphade.api_gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-key-at-least-32-characters-long";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
    }

    private String validToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder()
                .subject("4")
                .claim("role", "JOB_SEEKER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    @Test
    void publicPath_passesThroughWithoutToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void avatarGet_passesThroughWithoutToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/4/avatar");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void avatarPost_stillRequiresToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/4/avatar");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(eq(401), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void protectedPath_withoutToken_isRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/jobs/saved");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(eq(401), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void protectedPath_withValidToken_setsAttributesAndPasses() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/jobs/saved");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken());

        filter.doFilter(request, response, filterChain);

        verify(request).setAttribute("userId", "4");
        verify(request).setAttribute("role", "JOB_SEEKER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void protectedPath_withInvalidToken_isRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/jobs/saved");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-real-token");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(eq(401), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }
}
