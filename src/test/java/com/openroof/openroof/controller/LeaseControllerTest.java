package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.JacksonConfig;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.SecurityHeadersFilter;
import com.openroof.openroof.config.TestSecurityMocksConfig;
import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.LeaseSummaryResponse;
import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.mapper.RentalInstallmentMapper;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.DepositStatus;
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.security.LeaseSecurity;
import com.openroof.openroof.security.PropertySecurity;
import com.openroof.openroof.service.LeaseService;
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
import org.springframework.http.MediaType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaseController.class)
@Import({SecurityConfig.class, JacksonConfig.class, TestSecurityMocksConfig.class})
class LeaseControllerTest {

    private MockMvc mockMvc;
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private LeaseService leaseService;
    @MockitoBean private RentalInstallmentMapper installmentMapper;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private PropertySecurity propertySecurity;
    @MockitoBean(name = "leaseSecurity") private LeaseSecurity leaseSecurity;
    @MockitoBean private JwtAuthenticationFilter jwtAuthFilter;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private SecurityHeadersFilter securityHeadersFilter;
    @MockitoBean private com.openroof.openroof.security.PropertyViewRateLimiter propertyViewRateLimiter;
    @MockitoBean private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private User agentUser;
    private User tenantUser;

    private static final CreateLeaseRequest VALID_DTO = new CreateLeaseRequest(
            100L, 2L, LeaseType.FIXED_TERM,
            LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
            new BigDecimal("1000.00"), new BigDecimal("2000.00"),
            BillingFrequency.MONTHLY, null, null, null);

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        agentUser = User.builder().email("agent@test.com").passwordHash("h").role(UserRole.AGENT).build();
        agentUser.setId(1L);

