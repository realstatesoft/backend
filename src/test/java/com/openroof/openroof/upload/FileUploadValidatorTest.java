package com.openroof.openroof.upload;

import com.openroof.openroof.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.openroof.openroof.upload.UploadTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileUploadValidator")
class FileUploadValidatorTest {

    private FileUploadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FileUploadValidator();
    }

    @Test
    void validate_acceptsAllowedPdf() {
        var result = validator.validate(pdf("id.pdf"), 10_000_000, "10MB", FileUploadValidator.documentKinds());
        assertEquals(DetectedFileKind.PDF, result.kind());
        assertEquals(".pdf", result.normalizedExtension());
    }

    @Test
    void validate_acceptsAllowedJpeg() {
        var result = validator.validate(jpeg("photo.jpg"), 10_000_000, "10MB", FileUploadValidator.imageKinds());
        assertEquals(DetectedFileKind.JPEG, result.kind());
    }

    @Test
    void validate_rejectsSpoofedExtension() {
        assertThrows(BadRequestException.class, () ->
                validator.validate(spoofedPdfAsJpg(), 10_000_000, "10MB", FileUploadValidator.documentKinds()));
    }

    @Test
    void validate_rejectsSpoofedGlbWithJpegContent() {
        assertThrows(BadRequestException.class, () ->
                validator.validate(spoofedJpegAsGlb(), 10_000_000, "10MB", FileUploadValidator.modelKinds()));
    }

    @Test
    void validate_rejectsEmptyFile() {
        assertThrows(BadRequestException.class, () ->
                validator.validate(emptyPdf(), 10_000_000, "10MB", FileUploadValidator.documentKinds()));
    }

    @Test
    void validate_rejectsUnknownBinary() {
        var unknown = new org.springframework.mock.web.MockMultipartFile(
                "file", "x.bin", "application/octet-stream", "not-a-real-format".getBytes());
        assertThrows(BadRequestException.class, () ->
                validator.validate(unknown, 10_000_000, "10MB", FileUploadValidator.documentKinds()));
    }

    @Test
    void validate_rejectsAmbiguousOctetStreamForDocuments() {
        var fake = new org.springframework.mock.web.MockMultipartFile(
                "file", "doc.pdf", "application/octet-stream", PDF);
        assertThrows(BadRequestException.class, () ->
                validator.validate(fake, 10_000_000, "10MB", FileUploadValidator.documentKinds()));
    }

    @Test
    void validate_acceptsGlbWithOctetStreamMime() {
        var glb = new org.springframework.mock.web.MockMultipartFile(
                "file", "scene.glb", "application/octet-stream", GLB);
        var result = validator.validate(glb, 50_000_000, "50MB", FileUploadValidator.modelKinds());
        assertEquals(DetectedFileKind.GLB, result.kind());
    }

    @Test
    void validate_acceptsJsonConfig() {
        var result = validator.validate(json("tour.json"), 2_000_000, "2MB", java.util.Set.of(DetectedFileKind.JSON));
        assertEquals(DetectedFileKind.JSON, result.kind());
    }

    @Test
    void normalizeExtensionForStorage_rejectsBlockedExtension() {
        assertEquals("", FileUploadValidator.normalizeExtensionForStorage("malware.exe", ""));
    }

    @Test
    void normalizeExtensionForStorage_rejectsBlockedFallbackExtension() {
        assertEquals("", FileUploadValidator.normalizeExtensionForStorage("file", ".exe"));
    }
}
