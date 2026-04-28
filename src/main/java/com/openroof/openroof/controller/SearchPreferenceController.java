package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.search.SearchPreferenceRequest;
import com.openroof.openroof.dto.search.SearchPreferenceResponse;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.SearchPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/search-preferences")
@RequiredArgsConstructor
@Tag(name = "Search Preferences", description = "Gestión de búsquedas guardadas del usuario autenticado")
public class SearchPreferenceController {

    private final SearchPreferenceService service;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear una búsqueda guardada")
    public ResponseEntity<ApiResponse<SearchPreferenceResponse>> createSearchPreference(
            @Valid @RequestBody SearchPreferenceRequest request,
            @AuthenticationPrincipal User user) {

        SearchPreferenceResponse response = service.createSearchPreference(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Búsqueda guardada creada"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar mis búsquedas guardadas")
    public ResponseEntity<ApiResponse<Page<SearchPreferenceResponse>>> getMySearchPreferences(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<SearchPreferenceResponse> response = service.getUserSearchPreferences(user, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar nombre de búsqueda guardada")
    public ResponseEntity<ApiResponse<SearchPreferenceResponse>> updateSearchPreference(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSearchPreferenceRequest request,
            @AuthenticationPrincipal User user) {

        SearchPreferenceResponse response = service.updateName(id, request.name(), user);
        return ResponseEntity.ok(ApiResponse.ok(response, "Búsqueda guardada actualizada"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Eliminar búsqueda guardada")
    public ResponseEntity<ApiResponse<Void>> deleteSearchPreference(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        service.deleteSearchPreference(id, user);
        return ResponseEntity.ok(ApiResponse.ok(null, "Búsqueda guardada eliminada"));
    }

    public record UpdateSearchPreferenceRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name
    ) {}
}