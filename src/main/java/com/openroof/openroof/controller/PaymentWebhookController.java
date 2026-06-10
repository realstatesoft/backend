package com.openroof.openroof.controller;

import com.openroof.openroof.dto.payment.BancardConfirmRequest;
import com.openroof.openroof.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook público que recibe el single_buy_confirm de Bancard vPOS.
 * Contrato Bancard: responder HTTP 200 con {"status":"success"} en menos de 30
 * segundos, SIEMPRE — incluso si el token es inválido o el pago no existe
 * (la reconciliación programada resuelve cualquier inconsistencia).
 * No usa ApiResponse porque el consumidor es Bancard, no el frontend.
 */
@RestController
@RequestMapping("/payments/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhooks", description = "Callbacks de pasarelas de pago")
public class PaymentWebhookController {

    private final PaymentGatewayService paymentGatewayService;

    @PostMapping("/bancard")
    @Operation(summary = "Confirmación de pago de Bancard vPOS (single_buy_confirm)",
            description = "Endpoint público invocado por Bancard (o por el mock en dev). Verifica el token md5 y aplica la confirmación. Responde 200 siempre.")
    public ResponseEntity<Map<String, String>> confirmBancard(@RequestBody BancardConfirmRequest request) {
        try {
            paymentGatewayService.processWebhookConfirmation(request != null ? request.operation() : null);
        } catch (Exception e) {
            // Nunca propagar: Bancard solo necesita el 200; el estado real se
            // resuelve vía reconciliación (get_single_buy_confirmation).
            log.error("Error procesando confirmación de Bancard", e);
        }
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
