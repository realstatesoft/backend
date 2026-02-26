package com.openroof.openroof.service;

import com.openroof.openroof.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Implementación de {@link StorageService} usando Supabase Storage.
 * <p>
 * API de Supabase Storage:
 * <ul>
 *   <li>Upload:     POST /storage/v1/object/{bucket}/{path}</li>
 *   <li>Public URL: https://{ref}.supabase.co/storage/v1/object/public/{bucket}/{path}</li>
 * </ul>
 */
@Service
@Slf4j
public class SupabaseStorageService implements StorageService {

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String bucket;
    private final List<String> allowedTypes;

    public SupabaseStorageService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.storage.bucket}") String bucket,
            @Value("#{'${upload.allowed-types}'.split(',')}") List<String> allowedTypes
    ) {
        this.supabaseUrl = supabaseUrl;
        this.bucket = bucket;
        this.allowedTypes = allowedTypes;

        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .build();
    }

    @Override
    public UploadResult upload(MultipartFile file, String folder) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String key = buildKey(folder, extension);

        try {
            byte[] fileBytes = file.getBytes();

            restClient.post()
                    .uri("/object/{bucket}/{key}", bucket, key)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .body(fileBytes)
                    .retrieve()
                    .toBodilessEntity();

            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + key;

            log.info("Archivo subido a Supabase Storage: {} ({} bytes)", key, file.getSize());

            return new UploadResult(publicUrl, key, file.getSize(), file.getContentType());

        } catch (IOException e) {
            log.error("Error al leer el archivo para subir a Supabase Storage", e);
            throw new BadRequestException("No se pudo leer el archivo: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al subir archivo a Supabase Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo a Supabase Storage: " + e.getMessage(), e);
        }
    }

    // ─── Validación ──────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío o no fue proporcionado.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BadRequestException(
                    "Tipo de archivo no permitido: " + contentType
                            + ". Permitidos: " + String.join(", ", allowedTypes));
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private String buildKey(String folder, String extension) {
        String uuid = UUID.randomUUID().toString();
        if (folder == null || folder.isBlank()) {
            return uuid + extension;
        }
        return folder.replaceAll("/+$", "") + "/" + uuid + extension;
    }
}
