package com.openroof.openroof.upload;

import com.openroof.openroof.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valida uploads usando magic bytes, extensión normalizada y coherencia de MIME.
 * Rechaza tipos ambiguos (p. ej. application/octet-stream sin firma reconocida).
 */
@Component
public class FileUploadValidator {

    private static final int HEADER_BYTES = 512;
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".com", ".msi", ".scr", ".ps1", ".vbs",
            ".js", ".jar", ".sh", ".php", ".asp", ".aspx", ".jsp", ".html", ".htm"
    );

    public ValidatedUpload validate(
            MultipartFile file,
            long maxSizeBytes,
            String maxSizeLabel,
            Set<DetectedFileKind> allowedKinds
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío o no fue proporcionado.");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new BadRequestException(
                    "El archivo supera el tamaño máximo permitido de " + maxSizeLabel + ".");
        }

        byte[] content = readContent(file);
        if (content.length == 0) {
            throw new BadRequestException("El archivo está vacío o no fue proporcionado.");
        }

        byte[] header = content.length <= HEADER_BYTES
                ? content
                : Arrays.copyOf(content, HEADER_BYTES);

        DetectedFileKind detected = FileMagicSniffer.detect(header)
                .orElseThrow(() -> new BadRequestException(
                        "No se pudo verificar el tipo real del archivo. El contenido no coincide con un formato permitido."));

        if (!allowedKinds.contains(detected)) {
            throw new BadRequestException("Tipo de archivo no permitido para esta operación.");
        }

        String declaredMime = normalizeMime(file.getContentType());
        if (isAmbiguousMime(declaredMime) && detected != DetectedFileKind.GLB) {
            throw new BadRequestException(
                    "Content-Type ambiguo no permitido: " + file.getContentType()
                            + ". Suba el archivo con el tipo MIME correcto.");
        }

        if (declaredMime != null && !detected.acceptsMime(declaredMime) && !isCompatibleMime(detected, declaredMime)) {
            throw new BadRequestException(
                    "El Content-Type declarado (" + file.getContentType()
                            + ") no coincide con el contenido real del archivo.");
        }

        String normalizedExtension = normalizeExtension(file.getOriginalFilename(), detected);
        String extensionFromName = extensionFromFilename(file.getOriginalFilename());

        if (extensionFromName != null && BLOCKED_EXTENSIONS.contains(extensionFromName)) {
            throw new BadRequestException("Extensión de archivo no permitida.");
        }

        if (extensionFromName != null && !extensionFromName.equals(normalizedExtension)) {
            throw new BadRequestException(
                    "La extensión del archivo (" + extensionFromName
                            + ") no coincide con su contenido real (" + normalizedExtension + ").");
        }

        String resolvedContentType = resolveContentType(detected, declaredMime);
        return new ValidatedUpload(detected, normalizedExtension, resolvedContentType, content);
    }

    /**
     * Normaliza y valida una extensión para uso en claves de storage (defensa en profundidad).
     */
    public static String normalizeExtensionForStorage(String originalFilename, String fallbackExtension) {
        String fromName = extensionFromFilename(originalFilename);
        if (fromName != null && !BLOCKED_EXTENSIONS.contains(fromName)) {
            return fromName;
        }
        if (fallbackExtension != null && !fallbackExtension.isBlank()) {
            String normalized = fallbackExtension.startsWith(".")
                    ? fallbackExtension.toLowerCase(Locale.ROOT)
                    : "." + fallbackExtension.toLowerCase(Locale.ROOT);
            if (!BLOCKED_EXTENSIONS.contains(normalized)) {
                return normalized;
            }
        }
        return "";
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        String base = filename.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        if (base.contains("..") || base.contains("\0")) {
            throw new BadRequestException("Nombre de archivo no válido.");
        }
        return base;
    }

    public static Set<DetectedFileKind> kindsFromCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(FileUploadValidator::kindFromMime)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    public static Set<DetectedFileKind> documentKinds() {
        return Set.of(DetectedFileKind.PDF, DetectedFileKind.JPEG, DetectedFileKind.PNG, DetectedFileKind.WEBP);
    }

    public static Set<DetectedFileKind> imageKinds() {
        return Set.of(DetectedFileKind.JPEG, DetectedFileKind.PNG, DetectedFileKind.WEBP);
    }

    public static Set<DetectedFileKind> modelKinds() {
        return Set.of(DetectedFileKind.GLB, DetectedFileKind.GLTF_JSON);
    }

    private static Optional<DetectedFileKind> kindFromMime(String mime) {
        String normalized = mime.toLowerCase(Locale.ROOT);
        return Arrays.stream(DetectedFileKind.values())
                .filter(kind -> kind.mimeTypes().stream().anyMatch(m -> m.equalsIgnoreCase(normalized)))
                .findFirst();
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("No se pudo leer el archivo subido.");
        }
    }

    private static String normalizeMime(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String base = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(base)) {
            return "image/jpeg";
        }
        return base;
    }

    private static boolean isAmbiguousMime(String mime) {
        if (mime == null) {
            return true;
        }
        return "application/octet-stream".equals(mime)
                || "application/x-msdownload".equals(mime)
                || "binary/octet-stream".equals(mime);
    }

    /**
     * GLB suele declararse como octet-stream; se acepta solo si la firma ya validó GLB.
     */
    private static boolean isCompatibleMime(DetectedFileKind detected, String declaredMime) {
        return detected == DetectedFileKind.GLB && "application/octet-stream".equals(declaredMime);
    }

    private static String normalizeExtension(String filename, DetectedFileKind detected) {
        sanitizeFilename(filename);
        return detected.canonicalExtension();
    }

    private static String extensionFromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String safeName = sanitizeFilename(filename);
        int dot = safeName.lastIndexOf('.');
        if (dot < 0 || dot == safeName.length() - 1) {
            return null;
        }
        String ext = safeName.substring(dot).toLowerCase(Locale.ROOT);
        if (".jpeg".equals(ext)) {
            return ".jpg";
        }
        return ext;
    }

    private static String resolveContentType(DetectedFileKind detected, String declaredMime) {
        if (declaredMime != null && detected.acceptsMime(declaredMime)) {
            return declaredMime;
        }
        return detected.mimeTypes().iterator().next();
    }
}
