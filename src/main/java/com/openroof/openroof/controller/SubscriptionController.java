package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.subscription.SubscriptionResponse;
import com.openroof.openroof.model.enums.SubscriptionStatus;
import com.openroof.openroof.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Gestión de suscripciones de usuarios")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas las suscripciones (ADMIN)")
    public ResponseEntity<ApiResponse<Page<SubscriptionResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) SubscriptionStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.getAll(status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ver suscripción por ID (ADMIN)")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.getById(id)));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Historial de mis suscripciones")
    public ResponseEntity<ApiResponse<Page<SubscriptionResponse>>> getMySubscriptions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                subscriptionService.getMySubscriptions(principal.getName(), pageable)));
    }

    @GetMapping("/my/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mi suscripción activa actual")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getMyActiveSubscription(Principal principal) {
        Optional<SubscriptionResponse> active = subscriptionService.getMyActiveSubscription(principal.getName());
        return active
                .map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null, "Sin suscripción activa")));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancelar una suscripción (propio usuario o ADMIN)")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                subscriptionService.cancelSubscription(id, principal.getName()), "Suscripción cancelada"));
    }
}
