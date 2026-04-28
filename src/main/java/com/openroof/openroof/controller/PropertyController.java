package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.*;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.PropertyService;
import com.openroof.openroof.service.RentCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "CRUD de propiedades inmobiliarias")
public class PropertyController {

    private final PropertyService propertyService;
    private final RentCalculationService rentCalculationService;

    // --- CREATE ---------------------------------------------------

    @PostMapping
    @Operation(summary = "Crear una nueva propiedad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PropertyResponse>> create(
            @Valid @RequestBody CreatePropertyRequest request,
            @AuthenticationPrincipal User user) {

        PropertyResponse response = propertyService.create(request, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Propiedad creada exitosamente"));
    }

    // --- READ -----------------------------------------------------

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una propiedad por ID")
    public ResponseEntity<ApiResponse<PropertyResponse>> getById(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id) {

        PropertyResponse response = propertyService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/views")
    @Operation(summary = "Registrar una visualización de una propiedad")
    public ResponseEntity<ApiResponse<Long>> registerView(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            Authentication auth,
            HttpServletRequest request) {

        User user = (auth != null && auth.getPrincipal() instanceof User principal) ? principal : null;
        long count = propertyService.registerView(id, user, request);
        return ResponseEntity.ok(ApiResponse.ok(count, "Visualización registrada"));
    }

    @GetMapping("/{id}/views/count")
    @Operation(summary = "Obtener el conteo de visualizaciones de una propiedad")
    public ResponseEntity<ApiResponse<Long>> getViewCount(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id) {

        long count = propertyService.getViewCount(id);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @GetMapping
    @Operation(summary = "Listar todas las propiedades (paginado, con filtros avanzados opcionales)")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getAll(
            @Parameter(description = "Disponibilidad (IMMEDIATE, IN_30_DAYS, IN_60_DAYS, TO_NEGOTIATE)") @RequestParam(required = false) String availability,

            @Parameter(description = "Tipo de propiedad (HOUSE, APARTMENT, LAND, OFFICE, WAREHOUSE, FARM)") @RequestParam(required = false) String propertyType,

            @Parameter(description = "Estado (PENDING, APPROVED, REJECTED, PUBLISHED, SOLD, RENTED, ARCHIVED)") @RequestParam(required = false) String status,

            @Parameter(description = "Precio mínimo (inclusive)") @RequestParam(required = false) java.math.BigDecimal minPrice,

            @Parameter(description = "Precio máximo (inclusive)") @RequestParam(required = false) java.math.BigDecimal maxPrice,

            @Parameter(description = "ID de la ubicación/zona") @RequestParam(required = false) Long locationId,

            @Parameter(description = "Cantidad mínima de baños") @RequestParam(required = false) java.math.BigDecimal minBathrooms,

            @Parameter(description = "Cantidad mínima de dormitorios") @RequestParam(required = false) Integer minBedrooms,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {

        PropertyFilterRequest filter = new PropertyFilterRequest(
                availability, propertyType, status,
                minPrice, maxPrice, locationId,
                minBathrooms, minBedrooms, null);

        Long userId = (auth != null && auth.getPrincipal() instanceof User user) ? user.getId() : null;

        Page<PropertySummaryResponse> page = propertyService.getAll(filter, pageable, userId);
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getMine(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PropertySummaryResponse> page = propertyService.getByOwner(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/agent/me")
    @Operation(summary = "Listar propiedades del agente actual (asignadas + de sus clientes)")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getAgentScope(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PropertySummaryResponse> page = propertyService.getByAgentScope(user.getEmail(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar propiedades por texto (título o descripción) y filtros adicionales opcionales")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> search(
            @Parameter(description = "Palabra clave de búsqueda") @RequestParam(name = "q", required = false) String keyword,
            @Parameter(description = "Disponibilidad (IMMEDIATE, IN_30_DAYS, IN_60_DAYS, TO_NEGOTIATE)") @RequestParam(required = false) String availability,
            @Parameter(description = "Tipo de propiedad (HOUSE, APARTMENT, LAND, OFFICE, WAREHOUSE, FARM)") @RequestParam(required = false) String propertyType,
            @Parameter(description = "Estado (PENDING, APPROVED, REJECTED, PUBLISHED, SOLD, RENTED, ARCHIVED)") @RequestParam(required = false) String status,
            @Parameter(description = "Precio mínimo (inclusive)") @RequestParam(required = false) java.math.BigDecimal minPrice,
            @Parameter(description = "Precio máximo (inclusive)") @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @Parameter(description = "ID de la ubicación/zona") @RequestParam(required = false) Long locationId,
            @Parameter(description = "Cantidad mínima de baños") @RequestParam(required = false) java.math.BigDecimal minBathrooms,
            @Parameter(description = "Cantidad mínima de dormitorios") @RequestParam(required = false) Integer minBedrooms,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {

        PropertyFilterRequest filter = new PropertyFilterRequest(
                availability, propertyType, status,
                minPrice, maxPrice, locationId,
                minBathrooms, minBedrooms, keyword);

        Long userId = (auth != null && auth.getPrincipal() instanceof User user) ? user.getId() : null;

        Page<PropertySummaryResponse> page = propertyService.search(filter, pageable, userId);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/featured")
    @Operation(summary = "Obtener las 8-12 propiedades destacadas o más recientes")
    public ResponseEntity<ApiResponse<List<PropertySummaryResponse>>> getFeatured(
            @Parameter(description = "Límite de propiedades") @RequestParam(defaultValue = "12") int limit) {
        List<PropertySummaryResponse> properties = propertyService.getFeaturedProperties(limit);
        return ResponseEntity.ok(ApiResponse.ok(properties));
    }

    // --- UPDATE ---------------------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#id, principal)")
    @Operation(summary = "Actualizar una propiedad (parcial)")
    public ResponseEntity<ApiResponse<PropertyResponse>> update(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest request,
            @AuthenticationPrincipal User user) {

        PropertyResponse response = propertyService.update(id, request, user.getId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad actualizada exitosamente"));
    }

    // --- DELETE & TRASHCAN METHODS
    // ------------------------------------------------

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una propiedad (soft delete)")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#id, principal)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        propertyService.delete(id, user.getId(), user.getRole());
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PatchMapping("/{id}/trash")
    @Operation(summary = "Mover una propiedad a la papelera")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#id, principal)")
    public ResponseEntity<ApiResponse<PropertyResponse>> trash(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        PropertyResponse response = propertyService.trash(id, user.getId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad movida a papelera exitosamente"));
    }

    @PatchMapping("/{id}/restore")
    @Operation(summary = "Restaurar una propiedad de la papelera")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#id, principal)")
    public ResponseEntity<ApiResponse<PropertyResponse>> restoreFromTrashcan(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        PropertyResponse response = propertyService.restoreFromTrashcan(id, user.getId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad restaurada de la papelera exitosamente"));
    }

    @PostMapping("/clear-trashcan")
    @Operation(summary = "Vaciar la papelera del usuario actual (soft delete definitivo)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> clearTrashcan(@AuthenticationPrincipal User user) {

        int deletedCount = propertyService.clearTrashcanForUser(user.getId(), user.getId(), user.getRole());

        String message = deletedCount > 0
                ? "Se vació la papelera. Propiedades eliminadas: " + deletedCount
                : "La papelera ya estaba vacía.";

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(deletedCount > 0)
                        .message(message)
                        .build());
    }

    @GetMapping("/trashcan")
    @Operation(summary = "Obtener la papelera del usuario actual")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getTrashcan(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {

        Page<PropertySummaryResponse> page = propertyService.getTrashcan(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    // --- CHANGE STATUS --------------------------------------------

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar el estado de una propiedad (solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PropertyResponse>> changeStatus(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal User user) {

        PropertyResponse response = propertyService.changeStatus(id, request.newStatus(), user);
        return ResponseEntity.ok(ApiResponse.ok(response, "Estado actualizado exitosamente"));
    }

    @PatchMapping("/{id}/highlight")
    @Operation(summary = "Marcar o desmarcar una propiedad como destacada (solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PropertyResponse>> toggleHighlight(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @RequestParam boolean highlighted,
            @AuthenticationPrincipal User user) {

        PropertyResponse response = propertyService.toggleHighlight(id, highlighted, user);
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad " + (highlighted ? "destacada" : "desmarcada como destacada")));
    }

    // --- SIMILAR --------------------------------------------
    @GetMapping("/{id}/similar")
    @Operation(summary = "Obtener propiedades similares")
    public ResponseEntity<ApiResponse<List<PropertySummaryResponse>>> findSimilar(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id,
            @RequestParam int size) {
        List <PropertySummaryResponse> properties = propertyService.findSimilarProperties(id, size);
        return ResponseEntity.ok(ApiResponse.ok(properties));
    }

    // --- RENT COST --------------------------------------------
    @GetMapping("/{id}/rent-cost")
    @Operation(summary = "Calcular costo inicial de alquiler")
    public ResponseEntity<ApiResponse<RentCostBreakdownResponse>> getRentCost(
            @Parameter(description = "ID de la propiedad") @PathVariable Long id) {
        RentCostBreakdownResponse response = rentCalculationService.calculateInitialCost(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
