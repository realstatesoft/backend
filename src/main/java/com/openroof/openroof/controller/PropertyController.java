package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.*;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.nio.file.attribute.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "CRUD de propiedades inmobiliarias")
public class PropertyController {

    private final PropertyService propertyService;

    // ─── CREATE ───────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Crear una nueva propiedad")
    public ResponseEntity<ApiResponse<PropertyResponse>> create(
            @Valid @RequestBody CreatePropertyRequest request) {

        PropertyResponse response = propertyService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Propiedad creada exitosamente"));
    }

    // ─── READ ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una propiedad por ID")
    public ResponseEntity<ApiResponse<PropertyResponse>> getById(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id) {

        PropertyResponse response = propertyService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "Listar todas las propiedades (paginado, con filtros opcionales)")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getAll(
            @Parameter(description = "Filtrar por tipo de propiedad") @RequestParam(required = false) String propertyType,
            @Parameter(description = "Filtrar por estado") @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PropertySummaryResponse> page = propertyService.getAll(propertyType, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Listar propiedades de un propietario")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getByOwner(
            @Parameter(description = "ID del propietario") @PathVariable Long ownerId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PropertySummaryResponse> page = propertyService.getByOwner(ownerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/me")
    @Operation(summary = "Listar propiedades del usuario actual")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getMine(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PropertySummaryResponse> page = propertyService.getByOwner(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar propiedades por texto (título o descripción)")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> search(
            @Parameter(description = "Palabra clave de búsqueda") @RequestParam(name = "q", required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PropertySummaryResponse> page = propertyService.search(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }


    // ─── UPDATE ───────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una propiedad (parcial)")
    public ResponseEntity<ApiResponse<PropertyResponse>> update(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest request) {

        PropertyResponse response = propertyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad actualizada exitosamente"));
    }

    // ─── DELETE & TRASHCAN METHODS ────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una propiedad (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id) {

        propertyService.delete(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PatchMapping("/{id}/trash")
    @Operation(summary = "Mover una propiedad a la papelera")
    public  ResponseEntity<ApiResponse<PropertyResponse>> trash(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id) {

        PropertyResponse response = propertyService.trash(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad movida a papelera exitosamente"));

    }

    @PatchMapping("/{id}/restore")
    @Operation(summary = "Restaurar una propiedad de la papelera")
    public  ResponseEntity<ApiResponse<PropertyResponse>> restoreFromTrashcan(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id) {

        PropertyResponse response = propertyService.restoreFromTrashcan(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad restaurada de la papelera exitosamente"));
    }
    
    @PostMapping("/clear-trashcan")
    @Operation(summary = "Vaciar la papelera del usuario actual (soft delete definitivo)")
    public ResponseEntity<ApiResponse<Void>> clearTrashcan(@AuthenticationPrincipal User user) {

        int deletedCount = propertyService.clearTrashcanForUser(user.getId());

        String message = deletedCount > 0
                ? "Se vació la papelera. Propiedades eliminadas: " + deletedCount
                : "La papelera ya estaba vacía.";

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(deletedCount > 0)
                        .message(message)
                        .build()
        );
    }

    @GetMapping("/trashcan")
    @Operation(summary = "Obtener la papelera del usuario actual")
     public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getTrashcan(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, 
            @AuthenticationPrincipal User user) {

        Page<PropertySummaryResponse> page = propertyService.getTrashcan(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    // ─── CHANGE STATUS ────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar el estado de una propiedad")
    public ResponseEntity<ApiResponse<PropertyResponse>> changeStatus(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {

        PropertyResponse response = propertyService.changeStatus(id, request.newStatus());
        return ResponseEntity.ok(ApiResponse.ok(response, "Estado actualizado exitosamente"));
    }

    // ─── SIMILAR ────────────────────────────────────────────
    @GetMapping("/{id}/similar")
    @Operation(summary = "Obtener propiedades similares")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> findSimilar(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @RequestParam int n) {
        List <PropertyResponse> properties = propertyService.findSimilarProperties(id, n);
        return ResponseEntity.ok(ApiResponse.ok(properties));
    }
}
