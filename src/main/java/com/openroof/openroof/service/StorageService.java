package com.openroof.openroof.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Contrato para subir archivos al storage.
 * Implementaciones: SupabaseStorageService.
 */
public interface StorageService {

    /**
     * Sube un archivo al storage.
     *
     * @param file   archivo enviado por el usuario
     * @param folder carpeta lógica (e.g. "properties", "avatars")
     * @return resultado con URL, nombre, tamaño y tipo
     */
    UploadResult upload(MultipartFile file, String folder);

    /**
     * Elimina un archivo del storage por su clave (ruta relativa al bucket).
     * Se usa para purgar binarios huérfanos cuando un documento es reemplazado.
     * Los errores deben ser manejados por el llamador.
     *
     * @param key clave del archivo, tal como se obtuvo de {@link UploadResult#filename()}
     */
    void delete(String key);

    /**
     * Genera una URL firmada con expiración para un objeto privado del storage.
     *
     * @param objectPath     clave del objeto dentro del bucket (e.g. "documents/42/uuid.pdf")
     * @param expiresInSeconds tiempo de validez de la URL en segundos
     * @return URL firmada temporal
     */
    String generateSignedUrl(String objectPath, int expiresInSeconds);

    /**
     * DTO con el resultado de la subida.
     */
    record UploadResult(
            String url,
            String filename,
            long size,
            String contentType
    ) {}
}
