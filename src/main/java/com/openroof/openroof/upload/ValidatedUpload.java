package com.openroof.openroof.upload;

/**
 * Resultado de una validación exitosa de upload.
 */
public record ValidatedUpload(
        DetectedFileKind kind,
        String normalizedExtension,
        String resolvedContentType
) {
}
