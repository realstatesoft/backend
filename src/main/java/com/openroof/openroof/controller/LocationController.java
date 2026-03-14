package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.location.LocationDto;
import com.openroof.openroof.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Manejo de zonas geográficas (ciudades/barrios)")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/match")
    @Operation(summary = "Busca ubicaciones coincidentes con un nombre de ciudad")
    public ResponseEntity<ApiResponse<List<LocationDto>>> matchByCity(
            @RequestParam String city) {
        
        List<LocationDto> locations = locationService.matchByCity(city);
        return ResponseEntity.ok(ApiResponse.ok(locations));
    }

    @PostMapping("/find-or-create")
    @Operation(summary = "Busca una ubicación por nombre o la crea si no existe")
    public ResponseEntity<ApiResponse<LocationDto>> findOrCreate(
            @RequestParam String city,
            @RequestParam(required = false) String department,
            @RequestParam(required = false, defaultValue = "Paraguay") String country,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        
        LocationDto locationDto = locationService.findOrCreate(city, department, country, lat, lng);
        return ResponseEntity.ok(ApiResponse.ok(locationDto, "Zona detectada/creada con éxito"));
    }
}
