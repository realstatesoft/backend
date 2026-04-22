package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.ImageUploadResponse;
import com.openroof.openroof.model.image.Image;
import com.openroof.openroof.repository.ImageRepository;
import com.openroof.openroof.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint para subir imágenes a Supabase Storage.
 * <p>
 * Requiere autenticación JWT (cualquier rol autenticado).
 */
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Images", description = "Operaciones de carga de imágenes")
public class ImageUploadController {

    private final StorageService storageService;
    private final ImageRepository imageRepository;

    /**
     * Sube una imagen a Supabase Storage, persiste metadatos y devuelve la URL pública.
     *
     * @param file     archivo de imagen (jpg, png, webp — máx 5 MB)
     * @param folder   carpeta lógica en el bucket (opcional, default "general")
     * @param user     usuario autenticado (inyectado por Spring Security)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Subir imagen",
            description = "Sube una imagen (jpg, png, webp) al storage configurado. "
                    + "Requiere autenticación JWT. Devuelve URL pública, nombre, tamaño y tipo."
    )
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Imagen subida exitosamente",
                    content = @Content(schema = @Schema(implementation = ImageUploadResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Archivo vacío, tipo no permitido o error de lectura"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            @AuthenticationPrincipal UserDetails user
    ) {
        log.info("Upload solicitado por {} — archivo: {}, tamaño: {} bytes",
                user.getUsername(), file.getOriginalFilename(), file.getSize());

        // 1. Subir a Storage
        StorageService.UploadResult result = storageService.upload(file, folder);

        // 2. Persistir metadatos
        Image image = Image.builder()
                .url(result.url())
                .filename(result.filename())
                .contentType(result.contentType())
                .size(result.size())
                .uploadedBy(user.getUsername())
                .build();
        image = imageRepository.save(image);

        // 3. Armar respuesta
        ImageUploadResponse response = ImageUploadResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .filename(image.getFilename())
                .size(image.getSize())
                .contentType(image.getContentType())
                .uploadedBy(image.getUploadedBy())
                .createdAt(image.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response, "Imagen subida exitosamente"));
    }
}
