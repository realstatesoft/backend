package com.openroof.openroof.controller;

import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.SecurityHeadersFilter;
import com.openroof.openroof.dto.payment.BancardConfirmOperation;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import com.openroof.openroof.service.PaymentGatewayService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentWebhookController.class)
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class,
        com.openroof.openroof.config.TestSecurityMocksConfig.class})
class PaymentWebhookControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean PaymentGatewayService paymentGatewayService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean com.openroof.openroof.security.PropertyViewRateLimiter propertyViewRateLimiter;
    @MockitoBean PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;
    @MockitoBean SecurityHeadersFilter securityHeadersFilter;
    @MockitoBean com.openroof.openroof.security.AuthRateLimiter authRateLimiter;

    // JSON real de single_buy_confirm según la spec v1.23 (shop_process_id numérico)
    private static final String CONFIRM_BODY = """
            {
                "operation": {
                    "token": "961babce3a9a0c19ae12f571be74d10d",
                    "shop_process_id": 873,
                    "response": "S",
                    "response_details": "respuesta S",
                    "extended_response_description": "respuesta extendida",
                    "currency": "PYG",
                    "amount": "350000.00",
                    "authorization_number": "123456",
                    "ticket_number": "123456789123456",
                    "response_code": "00",
                    "response_description": "Transacción aprobada.",
                    "security_information": {
                        "customer_ip": "123.123.123.123",
                        "card_source": "L",
                        "card_country": "Paraguay",
                        "version": "0.3",
                        "risk_index": "0"
                    }
                }
            }
            """;

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
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(propertyViewRateLimitingFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(securityHeadersFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    @DisplayName("Endpoint público: sin autenticación responde 200 {\"status\":\"success\"}")
    void publicEndpointReturns200() throws Exception {
        when(paymentGatewayService.processWebhookConfirmation(any())).thenReturn(true);

        mockMvc.perform(post("/payments/webhooks/bancard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONFIRM_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("Mapea el JSON de Bancard: shop_process_id numérico llega como String")
    void mapsBancardJson() throws Exception {
        when(paymentGatewayService.processWebhookConfirmation(any())).thenReturn(true);

        mockMvc.perform(post("/payments/webhooks/bancard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONFIRM_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<BancardConfirmOperation> captor =
                ArgumentCaptor.forClass(BancardConfirmOperation.class);
        verify(paymentGatewayService).processWebhookConfirmation(captor.capture());
        BancardConfirmOperation op = captor.getValue();
        assertThat(op.shopProcessId()).isEqualTo("873");
        assertThat(op.amount()).isEqualTo("350000.00");
        assertThat(op.responseCode()).isEqualTo("00");
        assertThat(op.authorizationNumber()).isEqualTo("123456");
        assertThat(op.ticketNumber()).isEqualTo("123456789123456");
        assertThat(op.token()).isEqualTo("961babce3a9a0c19ae12f571be74d10d");
    }

    @Test
    @DisplayName("Token inválido (service devuelve false): igual responde 200")
    void invalidTokenStillReturns200() throws Exception {
        when(paymentGatewayService.processWebhookConfirmation(any())).thenReturn(false);

        mockMvc.perform(post("/payments/webhooks/bancard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONFIRM_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("Excepción interna al procesar: igual responde 200 (contrato <30s de Bancard)")
    void internalErrorStillReturns200() throws Exception {
        when(paymentGatewayService.processWebhookConfirmation(any()))
                .thenThrow(new RuntimeException("fallo interno"));

        mockMvc.perform(post("/payments/webhooks/bancard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONFIRM_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
