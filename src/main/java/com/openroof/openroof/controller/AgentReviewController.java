package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.AgentRatingSummaryResponse;
import com.openroof.openroof.dto.agent.AgentReviewResponse;
import com.openroof.openroof.dto.agent.CreateAgentReviewRequest;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.AgentReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agents/{agentId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Agent Reviews", description = "Reseñas de agentes")
public class AgentReviewController {

    private final AgentReviewService agentReviewService;

    @GetMapping
    @Operation(summary = "Listar reseñas de un agente (público)")
    public ResponseEntity<ApiResponse<Page<AgentReviewResponse>>> list(
            @PathVariable Long agentId,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                agentReviewService.getReviews(agentId, currentUserId, pageable)));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener la propia reseña del usuario autenticado para este agente")
    public ResponseEntity<ApiResponse<AgentReviewResponse>> myReview(
            @PathVariable Long agentId,
            @AuthenticationPrincipal User currentUser) {
        AgentReviewResponse review = agentReviewService.getMyReview(agentId, currentUser.getId());
        if (review == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(review));
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumen de calificaciones: promedio, distribución y últimas reseñas")
    public ResponseEntity<ApiResponse<AgentRatingSummaryResponse>> summary(
            @PathVariable Long agentId,
            @AuthenticationPrincipal User currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                agentReviewService.getRatingSummary(agentId, currentUserId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear reseña para un agente")
    public ResponseEntity<ApiResponse<AgentReviewResponse>> create(
            @PathVariable Long agentId,
            @Valid @RequestBody CreateAgentReviewRequest req,
            @AuthenticationPrincipal User currentUser) {
        AgentReviewResponse res = agentReviewService.createReview(agentId, currentUser.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(res, "Reseña creada"));
    }

    @RequestMapping(value = "/{reviewId}", method = {org.springframework.web.bind.annotation.RequestMethod.PUT, org.springframework.web.bind.annotation.RequestMethod.PATCH})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Editar la propia reseña")
    public ResponseEntity<ApiResponse<AgentReviewResponse>> update(
            @PathVariable Long agentId,
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateAgentReviewRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                agentReviewService.updateReview(reviewId, currentUser.getId(), req)));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Eliminar la propia reseña")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long agentId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser) {
        agentReviewService.deleteReview(reviewId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
