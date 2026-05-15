package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.LeaseSummaryResponse;
import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.RentalInstallmentMapper;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.service.LeaseService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/leases")
@RequiredArgsConstructor
@Tag(name = "Leases", description = "Gestión de contratos de arrendamiento")
public class LeaseController {

    private final LeaseService leaseService;
    private final RentalInstallmentMapper installmentMapper;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("@leaseSecurity.canCreateLease(#dto.propertyId(), authentication.principal)")
    @Operation(summary = "Crear un contrato de arrendamiento")
    public ResponseEntity<ApiResponse<LeaseResponse>> create(
            @Valid @RequestBody CreateLeaseRequest dto,
            Principal principal) {
        User landlord = resolveUser(principal);
        LeaseResponse response = leaseService.createLease(landlord.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Contrato creado"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ver un contrato de arrendamiento")
    public ResponseEntity<ApiResponse<LeaseResponse>> getById(
            @PathVariable Long id,
            Principal principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(ApiResponse.ok(leaseService.getLease(id, user.getId())));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar contratos del usuario autenticado")
    public ResponseEntity<ApiResponse<Page<LeaseSummaryResponse>>> list(
            @RequestParam(required = false) LeaseStatus status,
            @RequestParam(required = false) Long propertyId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        User user = resolveUser(principal);
        Page<LeaseSummaryResponse> page = leaseService.listLeases(
                user.getId(), user.getRole(), status, propertyId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@leaseSecurity.canManageLease(#id, authentication.principal)")
    @Operation(summary = "Actualizar un contrato de arrendamiento (solo en estado DRAFT)")
    public ResponseEntity<ApiResponse<LeaseResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateLeaseRequest dto) {
        return ResponseEntity.ok(ApiResponse.ok(leaseService.updateLease(id, dto), "Contrato actualizado"));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("@leaseSecurity.canManageLease(#id, authentication.principal)")
    @Operation(summary = "Activar un contrato (requiere firmas de ambas partes)")
    public ResponseEntity<ApiResponse<List<RentalInstallmentResponse>>> activate(
            @PathVariable Long id) {
        List<RentalInstallmentResponse> installments =
                installmentMapper.toResponseList(leaseService.activateLease(id));
        return ResponseEntity.ok(ApiResponse.ok(installments, "Contrato activado"));
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("@leaseSecurity.canManageLease(#id, authentication.principal)")
    @Operation(summary = "Terminar un contrato activo")
    public ResponseEntity<ApiResponse<LeaseResponse>> terminate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(leaseService.terminateLease(id), "Contrato terminado"));
    }

    private User resolveUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", principal.getName()));
    }
}
