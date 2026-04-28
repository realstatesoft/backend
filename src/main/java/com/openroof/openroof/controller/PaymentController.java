package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.payment.PaymentRequest;
import com.openroof.openroof.dto.payment.PaymentResponse;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<ApiResponse<PaymentResponse>> create(
            @Valid @RequestBody PaymentRequest request,
            Principal principal) {
        PaymentResponse response = paymentService.create(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Pago registrado"));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis pagos")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) PaymentStatus status,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.getMyPayments(principal.getName(), status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ver un pago por ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getById(id, principal.getName())));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los pagos (ADMIN)")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) PaymentStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.getAll(userId, status, pageable)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar un pago (ADMIN)")
    public ResponseEntity<ApiResponse<PaymentResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.approvePayment(id), "Pago aprobado"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechazar un pago (ADMIN)")
    public ResponseEntity<ApiResponse<PaymentResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.rejectPayment(id), "Pago rechazado"));
    }
}
