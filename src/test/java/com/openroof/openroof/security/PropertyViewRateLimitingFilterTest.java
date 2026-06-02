package com.openroof.openroof.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.DelegatingServletOutputStream;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PropertyViewRateLimitingFilterTest {

    private PropertyViewRateLimiter rateLimiter;
    private ObjectMapper objectMapper;
    private PropertyViewRateLimitingFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(PropertyViewRateLimiter.class);
        objectMapper = new ObjectMapper();
        filter = new PropertyViewRateLimitingFilter(rateLimiter, objectMapper);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldNotFilterNonPostRequests() {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getRequestURI()).thenReturn("/api/properties/123/views");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterUnmatchedPaths() {
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getRequestURI()).thenReturn("/api/properties/123/recent-views");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldFilterMatchedPathWithoutApiPrefix() {
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getRequestURI()).thenReturn("/properties/123/views");

        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void shouldFilterMatchedPathWithApiPrefix() {
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getRequestURI()).thenReturn("/api/properties/123/views");

        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_whenAllowed_proceedsWithChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/properties/123/views");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isAllowed("1.2.3.4", "123")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_whenBlocked_returns429AndAborts() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/properties/123/views");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isAllowed("1.2.3.4", "123")).thenReturn(false);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(out));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
        
        String responseBody = out.toString();
        assertTrue(responseBody.contains("Too many view registration attempts"));
        assertTrue(responseBody.contains("\"success\":false"));
    }
}
