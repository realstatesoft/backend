package com.openroof.openroof.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private UserService userService;
    private ObjectMapper objectMapper;
    private JwtAuthenticationFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        userService = mock(UserService.class);
        objectMapper = new ObjectMapper();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, userService, objectMapper);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldProceedWhenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldProceedWhenAuthHeaderDoesNotStartWithBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic user:pwd");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldHandleExpiredJwtGracefully() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired_token");
        when(jwtService.extractUsername("expired_token")).thenThrow(
                new ExpiredJwtException(null, null, "Token expired")
        );

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldHandleMalformedJwtGracefully() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer malformed_token");
        when(jwtService.extractUsername("malformed_token")).thenThrow(
                new MalformedJwtException("Malformed token")
        );

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAuthenticateValidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtService.extractUsername("valid_token")).thenReturn("user@test.com");
        
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid_token", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldReturn403WhenUserIsSuspended() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtService.extractUsername("valid_token")).thenReturn("suspended@test.com");

        User suspendedUser = mock(User.class);
        when(suspendedUser.getRole()).thenReturn(UserRole.USER);
        when(suspendedUser.getSuspendedUntil()).thenReturn(LocalDateTime.MAX);

        when(userDetailsService.loadUserByUsername("suspended@test.com")).thenReturn(suspendedUser);
        when(jwtService.isTokenValid("valid_token", suspendedUser)).thenReturn(true);
        when(userService.isUserSuspended(suspendedUser)).thenReturn(true);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(filterChain, never()).doFilter(request, response);
        verify(response).getOutputStream();
    }
}
