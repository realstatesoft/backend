package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.PropertyMediaResponse;
import com.openroof.openroof.service.PropertyModelService;
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
@Tag(name = "Property 3D Models", description = "Endpoints para gestionar modelos 3D de propiedades")
public class PropertyModelController {

    private final PropertyModelService modelService;

    @PostMapping(value = "/model3d/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir un modelo 3D (GLB/GLTF) sin asociar a propiedad aún")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> uploadModelGeneric(
            @RequestParam("file") MultipartFile file) {

        PropertyMediaResponse response = modelService.uploadModelGeneric(file);
        return ResponseEntity.ok(ApiResponse.ok(response, "Modelo 3D subido (pendiente de asociación)"));
    }

    @PostMapping(value = "/{propertyId}/model3d", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir un modelo 3D (GLB/GLTF) a una propiedad")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#propertyId, principal)")
    public ResponseEntity<ApiResponse<PropertyMediaResponse>> uploadModel(
            @PathVariable Long propertyId,
            @RequestParam("file") MultipartFile file) {

        PropertyMediaResponse response = modelService.uploadModel(propertyId, file);
        return ResponseEntity.ok(ApiResponse.ok(response, "Modelo 3D subido exitosamente"));
    }

    @GetMapping("/{propertyId}/model3d")
    @Operation(summary = "Obtener modelos 3D de una propiedad")
    public ResponseEntity<ApiResponse<List<PropertyMediaResponse>>> getModels(@PathVariable Long propertyId) {
        List<PropertyMediaResponse> models = modelService.getModelsByPropertyId(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(models));
    }

    @DeleteMapping("/{propertyId}/model3d/{mediaId}")
    @Operation(summary = "Eliminar un modelo 3D")
    @PreAuthorize("isAuthenticated() and @propertySecurity.canModify(#propertyId, principal)")
    public ResponseEntity<ApiResponse<Void>> deleteModel(
            @PathVariable Long propertyId,
            @PathVariable Long mediaId) {

        modelService.deleteModel(propertyId, mediaId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Modelo 3D eliminado exitosamente"));
    }
}
