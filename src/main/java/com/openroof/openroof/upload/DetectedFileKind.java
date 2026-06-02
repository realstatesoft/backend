package com.openroof.openroof.upload;

import java.util.Set;

/**
 * Tipos de archivo reconocidos por firma (magic bytes) y extensión canónica.
 */
public enum DetectedFileKind {

    PDF(Set.of("application/pdf"), ".pdf"),
    JPEG(Set.of("image/jpeg", "image/jpg"), ".jpg"),
    PNG(Set.of("image/png"), ".png"),
    WEBP(Set.of("image/webp"), ".webp"),
    GLB(Set.of("model/gltf-binary", "application/octet-stream"), ".glb"),
    GLTF_JSON(Set.of("model/gltf+json", "application/json"), ".gltf"),
    JSON(Set.of("application/json", "text/json"), ".json");

    private final Set<String> mimeTypes;
    private final String canonicalExtension;

    DetectedFileKind(Set<String> mimeTypes, String canonicalExtension) {
        this.mimeTypes = mimeTypes;
        this.canonicalExtension = canonicalExtension;
    }

    public Set<String> mimeTypes() {
        return mimeTypes;
    }

    public String canonicalExtension() {
        return canonicalExtension;
    }

    public boolean acceptsMime(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }
        String normalized = mimeType.trim().toLowerCase();
        return mimeTypes.stream().anyMatch(m -> m.equalsIgnoreCase(normalized));
    }
}
