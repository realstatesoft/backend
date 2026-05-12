package com.openroof.openroof.controller;

import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.LeaseSecurity;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import com.openroof.openroof.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica las reglas de SecurityConfig para los endpoints de leases / rentals /
 * lease-payments. AuthController se carga sólo para satisfacer el slice de
 * @WebMvcTest; el filtro de seguridad responde antes de cualquier dispatch al
 * controlador.
 */
@WebMvcTest(controllers = { AuthController.class })
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class})
class LeaseSecurityEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean private AuthService authService;
    @MockitoBean private LeaseSecurity leaseSecurity;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(propertyViewRateLimitingFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            jakarta.servlet.http.HttpServletResponse res = inv.getArgument(1);
            res.setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void leaseEndpoints_requireLogin() throws Exception {
        mockMvc.perform(get("/leases/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/leases")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/leases/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/leases/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void rentalEndpoints_requireLogin() throws Exception {
        mockMvc.perform(get("/rentals/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    void leasePaymentEndpoints_requireLogin() throws Exception {
        mockMvc.perform(get("/lease-payments/anything")).andExpect(status().isUnauthorized());
    }
}