        tenantUser = User.builder().email("tenant@test.com").passwordHash("h").role(UserRole.USER).build();
        tenantUser.setId(2L);

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agentUser));
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

    private Authentication agentAuth() {
        return UsernamePasswordAuthenticationToken.authenticated(agentUser, null, agentUser.getAuthorities());
    }

    private Authentication tenantAuth() {
        return UsernamePasswordAuthenticationToken.authenticated(tenantUser, null, tenantUser.getAuthorities());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/leases
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/leases")
    class Create {

        @Test
        @DisplayName("Owner/agente autorizado crea el lease y recibe 201")
        void createReturns201() throws Exception {
            when(leaseSecurity.canCreateLease(anyLong(), any())).thenReturn(true);
            when(leaseService.createLease(eq(1L), any(CreateLeaseRequest.class)))
                    .thenReturn(sampleLeaseResponse(10L, LeaseStatus.DRAFT));

            mockMvc.perform(post("/api/leases")
                            .with(authentication(agentAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(VALID_DTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.message").value("Contrato creado"));
        }

        @Test
        @DisplayName("Usuario sin permiso sobre la propiedad recibe 403")
        void createForbiddenReturns403() throws Exception {
            when(leaseSecurity.canCreateLease(anyLong(), any())).thenReturn(false);

            mockMvc.perform(post("/api/leases")
                            .with(authentication(agentAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(VALID_DTO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Request sin campos obligatorios retorna 400")
        void createWithInvalidBodyReturns400() throws Exception {
            mockMvc.perform(post("/api/leases")
                            .with(authentication(agentAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Sin autenticar recibe 401")
        void createUnauthenticatedReturns401() throws Exception {
            mockMvc.perform(post("/api/leases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(VALID_DTO)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/leases/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/leases/{id}")
    class GetById {

        @Test
        @DisplayName("Usuario autorizado obtiene el lease y recibe 200")
        void getByIdReturns200() throws Exception {
            when(leaseService.getLease(10L, 2L))
                    .thenReturn(sampleLeaseResponse(10L, LeaseStatus.ACTIVE));

            mockMvc.perform(get("/api/leases/10")
                            .with(authentication(tenantAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Sin autenticar recibe 401")
        void getByIdUnauthenticatedReturns401() throws Exception {
            mockMvc.perform(get("/api/leases/10"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/leases
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/leases")
    class ListLeases {

        @Test
        @DisplayName("Retorna página de leases del usuario autenticado")
        void listReturns200WithPage() throws Exception {
            var page = new PageImpl<>(java.util.List.of(sampleSummary()));
            when(leaseService.listLeases(eq(2L), eq(UserRole.USER), eq(null), eq(null), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/leases")
                            .with(authentication(tenantAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.page.totalElements").value(1));
        }

        @Test
        @DisplayName("Filtra por status y propertyId cuando se pasan como parámetros")
        void listWithFiltersReturns200() throws Exception {
            var page = new PageImpl<>(java.util.List.of(sampleSummary()));
            when(leaseService.listLeases(eq(1L), eq(UserRole.AGENT), eq(LeaseStatus.ACTIVE), eq(100L), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/leases")
                            .param("status", "ACTIVE")
                            .param("propertyId", "100")
                            .with(authentication(agentAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page.totalElements").value(1));
        }

        @Test
        @DisplayName("Sin autenticar recibe 401")
        void listUnauthenticatedReturns401() throws Exception {
            mockMvc.perform(get("/api/leases"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/leases/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/leases/{id}")
    class Update {

        @Test
        @DisplayName("Propietario autorizado actualiza el lease y recibe 200")
        void updateReturns200() throws Exception {
            when(leaseSecurity.canManageLease(eq(10L), any())).thenReturn(true);
            when(leaseService.updateLease(eq(10L), any(CreateLeaseRequest.class)))
                    .thenReturn(sampleLeaseResponse(10L, LeaseStatus.DRAFT));

            mockMvc.perform(patch("/api/leases/10")
                            .with(authentication(agentAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(VALID_DTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Contrato actualizado"));
        }

        @Test
        @DisplayName("Usuario sin permiso recibe 403")
        void updateForbiddenReturns403() throws Exception {
            when(leaseSecurity.canManageLease(eq(10L), any())).thenReturn(false);

            mockMvc.perform(patch("/api/leases/10")
                            .with(authentication(tenantAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(VALID_DTO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Lease no está en DRAFT retorna 400")
        void updateNonDraftReturns400() throws Exception {
            when(leaseSecurity.canManageLease(eq(10L), any())).thenReturn(true);
            when(leaseService.updateLease(eq(10L), any()))
                    .thenThrow(new BadRequestException("Lease can only be updated in DRAFT status"));

            mockMvc.perform(patch("/api/leases/10")
                            .with(authentication(agentAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(VALID_DTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/leases/{id}/activate
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/leases/{id}/activate")
    class Activate {

        @Test
        @DisplayName("Activa el lease y retorna lista de installments con 200")
        void activateReturns200WithInstallments() throws Exception {
            when(leaseSecurity.canManageLease(eq(10L), any())).thenReturn(true);
            when(leaseService.activateLease(10L)).thenReturn(List.of());
            when(installmentMapper.toResponseList(any())).thenReturn(List.of(sampleInstallmentResponse()));

            mockMvc.perform(post("/api/leases/10/activate")
                            .with(authentication(agentAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Contrato activado"))
                    .andExpect(jsonPath("$.data[0].leaseId").value(10));
        }

        @Test
        @DisplayName("Lease sin firmas retorna 400")
        void activateUnsignedReturns400() throws Exception {
            when(leaseSecurity.canManageLease(eq(10L), any())).thenReturn(true);
            when(leaseService.activateLease(10L))
                    .thenThrow(new BadRequestException("Lease must be signed by both parties before activation"));

            mockMvc.perform(post("/api/leases/10/activate")
                            .with(authentication(agentAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Usuario sin permiso recibe 403")
        void activateForbiddenReturns403() throws Exception {
            when(leaseSecurity.canManageLease(eq(10L), any())).thenReturn(false);

            mockMvc.perform(post("/api/leases/10/activate")
                            .with(authentication(tenantAuth())))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/leases/{id}/terminate
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/leases/{id}/terminate")
    class Terminate {

        @Test
        @DisplayName("Termina el lease y retorna 200")
        void terminateReturns200() throws Exception {
            when(leaseSecurity.canManageLease(eq(10L), any())).thenReturn(true);
            when(leaseService.terminateLease(10L))
                    .thenReturn(sampleLeaseResponse(10L, LeaseStatus.TERMINATED));

            mockMvc.perform(post("/api/leases/10/terminate")
                            .with(authentication(agentAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("TERMINATED"))
                    .andExpect(jsonPath("$.message").value("Contrato terminado"));
        }

        @Test
        @DisplayName("Lease en estado inválido retorna 400")
        void terminateInvalidStatusReturns400() throws Exception {
            when(leaseSecurity.canManageLease(eq(10L), any())).thenReturn(true);
            when(leaseService.terminateLease(10L))
                    .thenThrow(new BadRequestException("Only ACTIVE or EXPIRING_SOON leases can be terminated"));

            mockMvc.perform(post("/api/leases/10/terminate")
                            .with(authentication(agentAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Usuario sin permiso recibe 403")
        void terminateForbiddenReturns403() throws Exception {
            when(leaseSecurity.canManageLease(eq(10L), any())).thenReturn(false);

            mockMvc.perform(post("/api/leases/10/terminate")
                            .with(authentication(tenantAuth())))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private LeaseResponse sampleLeaseResponse(Long id, LeaseStatus status) {
        return new LeaseResponse(
                id, 100L, "Casa Test",
                1L, 2L, "Tenant",
                LeaseType.FIXED_TERM, status,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                DepositStatus.HELD, BillingFrequency.MONTHLY,
                null, null, LocalDateTime.now());
    }

    private LeaseSummaryResponse sampleSummary() {
        return new LeaseSummaryResponse(
                10L, "Casa Test", "Tenant",
                LeaseStatus.ACTIVE,
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12));
    }

    private RentalInstallmentResponse sampleInstallmentResponse() {
        return new RentalInstallmentResponse(
                1L, 10L, 1,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.now().plusMonths(1), null,
                InstallmentStatus.PENDING, null, LocalDateTime.now());
    }
}
