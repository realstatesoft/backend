package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.payment.PaymentRequest;
import com.openroof.openroof.dto.payment.PaymentResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.enums.PaymentType;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.PaymentService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class,
        com.openroof.openroof.config.TestSecurityMocksConfig.class})
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean PaymentService paymentService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean com.openroof.openroof.security.PropertyViewRateLimiter propertyViewRateLimiter;

    @BeforeEach
    void setupJwtFilterPassThrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse res = invocation.getArgument(1);
            res.setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PaymentResponse sampleResponse(Long id, PaymentStatus status) {
        return new PaymentResponse(id, 1L, "Juan Pérez", PaymentType.RESERVATION,
                status, "Señal de reserva", "uuid-" + id,
                new BigDecimal("500.00"), null, LocalDateTime.now(), LocalDateTime.now());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /payments")
    class CreatePayment {

        private final PaymentRequest validRequest = new PaymentRequest(
                PaymentType.RESERVATION, new BigDecimal("500.00"), "Señal de reserva", null);

        @Test
        @DisplayName("Usuario autenticado crea un pago y recibe 201")
        void authenticatedUser_returns201() throws Exception {
            when(paymentService.create(any(PaymentRequest.class), eq("user@test.com")))
                    .thenReturn(sampleResponse(1L, PaymentStatus.PENDING));

            mockMvc.perform(post("/payments")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.type").value("RESERVATION"))
                    .andExpect(jsonPath("$.message").value(containsString("registrado")));
        }

        @Test
        @DisplayName("Usuario no autenticado recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Request con tipo nulo devuelve 400")
        void nullType_returns400() throws Exception {
            String invalidBody = """
                    {"type": null, "amount": 500.00, "concept": "Test"}
                    """;
            mockMvc.perform(post("/payments")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Request con monto cero devuelve 400")
        void zeroAmount_returns400() throws Exception {
            String invalidBody = """
                    {"type": "RESERVATION", "amount": 0.00, "concept": "Test"}
                    """;
            mockMvc.perform(post("/payments")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Request con concepto vacío devuelve 400")
        void blankConcept_returns400() throws Exception {
            String invalidBody = """
                    {"type": "RESERVATION", "amount": 500.00, "concept": ""}
                    """;
            mockMvc.perform(post("/payments")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /payments/my")
    class GetMyPayments {

        @Test
        @DisplayName("Usuario autenticado recibe su lista de pagos paginada")
        void authenticatedUser_returns200() throws Exception {
            when(paymentService.getMyPayments(eq("user@test.com"), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleResponse(1L, PaymentStatus.PENDING))));

            mockMvc.perform(get("/payments/my")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.page.totalElements").value(1));
        }

        @Test
        @DisplayName("Filtra por estado cuando se pasa el parámetro status")
        void withStatusFilter_filtersResults() throws Exception {
            when(paymentService.getMyPayments(eq("user@test.com"), eq(PaymentStatus.APPROVED), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleResponse(2L, PaymentStatus.APPROVED))));

            mockMvc.perform(get("/payments/my")
                            .param("status", "APPROVED")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].status").value("APPROVED"));
        }

        @Test
        @DisplayName("Usuario no autenticado recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/payments/my"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /payments/{id}")
    class GetById {

        @Test
        @DisplayName("Usuario autenticado con acceso recibe el pago")
        void authenticatedOwner_returns200() throws Exception {
            when(paymentService.getById(eq(1L), eq("user@test.com")))
                    .thenReturn(sampleResponse(1L, PaymentStatus.PENDING));

            mockMvc.perform(get("/payments/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("ForbiddenException del service devuelve 403")
        void forbidden_returns403() throws Exception {
            when(paymentService.getById(eq(1L), eq("other@test.com")))
                    .thenThrow(new ForbiddenException("No tienes permiso para ver este pago"));

            mockMvc.perform(get("/payments/1")
                            .with(user("other@test.com").roles("USER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("permiso")));
        }

        @Test
        @DisplayName("ResourceNotFoundException del service devuelve 404")
        void notFound_returns404() throws Exception {
            when(paymentService.getById(eq(99L), eq("user@test.com")))
                    .thenThrow(new com.openroof.openroof.exception.ResourceNotFoundException("Pago no encontrado"));

            mockMvc.perform(get("/payments/99")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Pago no encontrado")));
        }

        @Test
        @DisplayName("Usuario no autenticado recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/payments/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /payments (ADMIN)")
    class GetAll {

        @Test
        @DisplayName("ADMIN puede listar todos los pagos")
        void admin_returns200() throws Exception {
            when(paymentService.getAll(isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(
                            sampleResponse(1L, PaymentStatus.PENDING),
                            sampleResponse(2L, PaymentStatus.APPROVED))));

            mockMvc.perform(get("/payments")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.page.totalElements").value(2));
        }

        @Test
        @DisplayName("ADMIN puede filtrar por userId y status")
        void admin_withFilters_returns200() throws Exception {
            when(paymentService.getAll(eq(1L), eq(PaymentStatus.PENDING), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleResponse(1L, PaymentStatus.PENDING))));

            mockMvc.perform(get("/payments")
                            .param("userId", "1")
                            .param("status", "PENDING")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("Usuario normal (USER) recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(get("/payments")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Sin autenticación recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/payments"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /payments/{id}/approve (ADMIN)")
    class ApprovePayment {

        @Test
        @DisplayName("ADMIN aprueba un pago PENDING y recibe 200")
        void admin_approvesPayment_returns200() throws Exception {
            when(paymentService.approvePayment(1L)).thenReturn(sampleResponse(1L, PaymentStatus.APPROVED));

            mockMvc.perform(post("/payments/1/approve")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.message").value(containsString("aprobado")));
        }

        @Test
        @DisplayName("Transición inválida devuelve 400")
        void invalidTransition_returns400() throws Exception {
            when(paymentService.approvePayment(1L))
                    .thenThrow(new BadRequestException("Transición de estado no permitida: APPROVED → APPROVED"));

            mockMvc.perform(post("/payments/1/approve")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Transición")));
        }

        @Test
        @DisplayName("Usuario normal (USER) recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(post("/payments/1/approve")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /payments/{id}/reject (ADMIN)")
    class RejectPayment {

        @Test
        @DisplayName("ADMIN rechaza un pago PENDING y recibe 200")
        void admin_rejectsPayment_returns200() throws Exception {
            when(paymentService.rejectPayment(1L)).thenReturn(sampleResponse(1L, PaymentStatus.REJECTED));

            mockMvc.perform(post("/payments/1/reject")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(jsonPath("$.message").value(containsString("rechazado")));
        }

        @Test
        @DisplayName("Transición inválida devuelve 400")
        void invalidTransition_returns400() throws Exception {
            when(paymentService.rejectPayment(1L))
                    .thenThrow(new BadRequestException("Transición de estado no permitida: REJECTED → REJECTED"));

            mockMvc.perform(post("/payments/1/reject")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Transición")));
        }

        @Test
        @DisplayName("Usuario normal (USER) recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(post("/payments/1/reject")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }
}
