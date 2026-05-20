package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.SecurityHeadersFilter;
import com.openroof.openroof.dto.subscription.SubscriptionPlanResponse;
import com.openroof.openroof.dto.subscription.SubscriptionResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.SubscriptionStatus;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import com.openroof.openroof.service.SubscriptionService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

@WebMvcTest(SubscriptionController.class)
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class,
        com.openroof.openroof.config.TestSecurityMocksConfig.class})
class SubscriptionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean SubscriptionService subscriptionService;
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

    private SubscriptionResponse sampleSub(Long id, SubscriptionStatus status) {
        SubscriptionPlanResponse plan = new SubscriptionPlanResponse(1L, "Básico", "Desc",
                new BigDecimal("150000.00"), 1, true, LocalDateTime.now(), LocalDateTime.now());
        return new SubscriptionResponse(id, 1L, "Test User", plan, status, 10L,
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1), null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /subscriptions (ADMIN)")
    class GetAll {

        @Test
        @DisplayName("ADMIN lista todas las suscripciones sin filtro")
        void admin_noFilter_returns200() throws Exception {
            when(subscriptionService.getAll(isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(
                            sampleSub(1L, SubscriptionStatus.ACTIVE),
                            sampleSub(2L, SubscriptionStatus.EXPIRED))));

            mockMvc.perform(get("/subscriptions")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.page.totalElements").value(2));
        }

        @Test
        @DisplayName("ADMIN filtra por estado ACTIVE")
        void admin_withStatusFilter_returns200() throws Exception {
            when(subscriptionService.getAll(eq(SubscriptionStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleSub(1L, SubscriptionStatus.ACTIVE))));

            mockMvc.perform(get("/subscriptions")
                            .param("status", "ACTIVE")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
        }

        @Test
        @DisplayName("USER recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(get("/subscriptions")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Sin autenticación recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/subscriptions"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /subscriptions/{id} (ADMIN)")
    class GetById {

        @Test
        @DisplayName("ADMIN puede ver suscripción por ID")
        void admin_returns200() throws Exception {
            when(subscriptionService.getById(1L)).thenReturn(sampleSub(1L, SubscriptionStatus.ACTIVE));

            mockMvc.perform(get("/subscriptions/1")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("Suscripción inexistente devuelve 404")
        void notFound_returns404() throws Exception {
            when(subscriptionService.getById(99L))
                    .thenThrow(new ResourceNotFoundException("Suscripción no encontrada"));

            mockMvc.perform(get("/subscriptions/99")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("USER recibe 403")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(get("/subscriptions/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Sin autenticación recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/subscriptions/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /subscriptions/my")
    class GetMySubscriptions {

        @Test
        @DisplayName("Usuario autenticado ve su historial paginado")
        void authenticated_returns200() throws Exception {
            when(subscriptionService.getMySubscriptions(eq("user@test.com"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleSub(1L, SubscriptionStatus.ACTIVE))));

            mockMvc.perform(get("/subscriptions/my")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.page.totalElements").value(1));
        }

        @Test
        @DisplayName("Sin autenticación recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/subscriptions/my"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /subscriptions/my/active")
    class GetMyActiveSubscription {

        @Test
        @DisplayName("Usuario con suscripción activa recibe los datos")
        void withActiveSub_returns200WithData() throws Exception {
            when(subscriptionService.getMyActiveSubscription("user@test.com"))
                    .thenReturn(Optional.of(sampleSub(1L, SubscriptionStatus.ACTIVE)));

            mockMvc.perform(get("/subscriptions/my/active")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Sin suscripción activa devuelve 200 con data nula y mensaje informativo")
        void noActiveSub_returns200WithNullData() throws Exception {
            when(subscriptionService.getMyActiveSubscription("user@test.com"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/subscriptions/my/active")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("Sin suscripción")));
        }

        @Test
        @DisplayName("Sin autenticación recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/subscriptions/my/active"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /subscriptions/{id}/cancel")
    class CancelSubscription {

        @Test
        @DisplayName("Propietario cancela su suscripción y recibe 200")
        void ownerCancels_returns200() throws Exception {
            when(subscriptionService.cancelSubscription(eq(1L), eq("user@test.com")))
                    .thenReturn(sampleSub(1L, SubscriptionStatus.CANCELLED));

            mockMvc.perform(post("/subscriptions/1/cancel")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.message").value(containsString("cancelada")));
        }

        @Test
        @DisplayName("ADMIN cancela suscripción ajena y recibe 200")
        void adminCancels_returns200() throws Exception {
            when(subscriptionService.cancelSubscription(eq(1L), eq("admin@test.com")))
                    .thenReturn(sampleSub(1L, SubscriptionStatus.CANCELLED));

            mockMvc.perform(post("/subscriptions/1/cancel")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Tercero sin permiso recibe 403")
        void forbidden_returns403() throws Exception {
            when(subscriptionService.cancelSubscription(eq(1L), eq("other@test.com")))
                    .thenThrow(new ForbiddenException("No tienes permiso para cancelar esta suscripción"));

            mockMvc.perform(post("/subscriptions/1/cancel")
                            .with(user("other@test.com").roles("USER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("permiso")));
        }

        @Test
        @DisplayName("Suscripción ya no activa devuelve 400")
        void alreadyCancelled_returns400() throws Exception {
            when(subscriptionService.cancelSubscription(eq(1L), eq("user@test.com")))
                    .thenThrow(new BadRequestException("Solo se pueden cancelar suscripciones en estado ACTIVE"));

            mockMvc.perform(post("/subscriptions/1/cancel")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("ACTIVE")));
        }

        @Test
        @DisplayName("Suscripción inexistente devuelve 404")
        void notFound_returns404() throws Exception {
            when(subscriptionService.cancelSubscription(eq(99L), eq("user@test.com")))
                    .thenThrow(new ResourceNotFoundException("Suscripción no encontrada"));

            mockMvc.perform(post("/subscriptions/99/cancel")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Sin autenticación recibe 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/subscriptions/1/cancel"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
