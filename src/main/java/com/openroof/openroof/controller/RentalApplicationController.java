package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.RentalApplicationResponse;
import com.openroof.openroof.model.enums.RentalApplicationStatus;
import com.openroof.openroof.service.RentalApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/rental-applications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Rental Applications", description = "Gestión de solicitudes de arrendamiento")
public class RentalApplicationController {

    private final RentalApplicationService rentalApplicationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Enviar una solicitud de arrendamiento")
    public ResponseEntity<ApiResponse<RentalApplicationResponse>> submit(
            @Valid @RequestBody CreateRentalApplicationRequest request,
            Principal principal) {
        RentalApplicationResponse response = rentalApplicationService.submitApplication(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Solicitud enviada correctamente"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ver detalle de una solicitud")
    public ResponseEntity<ApiResponse<RentalApplicationResponse>> getById(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                rentalApplicationService.getApplication(id, principal.getName())));
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar solicitudes de una propiedad (owner/agent/admin)")
    public ResponseEntity<ApiResponse<Page<RentalApplicationResponse>>> listByProperty(
            @PathVariable Long propertyId,
            @RequestParam(required = false) RentalApplicationStatus status,
            @PageableDefault(size = 10, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                rentalApplicationService.listApplications(propertyId, status, pageable, principal.getName())));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aprobar una solicitud de arrendamiento")
    public ResponseEntity<ApiResponse<RentalApplicationResponse>> approve(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                rentalApplicationService.approveApplication(id, principal.getName()),
                "Solicitud aprobada"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Rechazar una solicitud de arrendamiento")
    public ResponseEntity<ApiResponse<RentalApplicationResponse>> reject(
            @PathVariable Long id,
            @NotBlank @RequestParam String reason,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                rentalApplicationService.rejectApplication(id, reason, principal.getName()),
                "Solicitud rechazada"));
    }

    @PostMapping("/{id}/convert-to-lease")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Convertir solicitud aprobada en contrato de arrendamiento")
    public ResponseEntity<ApiResponse<LeaseResponse>> convertToLease(
            @PathVariable Long id,
            @Valid @RequestBody CreateLeaseRequest leaseRequest,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        rentalApplicationService.convertToLease(id, leaseRequest, principal.getName()),
                        "Contrato creado correctamente"));
    }
}
