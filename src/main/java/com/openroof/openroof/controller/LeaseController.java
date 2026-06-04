package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.SignLeaseRequest;
import com.openroof.openroof.dto.rental.LeaseSummaryResponse;
import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.RentalInstallmentMapper;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.service.ESignatureService;
import com.openroof.openroof.service.LeaseService;
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
    private final ESignatureService eSignatureService;
    private final RentalInstallmentMapper installmentMapper;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("@leaseSecurity.canCreateLease(#dto.propertyId(), authentication.principal)")
    @Operation(summary = "Crear un contrato de arrendamiento")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Contrato creado correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permiso para crear contratos en esta propiedad")
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contrato encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado al contrato"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    public ResponseEntity<ApiResponse<LeaseResponse>> getById(
            @Parameter(description = "ID del contrato") @PathVariable Long id,
            Principal principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(ApiResponse.ok(leaseService.getLease(id, user.getId())));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar contratos del usuario autenticado")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contratos obtenidos correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ApiResponse<Page<LeaseSummaryResponse>>> list(
            @Parameter(description = "Filtrar por estado (DRAFT, ACTIVE, TERMINATED, EXPIRED)") @RequestParam(required = false) LeaseStatus status,
            @Parameter(description = "Filtrar por ID de propiedad") @RequestParam(required = false) Long propertyId,
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contrato actualizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "El contrato no está en estado DRAFT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permiso para modificar este contrato"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    public ResponseEntity<ApiResponse<LeaseResponse>> update(
            @Parameter(description = "ID del contrato") @PathVariable Long id,
            @Valid @RequestBody CreateLeaseRequest dto) {
        return ResponseEntity.ok(ApiResponse.ok(leaseService.updateLease(id, dto), "Contrato actualizado"));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("@leaseSecurity.canManageLease(#id, authentication.principal)")
    @Operation(summary = "Activar un contrato (requiere firmas de ambas partes)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contrato activado y cuotas generadas"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "El contrato no está en estado DRAFT o falta alguna firma"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permiso para activar este contrato"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    public ResponseEntity<ApiResponse<List<RentalInstallmentResponse>>> activate(
            @Parameter(description = "ID del contrato") @PathVariable Long id) {
        List<RentalInstallmentResponse> installments =
                installmentMapper.toResponseList(leaseService.activateLease(id));
        return ResponseEntity.ok(ApiResponse.ok(installments, "Contrato activado"));
    }

    @PostMapping("/{id}/send-for-signature")
    @PreAuthorize("@leaseSecurity.canManageLease(#id, authentication.principal)")
    @Operation(summary = "Enviar lease a firma electrónica")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lease enviado a firma"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "El contrato no está en estado DRAFT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permiso para enviar a firma"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> sendForSignature(
            @Parameter(description = "ID del contrato") @PathVariable Long id) {
        eSignatureService.sendForSignature(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Lease enviado a firma"));
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "Firmar lease mediante token", description = "No requiere JWT; valida token de un solo uso.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Firma registrada correctamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Token inválido o expirado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> sign(
            @Parameter(description = "ID del contrato") @PathVariable Long id,
            @Parameter(description = "Token de firma de un solo uso") @RequestParam("token") String token,
            @Valid @RequestBody(required = false) SignLeaseRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        eSignatureService.sign(id, token, request, ip, userAgent);
        return ResponseEntity.ok(ApiResponse.ok(null, "Firma registrada"));
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("@leaseSecurity.canManageLease(#id, authentication.principal)")
    @Operation(summary = "Terminar un contrato activo")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contrato terminado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "El contrato no está en estado ACTIVE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permiso para terminar este contrato"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    public ResponseEntity<ApiResponse<LeaseResponse>> terminate(
            @Parameter(description = "ID del contrato") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(leaseService.terminateLease(id), "Contrato terminado"));
    }

    private User resolveUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", principal.getName()));
    }
}
