package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.AgentRatingSummaryResponse;
import com.openroof.openroof.dto.agent.AgentReviewResponse;
import com.openroof.openroof.dto.agent.CreateAgentReviewRequest;
import com.openroof.openroof.dto.agent.UpdateAgentReviewRequest;
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
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/agents/{agentId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Agent Reviews", description = "Reseñas de agentes")
public class AgentReviewController {

    private final AgentReviewService agentReviewService;

    @GetMapping
    @Operation(summary = "Listar reseñas de un agente (público)")
    public ResponseEntity<ApiResponse<Page<AgentReviewResponse>>> getReviews(
            @PathVariable Long agentId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(agentReviewService.getReviews(agentId, pageable)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumen de rating: promedio, distribución y últimas reseñas (público)")
    public ResponseEntity<ApiResponse<AgentRatingSummaryResponse>> getRatingSummary(@PathVariable Long agentId) {
        return ResponseEntity.ok(ApiResponse.ok(agentReviewService.getRatingSummary(agentId)));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener la propia reseña del usuario autenticado para este agente")
    public ResponseEntity<ApiResponse<AgentReviewResponse>> getMyReview(
            @PathVariable Long agentId,
            Principal principal) {
        return agentReviewService.getMyReview(agentId, principal.getName())
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear reseña para un agente")
    public ResponseEntity<ApiResponse<AgentReviewResponse>> create(
            @PathVariable Long agentId,
            @Valid @RequestBody CreateAgentReviewRequest req,
            Principal principal) {
        AgentReviewResponse res = agentReviewService.create(agentId, principal.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(res, "Reseña creada"));
    }

    @PatchMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Editar la propia reseña")
    public ResponseEntity<ApiResponse<AgentReviewResponse>> update(
            @PathVariable Long agentId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateAgentReviewRequest req,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                agentReviewService.update(reviewId, principal.getName(), req)));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Eliminar reseña (owner o ADMIN)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long agentId,
            @PathVariable Long reviewId,
            Principal principal) {
        agentReviewService.delete(reviewId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
