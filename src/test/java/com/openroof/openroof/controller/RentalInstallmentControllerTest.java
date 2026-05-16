package com.openroof.openroof.controller;

import com.openroof.openroof.config.JacksonConfig;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.TestSecurityMocksConfig;
import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.security.LeaseSecurity;
import com.openroof.openroof.security.PropertySecurity;
import com.openroof.openroof.service.RentalInstallmentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalInstallmentController.class)
@Import({SecurityConfig.class, JacksonConfig.class, TestSecurityMocksConfig.class})
class RentalInstallmentControllerTest {

    private MockMvc mockMvc;
    @Autowired private WebApplicationContext context;

    @MockitoBean private RentalInstallmentService installmentService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean(name = "leaseSecurity") private LeaseSecurity leaseSecurity;
    @MockitoBean private PropertySecurity propertySecurity;
    @MockitoBean private JwtAuthenticationFilter jwtAuthFilter;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private com.openroof.openroof.config.SecurityHeadersFilter securityHeadersFilter;
    @MockitoBean private com.openroof.openroof.security.PropertyViewRateLimiter propertyViewRateLimiter;
    @MockitoBean private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private User landlordUser;
    private User tenantUser;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        landlordUser = User.builder().email("landlord@test.com").passwordHash("h").role(UserRole.AGENT).build();
        landlordUser.setId(1L);

        tenantUser = User.builder().email("tenant@test.com").passwordHash("h").role(UserRole.USER).build();
        tenantUser.setId(2L);

        when(userRepository.findByEmail("landlord@test.com")).thenReturn(Optional.of(landlordUser));
        when(userRepository.findByEmail("tenant@test.com")).thenReturn(Optional.of(tenantUser));

        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(securityHeadersFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(inv -> {
            jakarta.servlet.http.HttpServletResponse res = inv.getArgument(1);
            res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    private Authentication landlordAuth() {
        return UsernamePasswordAuthenticationToken.authenticated(landlordUser, null, landlordUser.getAuthorities());
    }

    private Authentication tenantAuth() {
        return UsernamePasswordAuthenticationToken.authenticated(tenantUser, null, tenantUser.getAuthorities());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/leases/{id}/installments
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/leases/{id}/installments")
    class ListByLease {

        @Test
        @DisplayName("Retorna 200 con página de cuotas para usuario autorizado")
        void returns200WithPage() throws Exception {
            var page = new PageImpl<>(List.of(sampleResponse()));
            when(installmentService.listByLease(anyLong(), anyLong(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/leases/10/installments")
                            .with(authentication(tenantAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.page.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(50))
                    .andExpect(jsonPath("$.data.content[0].leaseId").value(10));
        }

        @Test
        @DisplayName("Retorna 200 con página vacía cuando el lease no tiene cuotas")
        void returns200WithEmptyPage() throws Exception {
            when(installmentService.listByLease(anyLong(), anyLong(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/leases/10/installments")
                            .with(authentication(landlordAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page.totalElements").value(0));
        }

        @Test
        @DisplayName("Sin autenticación retorna 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/leases/10/installments"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Usuario sin acceso al lease retorna 403")
        void accessDenied_returns403() throws Exception {
            when(installmentService.listByLease(anyLong(), anyLong(), any(Pageable.class)))
                    .thenThrow(new AccessDeniedException("sin acceso"));

            mockMvc.perform(get("/api/leases/10/installments")
                            .with(authentication(tenantAuth())))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/rentals/installments/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/rentals/installments/{id}")
    class GetById {

        @Test
        @DisplayName("Retorna 200 con el detalle de la cuota para usuario autorizado")
        void returns200WithInstallment() throws Exception {
            when(installmentService.getById(anyLong(), anyLong())).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/rentals/installments/50")
                            .with(authentication(tenantAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(50))
                    .andExpect(jsonPath("$.data.leaseId").value(10))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.totalAmount").value(1000.00));
        }

        @Test
        @DisplayName("Sin autenticación retorna 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/rentals/installments/50"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Cuota no encontrada retorna 404")
        void notFound_returns404() throws Exception {
            when(installmentService.getById(anyLong(), anyLong()))
                    .thenThrow(new ResourceNotFoundException("Installment", "id", 999L));

            mockMvc.perform(get("/api/rentals/installments/999")
                            .with(authentication(tenantAuth())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Usuario sin acceso al lease de la cuota retorna 403")
        void accessDenied_returns403() throws Exception {
            when(installmentService.getById(anyLong(), anyLong()))
                    .thenThrow(new AccessDeniedException("sin acceso"));

            mockMvc.perform(get("/api/rentals/installments/50")
                            .with(authentication(tenantAuth())))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private RentalInstallmentResponse sampleResponse() {
        return new RentalInstallmentResponse(
                50L, 10L, 1,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.now().plusMonths(1), null,
                InstallmentStatus.PENDING, null, LocalDateTime.now());
    }
}
