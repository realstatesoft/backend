package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.config.RentConfigResponse;
import com.openroof.openroof.dto.config.UpdateRentConfigRequest;
import com.openroof.openroof.service.RentConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config/rent")
@RequiredArgsConstructor
@Tag(name = "Rent Configuration", description = "Configuración de costos de alquiler")
public class RentConfigController {

    private final RentConfigService rentConfigService;

    @GetMapping
    @Operation(summary = "Obtener configuración actual de alquiler")
    public ResponseEntity<ApiResponse<RentConfigResponse>> getRentConfig() {
        RentConfigResponse config = rentConfigService.getRentConfig();
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PutMapping
    @Operation(summary = "Actualizar configuración de alquiler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RentConfigResponse>> updateRentConfig(
            @Valid @RequestBody UpdateRentConfigRequest request) {
        RentConfigResponse config = rentConfigService.updateRentConfig(request);
        return ResponseEntity.ok(ApiResponse.ok(config, "Configuración de alquiler actualizada"));
    }
}
