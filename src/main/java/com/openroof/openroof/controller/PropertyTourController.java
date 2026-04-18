package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.PropertyMediaResponse;
import com.openroof.openroof.service.PropertyTourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Tag(name = "Property Tour", description = "Endpoints para gestionar tours virtuales 360")
public class PropertyTourController {

    private final PropertyTourService tourService;

    @PostMapping(value = "/{propertyId}/tour/360", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir una imagen panorámica 360 a una propiedad")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#propertyId, principal)")
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> upload360Image(
            @PathVariable Long propertyId,
            @RequestParam("file") MultipartFile file) {

        PropertyMediaResponse response = tourService.upload360Image(propertyId, file);
        return ResponseEntity.ok(ApiResponse.ok(response, "Imagen 360 subida exitosamente"));
    }

    @PostMapping(value = "/tour/360/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir una imagen panorámica 360 sin asociar a propiedad aún")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> upload360ImageGeneric(
            @RequestParam("file") MultipartFile file) {

        PropertyMediaResponse response = tourService.upload360ImageGeneric(file);
        return ResponseEntity.ok(ApiResponse.ok(response, "Imagen 360 subida (pendiente de asociación)"));
    }

    @PostMapping(value = "/{propertyId}/tour/config", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir archivo de configuración JSON para el tour virtual")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#propertyId, principal)")
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> uploadTourConfig(
            @PathVariable Long propertyId,
            @RequestParam("file") MultipartFile file) {

        PropertyMediaResponse response = tourService.uploadTourConfig(propertyId, file);
        return ResponseEntity.ok(ApiResponse.ok(response, "Configuración de tour subida exitosamente"));
    }

    @PostMapping(value = "/tour/config/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir configuración de tour sin asociar a propiedad aún")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> uploadTourConfigGeneric(
            @RequestParam("file") MultipartFile file) {

        PropertyMediaResponse response = tourService.uploadTourConfigGeneric(file);
        return ResponseEntity.ok(ApiResponse.ok(response, "Configuración de tour subida (pendiente de asociación)"));
    }


    @GetMapping("/{propertyId}/tour/media")
    @Operation(summary = "Obtener media asociada al tour virtual (imágenes 360 y config)")
    public ResponseEntity<ApiResponse<List<PropertyMediaResponse>>> getTourMedia(@PathVariable Long propertyId) {
        List<PropertyMediaResponse> media = tourService.getTourMedia(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(media));
    }
}
