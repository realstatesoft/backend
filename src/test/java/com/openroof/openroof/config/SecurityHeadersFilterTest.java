package com.openroof.openroof.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
        request = new MockHttpServletRequest("GET", "/api/properties");
        response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(filter, "cspEnabled", false);
        ReflectionTestUtils.setField(filter, "cspApiPolicy", "");
    }

    @Test
    void alwaysSetsBaselineSecurityHeaders() throws Exception {
        filter.doFilterInternal(request, response, (req, res) -> {});

        assertEquals("strict-origin-when-cross-origin", response.getHeader("Referrer-Policy"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertNotNull(response.getHeader("Permissions-Policy"));
        assertNull(response.getHeader("Content-Security-Policy"));
    }

    @Test
    void setsApiCspWhenEnabled() throws Exception {
        ReflectionTestUtils.setField(filter, "cspEnabled", true);

        filter.doFilterInternal(request, response, (req, res) -> {});

        assertEquals(SecurityHeadersFilter.API_CSP_POLICY, response.getHeader("Content-Security-Policy"));
    }

    @Test
    void usesCustomApiCspPolicyWhenConfigured() throws Exception {
        ReflectionTestUtils.setField(filter, "cspEnabled", true);
        ReflectionTestUtils.setField(filter, "cspApiPolicy", "default-src 'none'; frame-ancestors 'none'");

        filter.doFilterInternal(request, response, (req, res) -> {});

        assertEquals("default-src 'none'; frame-ancestors 'none'", response.getHeader("Content-Security-Policy"));
    }
}
