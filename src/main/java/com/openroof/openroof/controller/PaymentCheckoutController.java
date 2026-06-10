package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.payment.CheckoutRequest;
import com.openroof.openroof.dto.payment.CheckoutResponse;
import com.openroof.openroof.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Gestión de pagos")
public class PaymentCheckoutController {

    private final PaymentGatewayService paymentGatewayService;

    @PostMapping("/{id}/checkout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Iniciar checkout de pasarela para un pago propio en estado PENDING",
            description = "Devuelve el process_id de Bancard vPOS (o del mock en dev) para levantar el iframe de pago con bancard-checkout-js.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Checkout iniciado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "El pago no está en estado PENDING"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El pago pertenece a otro usuario"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pago no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Error de comunicación con la pasarela")
    })
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @Parameter(description = "ID del pago") @PathVariable Long id,
            @Valid @RequestBody(required = false) CheckoutRequest request,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentGatewayService.checkout(id, principal.getName(), request),
                "Checkout iniciado"));
    }
}
