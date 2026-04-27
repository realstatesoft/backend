package com.openroof.openroof.service;

import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final long maxFileSizeBytes;
    private final String maxFileSizeLabel;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-role-key:}") String serviceRoleKey,
            @Value("${supabase.storage.bucket:openroof-images}") String bucket,
            @Value("${upload.max-file-size:10MB}") String maxFileSize
    ) {
        this.supabaseUrl = supabaseUrl;
        this.bucket = bucket;
        this.maxFileSizeBytes = DataSize.parse(maxFileSize).toBytes();
        this.maxFileSizeLabel = maxFileSize;

        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .build();
    }

    @Override
    public UploadResult upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o no fue proporcionado.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);

        // Permitimos sobrepasar el límite global para documentos KYC, PDFs, Modelos 3D y Planos,
        // ya que sus servicios específicos aplican sus propias reglas de tamaño.
        boolean isLargeFileAllowed = (folder != null && (
                folder.startsWith("documents/") || 
                folder.contains("models") || 
                folder.contains("floor-plans")
        ))
                || ".pdf".equalsIgnoreCase(extension)
                || ".glb".equalsIgnoreCase(extension)
                || ".gltf".equalsIgnoreCase(extension)
                || ".bin".equalsIgnoreCase(extension);

        if (!isLargeFileAllowed && file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido de " + maxFileSizeLabel + ".");
        }

        if (file.getContentType() == null) {
            throw new IllegalArgumentException("El Content-Type del archivo no puede ser nulo.");
        }
        try {
            MediaType.parseMediaType(file.getContentType());
        } catch (Exception e) {
            throw new IllegalArgumentException("Content-Type inválido: " + file.getContentType());
        }

        // Validation of specific allowed types is intentionally delegated to the calling service
        // (e.g. UserDocumentService or PropertyImageService) so that each
        // context can enforce its own allowed-types without
        // SupabaseStorageService duplicating or conflicting with those rules.


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
            throw new StorageException("Error al subir archivo a Supabase Storage: " + e.getMessage(), e);
        }
    }

    // ─── Delete ──────────────────────────────────────────────────────────

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            log.warn("delete() invocado con clave nula o vacía; operación ignorada.");
            return;
        }
        try {
            restClient.delete()
                    .uri("/object/{bucket}/{key}", bucket, key)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Archivo eliminado de Supabase Storage: {}", key);
        } catch (Exception e) {
            log.error("Error al eliminar archivo '{}' de Supabase Storage: {}", key, e.getMessage(), e);
            throw new StorageException("No se pudo eliminar el archivo '" + key + "': " + e.getMessage(), e);
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
