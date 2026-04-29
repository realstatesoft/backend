package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.PropertySummaryResponse;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.PropertyViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Property Views", description = "Historial de propiedades vistas recientemente")
public class PropertyViewController {

    private final PropertyViewService propertyViewService;

    @PostMapping("/properties/{propertyId}/recent-views")
    @Operation(summary = "Registrar una vista reciente de una propiedad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> registerRecentView(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal User user) {

        propertyViewService.registerRecentView(propertyId, user);
        return ResponseEntity.ok(ApiResponse.ok(null, "Vista reciente registrada"));
    }

    @GetMapping("/users/me/recent-properties")
    @Operation(summary = "Obtener propiedades vistas recientemente por el usuario actual")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PropertySummaryResponse>>> getRecentProperties(
            @AuthenticationPrincipal User user) {

        List<PropertySummaryResponse> recent = propertyViewService.getRecentProperties(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(recent));
    }
}
