package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.service.RentalInstallmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rental Installments", description = "Cuotas de arrendamiento")
public class RentalInstallmentController {

    private final RentalInstallmentService installmentService;
    private final UserRepository userRepository;

    @GetMapping("/api/leases/{id}/installments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar cuotas de un contrato (paginado)")
    public ResponseEntity<ApiResponse<Page<RentalInstallmentResponse>>> listByLease(
            @PathVariable Long id,
            @PageableDefault(size = 12, sort = "dueDate", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(ApiResponse.ok(installmentService.listByLease(id, user.getId(), pageable)));
    }

    @GetMapping("/api/rentals/installments/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalle de una cuota")
    public ResponseEntity<ApiResponse<RentalInstallmentResponse>> getById(
            @PathVariable Long id,
            Principal principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(ApiResponse.ok(installmentService.getById(id, user.getId())));
    }

    private User resolveUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", principal.getName()));
    }
}
