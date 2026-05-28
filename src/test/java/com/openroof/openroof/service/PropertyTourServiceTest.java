package com.openroof.openroof.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.repository.PropertyMediaRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.upload.FileUploadValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.openroof.openroof.upload.UploadTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyTourService upload validation")
class PropertyTourServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private PropertyMediaRepository mediaRepository;
    @Mock private SupabaseStorageService storageService;

    @InjectMocks
    private PropertyTourService propertyTourService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(propertyTourService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(propertyTourService, "fileUploadValidator", new FileUploadValidator());
        ReflectionTestUtils.setField(propertyTourService, "maxImageSizeRaw", "15MB");
        ReflectionTestUtils.setField(propertyTourService, "maxTourConfigSizeRaw", "2MB");
        propertyTourService.initConfig();
    }

    @Test
    void upload360Image_rejectsEmptyFile() {
        Property property = new Property();
        property.setId(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        assertThrows(BadRequestException.class, () ->
                propertyTourService.upload360Image(1L, emptyPdf()));
        verify(storageService, never()).upload(any(), any());
    }

    @Test
    void upload360Image_rejectsSpoofedExtension() {
        Property property = new Property();
        property.setId(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        assertThrows(BadRequestException.class, () ->
                propertyTourService.upload360Image(1L, spoofedPdfAsJpg()));
        verify(storageService, never()).upload(any(), any());
    }

    @Test
    void uploadTourConfig_rejectsInvalidJsonContent() {
        Property property = new Property();
        property.setId(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        var brokenJson = new org.springframework.mock.web.MockMultipartFile(
                "file", "tour.json", "application/json", "{not-json".getBytes());

        assertThrows(BadRequestException.class, () ->
                propertyTourService.uploadTourConfig(1L, brokenJson));
        verify(storageService, never()).upload(any(), any());
    }
}
