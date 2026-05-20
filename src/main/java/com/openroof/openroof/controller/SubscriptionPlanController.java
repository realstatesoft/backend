package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.subscription.SubscriptionPlanRequest;
import com.openroof.openroof.dto.subscription.SubscriptionPlanResponse;
import com.openroof.openroof.service.SubscriptionPlanService;
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

@RestController
@RequestMapping("/subscription-plans")
@RequiredArgsConstructor
@Tag(name = "Subscription Plans", description = "Gestión de planes de suscripción")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @GetMapping
    @Operation(summary = "Listar planes activos (público)")
    public ResponseEntity<ApiResponse<Page<SubscriptionPlanResponse>>> getActivePlans(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionPlanService.getAll(true, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver un plan activo por ID (público)")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionPlanService.getActiveById(id)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los planes incluidos inactivos (ADMIN)")
    public ResponseEntity<ApiResponse<Page<SubscriptionPlanResponse>>> getAllPlans(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionPlanService.getAll(active, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un nuevo plan (ADMIN)")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> create(
            @Valid @RequestBody SubscriptionPlanRequest request) {
        SubscriptionPlanResponse response = subscriptionPlanService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Plan creado"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar un plan (ADMIN)")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionPlanService.update(id, request), "Plan actualizado"));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar un plan (ADMIN)")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionPlanService.deactivate(id), "Plan desactivado"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un plan sin suscripciones activas (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        subscriptionPlanService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Plan eliminado"));
    }
}
