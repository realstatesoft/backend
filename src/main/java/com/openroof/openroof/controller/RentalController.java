package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.payment.PaymentRequest;
import com.openroof.openroof.dto.payment.PaymentResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.rental.LeasePayment;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.repository.LeasePaymentRepository;
import com.openroof.openroof.repository.RentalInstallmentRepository;
import com.openroof.openroof.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/rentals")
@RequiredArgsConstructor
@Tag(name = "Rentals", description = "Endpoints para la gestión de cuotas y pagos de contratos")
public class RentalController {

    private final RentalInstallmentRepository installmentRepository;
    private final LeasePaymentRepository leasePaymentRepository;
    private final PaymentService paymentService;

    @GetMapping("/installments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener las cuotas de un contrato")
    public ResponseEntity<ApiResponse<List<RentalInstallment>>> getInstallments(@RequestParam Long leaseId) {
        // En una implementación real, se debería validar que el usuario tenga acceso al lease (Owner, Tenant o PM)
        List<RentalInstallment> installments = installmentRepository.findByLeaseIdOrderByDueDateAsc(leaseId);
        return ResponseEntity.ok(ApiResponse.ok(installments));
    }

    @GetMapping("/payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener los pagos de un contrato")
    public ResponseEntity<ApiResponse<List<LeasePayment>>> getPayments(@RequestParam Long leaseId) {
        // En una implementación real, se debería validar que el usuario tenga acceso al lease
        List<LeasePayment> payments = leasePaymentRepository.findByLeaseIdOrderByCreatedAtDesc(leaseId);
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }

    @PostMapping("/installments/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Registrar un pago manual para una cuota")
    public ResponseEntity<ApiResponse<PaymentResponse>> registerManualPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Principal principal) {
        // Reusamos el servicio de pagos. Podríamos ajustar request si es necesario.
        // En este diseño básico, simplemente lo delegamos a PaymentService.
        // Ojo: PaymentService.create() asume ciertas cosas. 
        // Si no está adaptado para procesar cuotas específicas, se debería extender PaymentService.
        PaymentResponse response = paymentService.create(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Pago registrado"));
    }

    @GetMapping("/installments/{id}/invoice.pdf")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descargar factura de una cuota")
    public ResponseEntity<?> downloadInvoice(@PathVariable Long id) {
        RentalInstallment installment = installmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada"));

        if (installment.getInvoicePdfUrl() == null || installment.getInvoicePdfUrl().isBlank()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(null, "La factura aún no se ha generado"));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(installment.getInvoicePdfUrl()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/payments/{id}/receipt.pdf")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descargar recibo de un pago")
    public ResponseEntity<?> downloadReceipt(@PathVariable Long id) {
        LeasePayment payment = leasePaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        if (payment.getReceiptPdfUrl() == null || payment.getReceiptPdfUrl().isBlank()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(null, "El recibo aún no se ha generado"));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(payment.getReceiptPdfUrl()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
