package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.JacksonConfig;
import com.openroof.openroof.config.SecurityHeadersFilter;
import com.openroof.openroof.config.TestSecurityMocksConfig;
import com.openroof.openroof.dto.contract.ContractResponse;
import com.openroof.openroof.dto.contract.ContractStatusUpdateRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.ContractService;
import com.openroof.openroof.service.ContractPdfService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
@Import({SecurityConfig.class, JacksonConfig.class, TestSecurityMocksConfig.class})
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContractService contractService;

    @MockitoBean
    private ContractPdfService contractPdfService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private com.openroof.openroof.security.PropertyViewRateLimiter propertyViewRateLimiter;

    @MockitoBean
    private PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;

    @MockitoBean
    private SecurityHeadersFilter securityHeadersFilter;

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

    @Test
    @DisplayName("PATCH /{id}/status - Cambiar estado con USER debe fallar con 403")
    void updateStatus_withUser_returns403() throws Exception {
        ContractStatusUpdateRequest request = new ContractStatusUpdateRequest(ContractStatus.SENT);

        mockMvc.perform(patch("/contracts/1/status")
                        .with(user("buyer").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /{id}/status - Cambiar estado con AGENT retorna 200")
    void updateStatus_withAgent_returns200() throws Exception {
        ContractStatusUpdateRequest request = new ContractStatusUpdateRequest(ContractStatus.SENT);

        ContractResponse response = new ContractResponse(1L, 10L, "Prop", 20L, "Buyer", "b", 30L, "Seller", "s", null, null, null, null, ContractType.SALE, ContractStatus.SENT, new BigDecimal("150000"), null, null, "Terms", null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());

        when(contractService.updateStatus(eq(1L), any(ContractStatusUpdateRequest.class), eq("agent"))).thenReturn(response);

        mockMvc.perform(patch("/contracts/1/status")
                        .with(user("agent").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    @Test
    @DisplayName("GET /{id} - Obtener contrato existente retorna 200")
    void getById_returns200() throws Exception {
        ContractResponse response = new ContractResponse(1L, 10L, "Prop", 20L, "Buyer", "b", 30L, "Seller", "s", null, null, null, null, ContractType.SALE, ContractStatus.PARTIALLY_SIGNED, new BigDecimal("150000"), null, null, "Terms", null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());

        when(contractService.getById(eq(1L), eq("buyer"))).thenReturn(response);

        mockMvc.perform(get("/contracts/1")
                        .with(user("buyer").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_SIGNED"));
    }

    @Test
    @DisplayName("GET /{id} - Contrato sin permiso lanza BadRequestException")
    void getById_withoutPermission_returns400() throws Exception {
        when(contractService.getById(eq(1L), eq("buyer")))
                .thenThrow(new BadRequestException("No tiene permiso para ver este contrato"));

        mockMvc.perform(get("/contracts/1")
                        .with(user("buyer").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("permiso")));
    }

    @Test
    @DisplayName("GET /as-listing-agent - Lista contratos del agente listador retorna 200")
    void getAsListingAgent_returns200() throws Exception {
        when(contractService.getAsListingAgent(eq("agent"))).thenReturn(List.of());

        mockMvc.perform(get("/contracts/as-listing-agent")
                        .with(user("agent").roles("AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /{id} - Accept-Language mantiene el payload y resuelve el locale correcto")
    void getById_acceptLanguage_keepsPayload_andResolvesLocale() throws Exception {
        ContractResponse response = new ContractResponse(1L, 10L, "Prop", 20L, "Buyer", "b", 30L, "Seller", "s", null, null, null, null, ContractType.SALE, ContractStatus.PARTIALLY_SIGNED, new BigDecimal("150000"), null, null, "Terms", null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());

        List<Locale> seenLocales = new ArrayList<>();

        when(contractService.getById(eq(1L), eq("buyer"))).thenAnswer(invocation -> {
            seenLocales.add(LocaleContextHolder.getLocale());
            return response;
        });

        MvcResult en = mockMvc.perform(get("/contracts/1")
                        .with(user("buyer").roles("USER"))
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult pt = mockMvc.perform(get("/contracts/1")
                        .with(user("buyer").roles("USER"))
                        .header("Accept-Language", "pt"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode enBody = objectMapper.readTree(en.getResponse().getContentAsString()).get("data");
        JsonNode ptBody = objectMapper.readTree(pt.getResponse().getContentAsString()).get("data");

        assertThat(enBody).isEqualTo(ptBody);
        assertThat(seenLocales).containsExactly(Locale.forLanguageTag("en"), Locale.forLanguageTag("pt"));
    }
}
