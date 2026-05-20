package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.SecurityHeadersFilter;
import com.openroof.openroof.dto.subscription.SubscriptionPlanRequest;
import com.openroof.openroof.dto.subscription.SubscriptionPlanResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ConflictException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import com.openroof.openroof.service.SubscriptionPlanService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionPlanController.class)
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class,
        com.openroof.openroof.config.TestSecurityMocksConfig.class})
class SubscriptionPlanControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean SubscriptionPlanService subscriptionPlanService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean com.openroof.openroof.security.PropertyViewRateLimiter propertyViewRateLimiter;
    @MockitoBean PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;
    @MockitoBean SecurityHeadersFilter securityHeadersFilter;

    @BeforeEach
    void setupFiltersPassThrough() throws Exception {
        doAnswer(inv -> { ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        doAnswer(inv -> { ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(propertyViewRateLimitingFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        doAnswer(inv -> { ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(securityHeadersFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse res = invocation.getArgument(1);
            res.setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    private SubscriptionPlanResponse planResponse(Long id, String name, boolean active) {
        return new SubscriptionPlanResponse(id, name, "Descripción",
                new BigDecimal("150000.00"), 1, active, LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /subscription-plans (público)")
    class GetActivePlans {

        @Test
        @DisplayName("Sin autenticación devuelve planes activos con 200")
        void publicAccess_returns200() throws Exception {
            when(subscriptionPlanService.getAll(eq(true), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(planResponse(1L, "Básico", true))));

            mockMvc.perform(get("/subscription-plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].name").value("Básico"))
                    .andExpect(jsonPath("$.data.content[0].active").value(true));
        }

        @Test
        @DisplayName("Lista vacía devuelve 200 con contenido vacío")
        void emptyList_returns200() throws Exception {
            when(subscriptionPlanService.getAll(eq(true), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/subscription-plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /subscription-plans/{id} (público)")
    class GetById {

        @Test
        @DisplayName("Plan activo existente devuelve 200")
        void found_returns200() throws Exception {
            when(subscriptionPlanService.getActiveById(1L)).thenReturn(planResponse(1L, "Básico", true));

            mockMvc.perform(get("/subscription-plans/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("Básico"));
        }

        @Test
        @DisplayName("Plan inexistente o inactivo devuelve 404")
        void notFound_returns404() throws Exception {
            when(subscriptionPlanService.getActiveById(99L))
                    .thenThrow(new ResourceNotFoundException("Plan de suscripción no encontrado"));

            mockMvc.perform(get("/subscription-plans/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /subscription-plans/admin (ADMIN)")
    class GetAllAdmin {

        @Test
        @DisplayName("ADMIN puede listar todos los planes sin filtro")
        void admin_noFilter_returns200() throws Exception {
            when(subscriptionPlanService.getAll(isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(
                            planResponse(1L, "Básico", true),
                            planResponse(2L, "Antiguo", false))));

            mockMvc.perform(get("/subscription-plans/admin")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page.totalElements").value(2));
        }

        @Test
        @DisplayName("ADMIN puede filtrar por active=false")
        void admin_withActiveFilter_returns200() throws Exception {
            when(subscriptionPlanService.getAll(eq(false), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(planResponse(2L, "Antiguo", false))));

            mockMvc.perform(get("/subscription-plans/admin")
                            .param("active", "false")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].active").value(false));
        }

        @Test
        @DisplayName("USER recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(get("/subscription-plans/admin")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Sin autenticación recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/subscription-plans/admin"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /subscription-plans (ADMIN)")
    class CreatePlan {

        private final SubscriptionPlanRequest validRequest = new SubscriptionPlanRequest(
                "Premium", "Plan premium", new BigDecimal("250000.00"), 3, true);

        @Test
        @DisplayName("ADMIN crea plan y recibe 201")
        void admin_creates_returns201() throws Exception {
            when(subscriptionPlanService.create(any())).thenReturn(planResponse(1L, "Premium", true));

            mockMvc.perform(post("/subscription-plans")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Premium"))
                    .andExpect(jsonPath("$.message").value(containsString("creado")));
        }

        @Test
        @DisplayName("Nombre duplicado devuelve 409")
        void duplicateName_returns409() throws Exception {
            when(subscriptionPlanService.create(any()))
                    .thenThrow(new ConflictException("Ya existe un plan con el nombre: Premium"));

            mockMvc.perform(post("/subscription-plans")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Nombre vacío devuelve 400")
        void blankName_returns400() throws Exception {
            String invalidBody = """
                    {"name": "", "price": 150000.00, "durationMonths": 1}
                    """;
            mockMvc.perform(post("/subscription-plans")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Precio cero devuelve 400")
        void zeroPrice_returns400() throws Exception {
            String invalidBody = """
                    {"name": "Plan", "price": 0.00, "durationMonths": 1}
                    """;
            mockMvc.perform(post("/subscription-plans")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("USER recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(post("/subscription-plans")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Sin autenticación recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/subscription-plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /subscription-plans/{id} (ADMIN)")
    class UpdatePlan {

        private final SubscriptionPlanRequest updateRequest = new SubscriptionPlanRequest(
                "Actualizado", "Nueva descripción", new BigDecimal("300000.00"), 6, true);

        @Test
        @DisplayName("ADMIN actualiza plan y recibe 200")
        void admin_updates_returns200() throws Exception {
            when(subscriptionPlanService.update(eq(1L), any())).thenReturn(planResponse(1L, "Actualizado", true));

            mockMvc.perform(put("/subscription-plans/1")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Actualizado"))
                    .andExpect(jsonPath("$.message").value(containsString("actualizado")));
        }

        @Test
        @DisplayName("Plan inexistente devuelve 404")
        void notFound_returns404() throws Exception {
            when(subscriptionPlanService.update(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Plan no encontrado"));

            mockMvc.perform(put("/subscription-plans/99")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Nombre duplicado devuelve 409")
        void duplicateName_returns409() throws Exception {
            when(subscriptionPlanService.update(eq(1L), any()))
                    .thenThrow(new ConflictException("Ya existe un plan con ese nombre"));

            mockMvc.perform(put("/subscription-plans/1")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("USER recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(put("/subscription-plans/1")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /subscription-plans/{id}/deactivate (ADMIN)")
    class DeactivatePlan {

        @Test
        @DisplayName("ADMIN desactiva plan y recibe 200 con active=false")
        void admin_deactivates_returns200() throws Exception {
            when(subscriptionPlanService.deactivate(1L)).thenReturn(planResponse(1L, "Básico", false));

            mockMvc.perform(post("/subscription-plans/1/deactivate")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.active").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("desactivado")));
        }

        @Test
        @DisplayName("Plan inexistente devuelve 404")
        void notFound_returns404() throws Exception {
            when(subscriptionPlanService.deactivate(99L))
                    .thenThrow(new ResourceNotFoundException("Plan no encontrado"));

            mockMvc.perform(post("/subscription-plans/99/deactivate")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("USER recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(post("/subscription-plans/1/deactivate")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /subscription-plans/{id} (ADMIN)")
    class DeletePlan {

        @Test
        @DisplayName("ADMIN elimina plan sin suscripciones activas y recibe 200")
        void admin_deletes_returns200() throws Exception {
            mockMvc.perform(delete("/subscription-plans/1")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("eliminado")));
        }

        @Test
        @DisplayName("Plan con suscripciones activas devuelve 400")
        void withActiveSubs_returns400() throws Exception {
            doThrow(new BadRequestException("tiene suscripciones activas"))
                    .when(subscriptionPlanService).delete(1L);

            mockMvc.perform(delete("/subscription-plans/1")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Plan inexistente devuelve 404")
        void notFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Plan no encontrado"))
                    .when(subscriptionPlanService).delete(99L);

            mockMvc.perform(delete("/subscription-plans/99")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("USER recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(delete("/subscription-plans/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Sin autenticación recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete("/subscription-plans/1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
