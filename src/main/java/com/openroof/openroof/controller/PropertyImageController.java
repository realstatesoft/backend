package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.PropertyMediaResponse;
import com.openroof.openroof.service.PropertyImageService;
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
 * Endpoints para gestionar imágenes de propiedades.
 * <p>
 * Requiere autenticación JWT.
 */
@RestController
@RequestMapping("/properties/{propertyId}/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Property Images", description = "Subir, listar y gestionar imágenes de propiedades")
public class PropertyImageController {

    private final PropertyImageService propertyImageService;

    // ─── UPLOAD ──────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Subir imágenes a una propiedad",
            description = "Sube una o varias imágenes (jpg, png, webp) y las asocia a la propiedad. "
                    + "Máximo 20 imágenes por propiedad. Requiere JWT."
    )
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#propertyId, principal)")
    public ResponseEntity<ApiResponse<List<PropertyMediaResponse>>> uploadImages(
            @PathVariable Long propertyId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user
    ) {
        log.info("Upload de {} imagen(es) para propiedad {} por {}",
                files.size(), propertyId, user.getUsername());

        List<PropertyMediaResponse> responses = propertyImageService
                .uploadImages(propertyId, files, isPrimary);

        return ResponseEntity.ok(
                ApiResponse.ok(responses, "Imagen(es) subida(s) exitosamente"));
    }

    // ─── GET ALL ─────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "Obtener todas las imágenes de una propiedad",
            description = "Devuelve la lista de imágenes ordenadas por order_index."
    )
    public ResponseEntity<ApiResponse<List<PropertyMediaResponse>>> getImages(
            @PathVariable Long propertyId
    ) {
        List<PropertyMediaResponse> images = propertyImageService.getByPropertyId(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(images));
    }

    // ─── GET PRIMARY ─────────────────────────────────────────────

    @GetMapping("/primary")
    @Operation(
            summary = "Obtener la imagen principal de una propiedad",
            description = "Devuelve la imagen marcada como principal (is_primary=true)."
    )
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> getPrimaryImage(
            @PathVariable Long propertyId
    ) {
        PropertyMediaResponse primary = propertyImageService.getPrimaryImage(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(primary));
    }

    // ─── SET PRIMARY ─────────────────────────────────────────────

    @PatchMapping("/{mediaId}/primary")
    @Operation(
            summary = "Marcar una imagen como principal",
            description = "Desmarca la imagen principal actual y marca la indicada."
    )
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#propertyId, principal)")
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> setPrimary(
            @PathVariable Long propertyId,
            @PathVariable Long mediaId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user
    ) {
        log.info("Set primary image: propertyId={}, mediaId={}, by={}",
                propertyId, mediaId, user.getUsername());

        PropertyMediaResponse response = propertyImageService
                .setPrimaryImage(propertyId, mediaId);

        return ResponseEntity.ok(
                ApiResponse.ok(response, "Imagen principal actualizada"));
    }

    // ─── DELETE ──────────────────────────────────────────────────

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#propertyId, principal)")
    @Operation(
            summary = "Eliminar una imagen de una propiedad",
            description = "Elimina el registro de media. El objeto en Supabase Storage se mantiene (por auditoría)."
    )
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long propertyId,
            @PathVariable Long mediaId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user
    ) {
        log.info("Delete image: propertyId={}, mediaId={}, by={}",
                propertyId, mediaId, user.getUsername());

        propertyImageService.deleteImage(propertyId, mediaId);

        return ResponseEntity.ok(ApiResponse.ok(null, "Imagen eliminada exitosamente"));
    }
}
