package com.openroof.openroof.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Detecta el tipo real de un archivo a partir de magic bytes y estructura básica.
 */
final class FileMagicSniffer {

    private static final byte[] PDF_SIGNATURE = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] GLB_SIGNATURE = "glTF".getBytes(StandardCharsets.US_ASCII);

    private FileMagicSniffer() {
    }

    static Optional<DetectedFileKind> detect(byte[] header) {
        if (header == null || header.length == 0) {
            return Optional.empty();
        }

        if (startsWith(header, PDF_SIGNATURE)) {
            return Optional.of(DetectedFileKind.PDF);
        }
        if (isJpeg(header)) {
            return Optional.of(DetectedFileKind.JPEG);
        }
        if (startsWith(header, PNG_SIGNATURE)) {
            return Optional.of(DetectedFileKind.PNG);
        }
        if (isWebp(header)) {
            return Optional.of(DetectedFileKind.WEBP);
        }
        if (isGlb(header)) {
            return Optional.of(DetectedFileKind.GLB);
        }
        if (isGenericJson(header)) {
            if (isGltfJson(header)) {
                return Optional.of(DetectedFileKind.GLTF_JSON);
            }
            return Optional.of(DetectedFileKind.JSON);
        }
        return Optional.empty();
    }

    static byte[] readHeader(InputStream inputStream, int maxBytes) throws IOException {
        byte[] buffer = new byte[maxBytes];
        int read = inputStream.read(buffer);
        if (read <= 0) {
            return new byte[0];
        }
        return read == buffer.length ? buffer : Arrays.copyOf(buffer, read);
    }

    private static boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private static boolean isWebp(byte[] header) {
        if (header.length < 12) {
            return false;
        }
        return header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
    }

    private static boolean isGlb(byte[] header) {
        if (header.length < 8 || !startsWith(header, GLB_SIGNATURE)) {
            return false;
        }
        int version = littleEndianInt(header, 4);
        return version == 2;
    }

    private static boolean isGltfJson(byte[] header) {
        if (!looksLikeJsonText(header)) {
            return false;
        }
        String sample = new String(header, StandardCharsets.UTF_8).toLowerCase();
        return sample.contains("\"asset\"");
    }

    private static boolean isGenericJson(byte[] header) {
        return looksLikeJsonText(header);
    }

    private static boolean looksLikeJsonText(byte[] header) {
        int index = 0;
        while (index < header.length && Character.isWhitespace(header[index])) {
            index++;
        }
        if (index >= header.length) {
            return false;
        }
        byte first = header[index];
        return first == '{' || first == '[';
    }

    private static boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static int littleEndianInt(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }
}
