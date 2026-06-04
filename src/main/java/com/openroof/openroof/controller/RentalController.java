package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.rental.InstallmentPaymentRequest;
import com.openroof.openroof.dto.rental.LeasePaymentResponse;
import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.RentalInstallmentMapper;
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
import com.openroof.openroof.service.RentalDocumentPdfService;
import com.openroof.openroof.service.RentalPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

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
    private final RentalDocumentPdfService rentalDocumentPdfService;
    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;
    private final RentalInstallmentMapper installmentMapper;

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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cuotas obtenidas correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado al contrato"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    public ResponseEntity<ApiResponse<List<RentalInstallmentResponse>>> getInstallments(
            @Parameter(description = "ID del contrato") @RequestParam Long leaseId, Principal principal) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));
        verifyLeaseAccess(lease, principal.getName());
        List<RentalInstallment> installments = installmentRepository.findByLeaseIdOrderByDueDateAsc(leaseId);
        List<RentalInstallmentResponse> response = installmentMapper.toResponseList(installments);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener los pagos de un contrato")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pagos obtenidos correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado al contrato"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    public ResponseEntity<ApiResponse<List<LeasePaymentResponse>>> getPayments(
            @Parameter(description = "ID del contrato") @RequestParam Long leaseId, Principal principal) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));
        verifyLeaseAccess(lease, principal.getName());
        List<LeasePayment> payments = leasePaymentRepository.findByLeaseIdOrderByCreatedAtDesc(leaseId);
        List<LeasePaymentResponse> response = payments.stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private LeasePaymentResponse toPaymentResponse(LeasePayment payment) {
        return new LeasePaymentResponse(
                payment.getId(),
                payment.getLease() != null ? payment.getLease().getId() : null,
                payment.getInstallment() != null ? payment.getInstallment().getId() : null,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod() != null ? payment.getMethod().name() : null,
                payment.getStatus() != null ? payment.getStatus().name() : null,
                payment.getType() != null ? payment.getType().name() : null,
                payment.getPaidAt(),
                payment.getReceiptPdfUrl()
        );
    }

    @PostMapping("/installments/{id}/payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar un pago para una cuota")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pago registrado correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Idempotency-Key es obligatorio o datos inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado al contrato"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cuota no encontrada")
    })
    @Transactional
    public ResponseEntity<ApiResponse<com.openroof.openroof.dto.rental.LeasePaymentResponse>> registerManualPayment(
            @Parameter(description = "ID de la cuota") @PathVariable Long id,
            @Valid @RequestBody InstallmentPaymentRequest request,
            @Parameter(description = "Clave de idempotencia para evitar pagos duplicados") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
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

    @GetMapping(value = "/installments/{id}/invoice.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descargar PDF de la factura de una cuota (generado on-the-fly)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF de la factura generado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado al contrato"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cuota no encontrada")
    })
    public ResponseEntity<byte[]> downloadInvoice(
            @Parameter(description = "ID de la cuota") @PathVariable Long id, Principal principal) {
        RentalInstallment installment = installmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada"));
        verifyLeaseAccess(installment.getLease(), principal.getName());

        byte[] pdfBytes = rentalDocumentPdfService.generateInvoice(installment);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.builder("attachment")
                .filename("factura-" + id + ".pdf").build());
        headers.setContentLength(pdfBytes.length);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/payments/{id}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descargar PDF del recibo de un pago (generado on-the-fly)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF del recibo generado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado al contrato"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<byte[]> downloadReceipt(
            @Parameter(description = "ID del pago") @PathVariable Long id, Principal principal) {
        LeasePayment payment = leasePaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        verifyLeaseAccess(payment.getLease(), principal.getName());

        byte[] pdfBytes = rentalDocumentPdfService.generateReceipt(payment);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.builder("attachment")
                .filename("recibo-" + id + ".pdf").build());
        headers.setContentLength(pdfBytes.length);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
