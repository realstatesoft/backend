package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.favorite.FavoriteActionResponse;
import com.openroof.openroof.dto.property.PropertySummaryResponse;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Gestión de propiedades favoritas del usuario autenticado")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Agregar una propiedad a favoritos")
    public ResponseEntity<ApiResponse<FavoriteActionResponse>> addFavorite(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId,
            @AuthenticationPrincipal User user) {

        FavoriteActionResponse response = favoriteService.addFavorite(propertyId, user);
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad agregada a favoritos"));
    }

    @DeleteMapping("/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Quitar una propiedad de favoritos")
    public ResponseEntity<ApiResponse<FavoriteActionResponse>> removeFavorite(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId,
            @AuthenticationPrincipal User user) {

        FavoriteActionResponse response = favoriteService.removeFavorite(propertyId, user);
        return ResponseEntity.ok(ApiResponse.ok(response, "Propiedad eliminada de favoritos"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar propiedades favoritas del usuario autenticado")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getMyFavorites(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Estado de propiedad (PUBLISHED, SOLD, RENTED, etc.)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Fecha mínima de agregado a favoritos (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate addedFrom,
            @Parameter(description = "Fecha máxima de agregado a favoritos (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate addedTo,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<PropertySummaryResponse> response = favoriteService.getMyFavorites(
                user, status, addedFrom, addedTo, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
