package com.openroof.openroof.upload;

import org.springframework.mock.web.MockMultipartFile;

/**
 * Contenido binario mínimo con magic bytes válidos para tests de upload.
 */
public final class UploadTestFixtures {

    public static final byte[] PDF = "%PDF-1.4\n%EOF".getBytes();
    public static final byte[] JPEG = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0
    };
    public static final byte[] PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
    };
    public static final byte[] WEBP = new byte[]{
            'R', 'I', 'F', 'F', 0, 0, 0, 0,
            'W', 'E', 'B', 'P', 0, 0, 0, 0
    };
    public static final byte[] GLB = new byte[]{
            0x67, 0x6C, 0x54, 0x46,
            0x02, 0x00, 0x00, 0x00,
            0x0C, 0x00, 0x00, 0x00
    };
    public static final byte[] JSON = "{\"scenes\":[]}".getBytes();
    public static final byte[] GLTF = "{\"asset\":{\"version\":\"2.0\"}}".getBytes();

    private UploadTestFixtures() {
    }

    public static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", PDF);
    }

    public static MockMultipartFile jpeg(String name) {
        return new MockMultipartFile("file", name, "image/jpeg", JPEG);
    }

    public static MockMultipartFile png(String name) {
        return new MockMultipartFile("file", name, "image/png", PNG);
    }

    public static MockMultipartFile glb(String name) {
        return new MockMultipartFile("file", name, "model/gltf-binary", GLB);
    }

    public static MockMultipartFile json(String name) {
        return new MockMultipartFile("file", name, "application/json", JSON);
    }

    public static MockMultipartFile spoofedPdfAsJpg() {
        return new MockMultipartFile("file", "doc.jpg", "image/jpeg", PDF);
    }

    public static MockMultipartFile spoofedJpegAsGlb() {
        return new MockMultipartFile("file", "model.glb", "model/gltf-binary", JPEG);
    }

    public static MockMultipartFile emptyPdf() {
        return new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
    }
}
