package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.AdminDeleteAgentReviewRequest;
import com.openroof.openroof.service.AdminAgentReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/admin/agent-reviews")
@RequiredArgsConstructor
@Tag(name = "Admin Agent Reviews", description = "Moderación de reseñas de agentes")
public class AdminAgentReviewController {

    private final AdminAgentReviewService adminAgentReviewService;

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar reseña como ADMIN con motivo registrado en AuditLog")
    public ResponseEntity<ApiResponse<Void>> deleteAsAdmin(
            @PathVariable Long reviewId,
            @Valid @RequestBody AdminDeleteAgentReviewRequest req,
            Principal principal) {
        adminAgentReviewService.deleteReviewAsAdmin(reviewId, principal.getName(), req.moderationReason());
        return ResponseEntity.ok(ApiResponse.ok(null, "Reseña eliminada"));
    }
}
