package com.openroof.openroof.service;

import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.MediaType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyMedia;
import com.openroof.openroof.repository.PropertyMediaRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.upload.FileUploadValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static com.openroof.openroof.upload.UploadTestFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyModelServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyMediaRepository mediaRepository;

    @Mock
    private SupabaseStorageService storageService;

    @InjectMocks
    private PropertyModelService propertyModelService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(propertyModelService, "allowedTypesCsv", "model/gltf-binary, model/gltf+json");
        ReflectionTestUtils.setField(propertyModelService, "maxModelFileSizeRaw", "10MB");
        ReflectionTestUtils.setField(propertyModelService, "fileUploadValidator",
                new FileUploadValidator());
        propertyModelService.initConfig();
    }

    @Test
    void uploadModel_WhenPropertyNotFound_ShouldThrowException() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.empty());
        MockMultipartFile file = glb("test.glb");

        assertThrows(ResourceNotFoundException.class, () -> 
            propertyModelService.uploadModel(1L, file)
        );
    }

    @Test
    void uploadModel_WhenFileEmpty_ShouldThrowBadRequest() {
        Property property = new Property();
        property.setId(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "model/gltf-binary", new byte[0]);

        assertThrows(BadRequestException.class, () -> 
            propertyModelService.uploadModel(1L, emptyFile)
        );
    }

    @Test
    void uploadModel_WhenInvalidExtension_ShouldThrowBadRequest() {
        Property property = new Property();
        property.setId(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        
        assertThrows(BadRequestException.class, () ->
            propertyModelService.uploadModel(1L, spoofedJpegAsGlb())
        );
    }

    @Test
    void uploadModel_WhenFakeExtensionOnValidGlb_ShouldThrowBadRequest() {
        Property property = new Property();
        property.setId(1L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        MockMultipartFile fakeExt = new MockMultipartFile(
                "file", "model.jpg", "model/gltf-binary", GLB);

        assertThrows(BadRequestException.class, () ->
            propertyModelService.uploadModel(1L, fakeExt)
        );
    }

    @Test
    void uploadModel_Success_ShouldCallStorageAndRepository() {
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            mockedStatic.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            Property property = new Property();
            property.setId(1L);
            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
            
            MockMultipartFile file = glb("model.glb");
            
            StorageService.UploadResult mockResult = new StorageService.UploadResult(
                "https://storage.com/model.glb", "properties/1/models/model.glb", 1024L, "model/gltf-binary");
            
            when(storageService.upload(any(), anyString())).thenReturn(mockResult);
            
            PropertyMedia mockMedia = PropertyMedia.builder()
                    .property(property)
                    .url(mockResult.url())
                    .type(MediaType.MODEL_3D)
                    .build();
            mockMedia.setId(100L);
            when(mediaRepository.save(any(PropertyMedia.class))).thenReturn(mockMedia);

            var response = propertyModelService.uploadModel(1L, file);

            assertNotNull(response);
            assertEquals(mockResult.url(), response.getUrl());
            verify(storageService).upload(file, "properties/1/models");
            verify(mediaRepository).save(any(PropertyMedia.class));
            
            // Verificamos que se intentó registrar la sincronización
            mockedStatic.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
        }
    }

    @Test
    void deleteModel_WhenNotOwnedByProperty_ShouldThrowBadRequest() {
        Property p1 = new Property(); p1.setId(1L);
        Property p2 = new Property(); p2.setId(2L);
        
        PropertyMedia media = PropertyMedia.builder()
                .property(p2) // Pertenece a propiedad 2
                .type(MediaType.MODEL_3D)
                .build();
        media.setId(100L);
        
        when(mediaRepository.findById(100L)).thenReturn(Optional.of(media));

        // Intentamos borrar media 100 desde propiedad 1
        assertThrows(BadRequestException.class, () -> 
            propertyModelService.deleteModel(1L, 100L)
        );
    }
}
