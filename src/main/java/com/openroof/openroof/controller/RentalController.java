package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.rental.InstallmentPaymentRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.LeasePayment;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.repository.LeasePaymentRepository;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.RentalInstallmentRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.service.PaymentService;
import com.openroof.openroof.service.RentalPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/rentals")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Tag(name = "Rentals", description = "Endpoints para la gestión de cuotas y pagos de contratos")
public class RentalController {

    private final RentalInstallmentRepository installmentRepository;
    private final LeasePaymentRepository leasePaymentRepository;
    private final PaymentService paymentService;
    private final RentalPaymentService rentalPaymentService;
    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;

    private void verifyLeaseAccess(Lease lease, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (user.getRole() == UserRole.ADMIN || user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROPERTY_MANAGER"))) return;
        boolean isTenant = lease.getPrimaryTenant() != null && lease.getPrimaryTenant().getId().equals(user.getId());
        boolean isLandlord = lease.getLandlord() != null && lease.getLandlord().getId().equals(user.getId());
        if (!isTenant && !isLandlord) {
            throw new ForbiddenException("No tiene acceso a este contrato");
        }
    }

    @GetMapping("/installments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener las cuotas de un contrato")
    public ResponseEntity<ApiResponse<List<RentalInstallment>>> getInstallments(@RequestParam Long leaseId, Principal principal) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));
        verifyLeaseAccess(lease, principal.getName());
        List<RentalInstallment> installments = installmentRepository.findByLeaseIdOrderByDueDateAsc(leaseId);
        return ResponseEntity.ok(ApiResponse.ok(installments));
    }

    @GetMapping("/payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener los pagos de un contrato")
    public ResponseEntity<ApiResponse<List<LeasePayment>>> getPayments(@RequestParam Long leaseId, Principal principal) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));
        verifyLeaseAccess(lease, principal.getName());
        List<LeasePayment> payments = leasePaymentRepository.findByLeaseIdOrderByCreatedAtDesc(leaseId);
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }

    @PostMapping("/installments/{id}/payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar un pago para una cuota")
    @Transactional
    public ResponseEntity<ApiResponse<com.openroof.openroof.dto.rental.LeasePaymentResponse>> registerManualPayment(
            @PathVariable Long id,
            @Valid @RequestBody InstallmentPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Principal principal) {
        RentalInstallment installment = installmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada"));
        verifyLeaseAccess(installment.getLease(), principal.getName());
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key es obligatorio");
        }
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        LeasePayment payment = rentalPaymentService.registerPayment(
                id, user, request.amount(), request.method(), idempotencyKey, request.notes());
                
        com.openroof.openroof.dto.rental.LeasePaymentResponse response = new com.openroof.openroof.dto.rental.LeasePaymentResponse(
                payment.getId(),
                payment.getLease().getId(),
                payment.getInstallment().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod() != null ? payment.getMethod().name() : null,
                payment.getStatus() != null ? payment.getStatus().name() : null,
                payment.getType() != null ? payment.getType().name() : null,
                payment.getPaidAt(),
                payment.getReceiptPdfUrl()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Pago registrado"));
    }

    @GetMapping({"/installments/{id}/invoice-url", "/installments/{id}/invoice.pdf"})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener URL de la factura de una cuota")
    public ResponseEntity<ApiResponse<String>> downloadInvoice(@PathVariable Long id, Principal principal) {
        RentalInstallment installment = installmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada"));
        verifyLeaseAccess(installment.getLease(), principal.getName());

        if (installment.getInvoicePdfUrl() == null || installment.getInvoicePdfUrl().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("La factura aún no se ha generado"));
        }

        return ResponseEntity.ok(ApiResponse.ok(installment.getInvoicePdfUrl(), "URL de la factura obtenida"));
    }

    @GetMapping({"/payments/{id}/receipt-url", "/payments/{id}/receipt.pdf"})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener URL del recibo de un pago")
    public ResponseEntity<ApiResponse<String>> downloadReceipt(@PathVariable Long id, Principal principal) {
        LeasePayment payment = leasePaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        verifyLeaseAccess(payment.getLease(), principal.getName());

        if (payment.getReceiptPdfUrl() == null || payment.getReceiptPdfUrl().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("El recibo aún no se ha generado"));
        }

        return ResponseEntity.ok(ApiResponse.ok(payment.getReceiptPdfUrl(), "URL del recibo obtenida"));
    }
}
