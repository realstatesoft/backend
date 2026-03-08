package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.*;
import com.openroof.openroof.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "CRUD de propiedades inmobiliarias")
public class PropertyController {

    private final PropertyService propertyService;

    // ─── CREATE ───────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Crear una nueva propiedad")
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#id, principal)") //seguridad!!
    @Operation(summary = "Actualizar una propiedad (parcial)")
    
    public ResponseEntity<ApiResponse<PropertyResponse>> update(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest request) {

        PropertyResponse response = propertyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad actualizada exitosamente"));
    }

    // ─── DELETE ───────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una propiedad (soft delete)")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#id, principal)") //seguridad!!
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id) {

        propertyService.delete(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    // ─── CHANGE STATUS ────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar el estado de una propiedad")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#id, principal)") //seguridad!!
    public ResponseEntity<ApiResponse<PropertyResponse>> changeStatus(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {

        PropertyResponse response = propertyService.changeStatus(id, request.newStatus());
        return ResponseEntity.ok(ApiResponse.ok(response, "Estado actualizado exitosamente"));
    }
}
