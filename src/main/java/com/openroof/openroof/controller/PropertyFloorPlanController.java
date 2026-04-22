package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.PropertyMediaResponse;
import com.openroof.openroof.service.PropertyFloorPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Endpoints para gestionar planos (floor plans) de propiedades.
 * <p>
 * Acepta PDF, JPG, PNG y WebP. Máximo 5 planos por propiedad.
 * Requiere autenticación JWT para todas las operaciones de escritura.
 */
@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Property Floor Plans", description = "Subir, listar y eliminar planos de propiedades")
public class PropertyFloorPlanController {

    private final PropertyFloorPlanService floorPlanService;

    // ─── UPLOAD GENÉRICO (sin propertyId, para flujo de creación) ────────────

    @PostMapping(value = "/floor-plans/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Subir un plano sin asociar a propiedad todavía",
            description = "Sube el archivo al storage en la carpeta pending. Útil durante la creación de una propiedad antes de guardarla."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> uploadGeneric(
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user) {

        log.info("Upload genérico de plano por {}", user.getUsername());
        PropertyMediaResponse response = floorPlanService.uploadGeneric(file);
        return ResponseEntity.ok(ApiResponse.ok(response, "Plano subido (pendiente de asociación)"));
    }

    // ─── UPLOAD ligado a propiedad existente ────────────────────────────────

    @PostMapping(value = "/{propertyId}/floor-plans", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Subir un plano a una propiedad",
            description = "Acepta PDF, JPG, PNG o WebP. Máximo 5 planos por propiedad. Requiere ser dueño o admin."
    )
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#propertyId, principal)")
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> upload(
            @PathVariable Long propertyId,
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user) {

        log.info("Upload de plano para propiedad {} por {}", propertyId, user.getUsername());
        PropertyMediaResponse response = floorPlanService.upload(propertyId, file);
        return ResponseEntity.ok(ApiResponse.ok(response, "Plano subido exitosamente"));
    }

    // ─── GET ─────────────────────────────────────────────────────────────────

    @GetMapping("/{propertyId}/floor-plans")
    @Operation(
            summary = "Obtener planos de una propiedad",
            description = "Devuelve la lista de planos (FLOOR_PLAN) ordenados por order_index."
    )
    public ResponseEntity<ApiResponse<List<PropertyMediaResponse>>> getFloorPlans(
            @PathVariable Long propertyId) {

        List<PropertyMediaResponse> plans = floorPlanService.getByPropertyId(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(plans));
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @DeleteMapping("/{propertyId}/floor-plans/{mediaId}")
    @Operation(
            summary = "Eliminar un plano",
            description = "Elimina el registro de DB y borra el archivo de Supabase Storage tras el commit."
    )
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#propertyId, principal)")
    public ResponseEntity<ApiResponse<Void>> deleteFloorPlan(
            @PathVariable Long propertyId,
            @PathVariable Long mediaId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user) {

        log.info("Eliminar plano mediaId={} de propiedad {} por {}", mediaId, propertyId, user.getUsername());
        floorPlanService.delete(propertyId, mediaId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Plano eliminado exitosamente"));
    }
}
