package com.openroof.openroof.controller;

import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.SecurityHeadersFilter;
import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import com.openroof.openroof.security.ScreeningSecurity;
import com.openroof.openroof.service.TenantScreeningService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test que verifica que los endpoints scoped por application
 * ({@code /rentals/applications/{id}/screening}) están mapeados y exigen
 * autenticación (sin token → 401).
 *
 * <p>La lógica de autorización fina (owner/agent/admin) está cubierta en
 * {@link com.openroof.openroof.security.ScreeningSecurityTest}.
 * La lógica de delegación al service está cubierta en
 * {@link com.openroof.openroof.service.TenantScreeningServiceTest}.
 */
@WebMvcTest(controllers = TenantScreeningController.class)
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class,
        com.openroof.openroof.test.SliceSecurityBeans.class})
class TenantScreeningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private TenantScreeningService screeningService;
    @MockitoBean private ScreeningSecurity screeningSecurity;
    @MockitoBean private JwtAuthenticationFilter jwtAuthFilter;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean private PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;
    @MockitoBean private SecurityHeadersFilter securityHeadersFilter;

    private static final String BASE = "/rentals/applications/1/screening";

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(propertyViewRateLimitingFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(securityHeadersFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            HttpServletResponse res = inv.getArgument(1);
            res.setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    @Test
    @DisplayName("POST /rentals/applications/{id}/screening sin token → 401")
    void createForApplication_noToken_returns401() throws Exception {
        mockMvc.perform(post(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /rentals/applications/{id}/screening sin token → 401")
    void getForApplication_noToken_returns401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /rentals/applications/{id}/screening sin token → 401")
    void patchForApplication_noToken_returns401() throws Exception {
        mockMvc.perform(patch(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
