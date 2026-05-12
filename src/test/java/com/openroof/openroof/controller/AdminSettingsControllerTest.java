package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.JacksonConfig;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.settings.AdminSettingsResponse;
import com.openroof.openroof.dto.settings.UpdateAdminCommissionsRequest;
import com.openroof.openroof.dto.settings.UpdateAdminPropertiesRequest;
import com.openroof.openroof.dto.settings.UpdateAdminReservationsRequest;
import com.openroof.openroof.dto.settings.UpdateAdminSystemRequest;
import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.AdminSettingsService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSettingsController.class)
@Import({SecurityConfig.class, JacksonConfig.class, com.openroof.openroof.test.SliceSecurityBeans.class})
class AdminSettingsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminSettingsService adminSettingsService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE = "/settings/admin";

    @BeforeEach
    void setupJwtFilterPassThrough() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(
                    inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(inv -> {
            ((jakarta.servlet.http.HttpServletResponse) inv.getArgument(1)).setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    private AdminSettingsResponse sampleSettings() {
        return new AdminSettingsResponse(
                new AdminSettingsResponse.CommissionSettings(
                        new BigDecimal("10.00"), new BigDecimal("5.00"), 1),
                new AdminSettingsResponse.ReservationSettings(72, new BigDecimal("1.00")),
                new AdminSettingsResponse.PropertySettings(15),
                new AdminSettingsResponse.SystemSettings("PYG")
        );
    }

    @Nested
    @DisplayName("GET /settings/admin")
    class GetSettings {

        @Test
        @DisplayName("ADMIN obtiene todos los ajustes agrupados → 200")
        void adminGetsSettings_returns200() throws Exception {
            when(adminSettingsService.getSettings()).thenReturn(sampleSettings());

            mockMvc.perform(get(BASE).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.commissions.saleCommissionPercent").value(10.00))
                    .andExpect(jsonPath("$.data.commissions.rentDepositMonths").value(1))
                    .andExpect(jsonPath("$.data.reservations.ttlHours").value(72))
                    .andExpect(jsonPath("$.data.properties.maxImages").value(15))
                    .andExpect(jsonPath("$.data.system.defaultCurrency").value("PYG"));
        }

        @Test
        @DisplayName("Sin autenticación → 401")
        void noAuth_returns401() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Rol AGENT → 403")
        void agentRole_returns403() throws Exception {
            mockMvc.perform(get(BASE).with(user("agent").roles("AGENT")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Rol USER → 403")
        void userRole_returns403() throws Exception {
            mockMvc.perform(get(BASE).with(user("buyer").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /settings/admin/commissions")
    class UpdateCommissions {

        @Test
        @DisplayName("ADMIN con payload válido → 200 con ajustes actualizados")
        void adminUpdatesCommissions_returns200() throws Exception {
            when(adminSettingsService.updateCommissions(any())).thenReturn(sampleSettings());

            var body = new UpdateAdminCommissionsRequest(
                    new BigDecimal("10.00"), new BigDecimal("5.00"), 1);

            mockMvc.perform(put(BASE + "/commissions")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Configuración de comisiones actualizada"));
        }

        @Test
        @DisplayName("Porcentaje fuera de rango (> 100) → 400")
        void outOfRangePercent_returns400() throws Exception {
            String body = """
                    {"saleCommissionPercent": 150.00, "rentCommissionPercent": 5.00, "rentDepositMonths": 1}
                    """;

            mockMvc.perform(put(BASE + "/commissions")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Meses de depósito fuera de rango (> 12) → 400")
        void depositMonthsOutOfRange_returns400() throws Exception {
            String body = """
                    {"saleCommissionPercent": 10.00, "rentCommissionPercent": 5.00, "rentDepositMonths": 13}
                    """;

            mockMvc.perform(put(BASE + "/commissions")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Sin autenticación → 401")
        void noAuth_returns401() throws Exception {
            mockMvc.perform(put(BASE + "/commissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Rol AGENT → 403")
        void agentRole_returns403() throws Exception {
            mockMvc.perform(put(BASE + "/commissions")
                            .with(user("agent").roles("AGENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /settings/admin/reservations")
    class UpdateReservations {

        @Test
        @DisplayName("ADMIN con payload válido → 200")
        void adminUpdatesReservations_returns200() throws Exception {
            when(adminSettingsService.updateReservations(any())).thenReturn(sampleSettings());

            var body = new UpdateAdminReservationsRequest(72, new BigDecimal("1.00"));

            mockMvc.perform(put(BASE + "/reservations")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("TTL de 0 horas → 400")
        void zeroTtl_returns400() throws Exception {
            String body = """
                    {"ttlHours": 0, "depositPercent": 1.00}
                    """;

            mockMvc.perform(put(BASE + "/reservations")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /settings/admin/properties")
    class UpdateProperties {

        @Test
        @DisplayName("ADMIN con payload válido → 200")
        void adminUpdatesProperties_returns200() throws Exception {
            when(adminSettingsService.updateProperties(any())).thenReturn(sampleSettings());

            var body = new UpdateAdminPropertiesRequest(15);

            mockMvc.perform(put(BASE + "/properties")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("maxImages = 0 → 400")
        void zeroImages_returns400() throws Exception {
            mockMvc.perform(put(BASE + "/properties")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"maxImages\": 0}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /settings/admin/system")
    class UpdateSystem {

        @Test
        @DisplayName("ADMIN con moneda válida → 200")
        void adminUpdatesSystem_returns200() throws Exception {
            when(adminSettingsService.updateSystem(any())).thenReturn(sampleSettings());

            var body = new UpdateAdminSystemRequest("PYG");

            mockMvc.perform(put(BASE + "/system")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Moneda con más de 3 caracteres → 400")
        void invalidCurrencyLength_returns400() throws Exception {
            mockMvc.perform(put(BASE + "/system")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"defaultCurrency\": \"DOLLARS\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Moneda vacía → 400")
        void blankCurrency_returns400() throws Exception {
            mockMvc.perform(put(BASE + "/system")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"defaultCurrency\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
