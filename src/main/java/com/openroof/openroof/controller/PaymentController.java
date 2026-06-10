package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.payment.PaymentRequest;
import com.openroof.openroof.dto.payment.PaymentResponse;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Gestión de pagos")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar un nuevo pago")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pago registrado correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> create(
            @Valid @RequestBody PaymentRequest request,
            @Parameter(description = "Clave de idempotencia opcional: reintentos con la misma key devuelven el pago original sin duplicar")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Principal principal) {
        PaymentResponse response = (idempotencyKey == null || idempotencyKey.isBlank())
                ? paymentService.create(request, principal.getName())
                : paymentService.create(request, principal.getName(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Pago registrado"));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis pagos")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pagos obtenidos correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Filtrar por estado (PENDING, APPROVED, REJECTED, REFUNDED)") @RequestParam(required = false) PaymentStatus status,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.getMyPayments(principal.getName(), status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ver un pago por ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(
            @Parameter(description = "ID del pago") @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getById(id, principal.getName())));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los pagos (ADMIN)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pagos obtenidos correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado (solo ADMIN)")
    })
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Filtrar por ID de usuario") @RequestParam(required = false) Long userId,
            @Parameter(description = "Filtrar por estado (PENDING, APPROVED, REJECTED)") @RequestParam(required = false) PaymentStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.getAll(userId, status, pageable)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar un pago (ADMIN)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pago aprobado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "El pago no está en estado PENDING"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado (solo ADMIN)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> approve(
            @Parameter(description = "ID del pago") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.approvePayment(id), "Pago aprobado"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechazar un pago (ADMIN)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pago rechazado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "El pago no está en estado PENDING"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado (solo ADMIN)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> reject(
            @Parameter(description = "ID del pago") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.rejectPayment(id), "Pago rechazado"));
    }
}
