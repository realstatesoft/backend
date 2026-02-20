package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Health", description = "Endpoint de verificación")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Verificar estado de la API")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(
                ApiResponse.ok(Map.of(
                        "status", "UP",
                        "service", "OpenRoof API"
                ), "Servicio funcionando correctamente")
        );
    }
}
