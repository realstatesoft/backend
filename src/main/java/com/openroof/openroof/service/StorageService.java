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
     * DTO con el resultado de la subida.
     */
    record UploadResult(
            String url,
            String filename,
            long size,
            String contentType
    ) {}
}
