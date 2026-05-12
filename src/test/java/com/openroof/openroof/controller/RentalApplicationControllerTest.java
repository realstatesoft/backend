package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.JacksonConfig;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.TestSecurityMocksConfig;
import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.RentalApplicationResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.DepositStatus;
import com.openroof.openroof.model.enums.EmploymentStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.RentalApplicationStatus;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.RentalApplicationService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalApplicationController.class)
@Import({SecurityConfig.class, JacksonConfig.class, TestSecurityMocksConfig.class})
class RentalApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean private RentalApplicationService rentalApplicationService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthFilter;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean private com.openroof.openroof.security.PropertyViewRateLimiter propertyViewRateLimiter;

    @BeforeEach
    void bypassJwtFilter() throws Exception {
        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /rental-applications
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /rental-applications")
    class Submit {

        private final CreateRentalApplicationRequest request = new CreateRentalApplicationRequest(
                100L, "Me interesa el apartamento",
                new BigDecimal("3000.00"), EmploymentStatus.EMPLOYED,
                "Empresa SA", 2, false, true);

        @Test
        @DisplayName("Usuario autenticado envía solicitud y recibe 201")
        void submitReturns201() throws Exception {
            when(rentalApplicationService.submitApplication(any(), eq("tenant@test.com")))
                    .thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.SUBMITTED));

            mockMvc.perform(post("/rental-applications")
                            .with(user("tenant@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        }

        @Test
        @DisplayName("Aplicación duplicada activa retorna 400")
        void submitDuplicateReturns400() throws Exception {
            when(rentalApplicationService.submitApplication(any(), eq("tenant@test.com")))
                    .thenThrow(new BadRequestException("Ya tienes una aplicación activa para esta propiedad"));

            mockMvc.perform(post("/rental-applications")
                            .with(user("tenant@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /rental-applications/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /rental-applications/{id}")
    class GetById {

        @Test
        @DisplayName("Applicant autenticado puede ver su solicitud y recibe 200")
        void getByIdReturns200() throws Exception {
            when(rentalApplicationService.getApplication(1L, "tenant@test.com"))
                    .thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.SUBMITTED));

            mockMvc.perform(get("/rental-applications/1")
                            .with(user("tenant@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.propertyId").value(100));
        }

        @Test
        @DisplayName("Usuario sin permiso recibe 403")
        void getByIdForbiddenReturns403() throws Exception {
            when(rentalApplicationService.getApplication(1L, "other@test.com"))
                    .thenThrow(new ForbiddenException("Sin permiso"));

            mockMvc.perform(get("/rental-applications/1")
                            .with(user("other@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /rental-applications/property/{propertyId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /rental-applications/property/{propertyId}")
    class ListByProperty {

        @Test
        @DisplayName("Owner autenticado lista solicitudes sin filtro y recibe 200 con datos paginados")
        void listReturns200() throws Exception {
            var page = new PageImpl<>(List.of(
                    sampleApplicationResponse(1L, RentalApplicationStatus.SUBMITTED),
                    sampleApplicationResponse(2L, RentalApplicationStatus.UNDER_REVIEW)));

            when(rentalApplicationService.listApplications(eq(100L), eq(null), any(Pageable.class), eq("owner@test.com")))
                    .thenReturn(page);

            mockMvc.perform(get("/rental-applications/property/100")
                            .with(user("owner@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.page.totalElements").value(2));
        }

        @Test
        @DisplayName("Filtra por status cuando se pasa el parámetro")
        void listWithStatusFilterReturns200() throws Exception {
            var page = new PageImpl<>(List.of(
                    sampleApplicationResponse(1L, RentalApplicationStatus.APPROVED)));

            when(rentalApplicationService.listApplications(
                    eq(100L), eq(RentalApplicationStatus.APPROVED), any(Pageable.class), eq("owner@test.com")))
                    .thenReturn(page);

            mockMvc.perform(get("/rental-applications/property/100")
                            .param("status", "APPROVED")
                            .with(user("owner@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].status").value("APPROVED"));
        }

        @Test
        @DisplayName("Usuario sin permiso recibe 403")
        void listForbiddenReturns403() throws Exception {
            when(rentalApplicationService.listApplications(eq(100L), any(), any(), eq("tenant@test.com")))
                    .thenThrow(new ForbiddenException("Sin permiso"));

            mockMvc.perform(get("/rental-applications/property/100")
                            .with(user("tenant@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /rental-applications/{id}/approve
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /rental-applications/{id}/approve")
    class Approve {

        @Test
        @DisplayName("Owner aprueba la solicitud y recibe 200")
        void approveReturns200() throws Exception {
            when(rentalApplicationService.approveApplication(1L, "owner@test.com"))
                    .thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.APPROVED));

            mockMvc.perform(post("/rental-applications/1/approve")
                            .with(user("owner@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.message").value("Solicitud aprobada"));
        }

        @Test
        @DisplayName("Aprobar aplicación ya decidida retorna 400")
        void approveAlreadyDecidedReturns400() throws Exception {
            when(rentalApplicationService.approveApplication(1L, "owner@test.com"))
                    .thenThrow(new BadRequestException("La aplicación no puede ser aprobada en estado: APPROVED"));

            mockMvc.perform(post("/rental-applications/1/approve")
                            .with(user("owner@test.com").roles("USER")))
                    .andExpect(status().isBadRequest());
        }

    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /rental-applications/{id}/reject
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /rental-applications/{id}/reject")
    class Reject {

        @Test
        @DisplayName("Owner rechaza la solicitud con motivo y recibe 200")
        void rejectReturns200() throws Exception {
            when(rentalApplicationService.rejectApplication(1L, "Historial insuficiente", "owner@test.com"))
                    .thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.REJECTED));

            mockMvc.perform(post("/rental-applications/1/reject")
                            .param("reason", "Historial insuficiente")
                            .with(user("owner@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(jsonPath("$.message").value("Solicitud rechazada"));
        }

        @Test
        @DisplayName("Rechazar sin reason retorna 400")
        void rejectWithoutReasonReturns400() throws Exception {
            mockMvc.perform(post("/rental-applications/1/reject")
                            .with(user("owner@test.com").roles("USER")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Rechazar con reason en blanco retorna 400")
        void rejectWithBlankReasonReturns400() throws Exception {
            mockMvc.perform(post("/rental-applications/1/reject")
                            .param("reason", "   ")
                            .with(user("owner@test.com").roles("USER")))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /rental-applications/{id}/convert-to-lease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /rental-applications/{id}/convert-to-lease")
    class ConvertToLease {

        private final CreateLeaseRequest leaseRequest = new CreateLeaseRequest(
                100L, 10L, LeaseType.FIXED_TERM,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                BillingFrequency.MONTHLY, null, null, null);

        @Test
        @DisplayName("Owner convierte aplicación aprobada a lease y recibe 201")
        void convertReturns201() throws Exception {
            when(rentalApplicationService.convertToLease(eq(1L), any(CreateLeaseRequest.class), eq("owner@test.com")))
                    .thenReturn(sampleLeaseResponse(50L));

            mockMvc.perform(post("/rental-applications/1/convert-to-lease")
                            .with(user("owner@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(leaseRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(50))
                    .andExpect(jsonPath("$.message").value("Contrato creado correctamente"));
        }

        @Test
        @DisplayName("Aplicación no aprobada retorna 400")
        void convertNonApprovedReturns400() throws Exception {
            when(rentalApplicationService.convertToLease(eq(1L), any(), eq("owner@test.com")))
                    .thenThrow(new BadRequestException("Solo se pueden convertir a contrato las aplicaciones aprobadas"));

            mockMvc.perform(post("/rental-applications/1/convert-to-lease")
                            .with(user("owner@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(leaseRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Usuario sin permiso recibe 403")
        void convertForbiddenReturns403() throws Exception {
            when(rentalApplicationService.convertToLease(eq(1L), any(), eq("other@test.com")))
                    .thenThrow(new ForbiddenException("Sin permiso"));

            mockMvc.perform(post("/rental-applications/1/convert-to-lease")
                            .with(user("other@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(leaseRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private RentalApplicationResponse sampleApplicationResponse(Long id, RentalApplicationStatus status) {
        return new RentalApplicationResponse(
                id, 100L, "Apartamento Centro",
                10L, "Inquilino",
                status, "Me interesa",
                new BigDecimal("3000.00"), EmploymentStatus.EMPLOYED,
                2, false,
                LocalDateTime.now(), null, LocalDateTime.now());
    }

    private LeaseResponse sampleLeaseResponse(Long id) {
        return new LeaseResponse(
                id, 100L, "Apartamento Centro",
                20L, 10L, "Inquilino",
                LeaseType.FIXED_TERM, LeaseStatus.DRAFT,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                DepositStatus.HELD, BillingFrequency.MONTHLY,
                null, null, LocalDateTime.now());
    }
}
