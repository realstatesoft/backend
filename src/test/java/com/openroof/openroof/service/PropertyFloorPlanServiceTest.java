package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.PropertyMediaResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.MediaType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyMedia;
import com.openroof.openroof.repository.PropertyMediaRepository;
import com.openroof.openroof.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyFloorPlanService")
class PropertyFloorPlanServiceTest {

    @Mock private PropertyRepository     propertyRepository;
    @Mock private PropertyMediaRepository mediaRepository;
    @Mock private SupabaseStorageService  storageService;

    @InjectMocks
    private PropertyFloorPlanService service;

    private Property property;

    @BeforeEach
    void setUp() {
        // Inyectar @Value manualmente (igual que PropertyModelServiceTest)
        ReflectionTestUtils.setField(service, "maxFileSizeRaw", "10MB");
        ReflectionTestUtils.setField(service, "allowedTypesRaw",
                "application/pdf,image/jpeg,image/png,image/webp");
        ReflectionTestUtils.setField(service, "maxPerProperty", 5);
        service.initConfig();

        property = new Property();
        property.setId(1L);
    }

    // ─── upload() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("upload()")
    class Upload {

        @Test
        @DisplayName("Propiedad no encontrada → ResourceNotFoundException")
        void upload_propertyNotFound_throws() {
            when(propertyRepository.findById(99L)).thenReturn(Optional.empty());
            MockMultipartFile file = pdf("plano.pdf");

            assertThrows(ResourceNotFoundException.class, () -> service.upload(99L, file));
        }

        @Test
        @DisplayName("Archivo vacío → BadRequestException")
        void upload_emptyFile_throws() {
            MockMultipartFile empty = new MockMultipartFile("file", "plano.pdf",
                    "application/pdf", new byte[0]);

            assertThrows(BadRequestException.class, () -> service.upload(1L, empty));
        }

        @Test
        @DisplayName("Tipo no permitido (mp4) → BadRequestException")
        void upload_invalidType_throws() {
            MockMultipartFile mp4 = new MockMultipartFile("file", "video.mp4",
                    "video/mp4", "data".getBytes());

            assertThrows(BadRequestException.class, () -> service.upload(1L, mp4));
        }

        @Test
        @DisplayName("Límite de planos alcanzado → BadRequestException")
        void upload_limitReached_throws() {
            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
            when(mediaRepository.countByPropertyIdAndType(1L, MediaType.FLOOR_PLAN)).thenReturn(5L);

            assertThrows(BadRequestException.class, () -> service.upload(1L, pdf("plano.pdf")));
        }

        @Test
        @DisplayName("Éxito con PDF → llama a storage y guarda en DB")
        void upload_success_callsStorageAndRepository() {
            try (var mocked = mockStatic(TransactionSynchronizationManager.class)) {
                mocked.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

                when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
                when(mediaRepository.countByPropertyIdAndType(1L, MediaType.FLOOR_PLAN)).thenReturn(0L);

                StorageService.UploadResult result = new StorageService.UploadResult(
                        "https://storage.test/plano.pdf",
                        "properties/1/floor-plans/plano.pdf",
                        5000L, "application/pdf");
                when(storageService.upload(any(), anyString())).thenReturn(result);

                PropertyMedia saved = PropertyMedia.builder()
                        .property(property)
                        .type(MediaType.FLOOR_PLAN)
                        .url(result.url())
                        .storageKey(result.filename())
                        .isPrimary(false)
                        .orderIndex(0)
                        .title("plano.pdf")
                        .build();
                saved.setId(10L);
                when(mediaRepository.save(any(PropertyMedia.class))).thenReturn(saved);

                PropertyMediaResponse response = service.upload(1L, pdf("plano.pdf"));

                assertNotNull(response);
                assertEquals(result.url(), response.getUrl());
                assertEquals(MediaType.FLOOR_PLAN, response.getType());
                verify(storageService).upload(any(), eq("properties/1/floor-plans"));
                verify(mediaRepository).save(any(PropertyMedia.class));
                mocked.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
            }
        }

        @Test
        @DisplayName("Éxito con imagen JPG → tipo permitido y guarda correctamente")
        void upload_successJpeg_savesMedia() {
            try (var mocked = mockStatic(TransactionSynchronizationManager.class)) {
                mocked.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

                when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
                when(mediaRepository.countByPropertyIdAndType(1L, MediaType.FLOOR_PLAN)).thenReturn(2L);

                StorageService.UploadResult result = new StorageService.UploadResult(
                        "https://storage.test/foto.jpg",
                        "properties/1/floor-plans/foto.jpg",
                        3000L, "image/jpeg");
                when(storageService.upload(any(), anyString())).thenReturn(result);

                PropertyMedia saved = PropertyMedia.builder()
                        .property(property).type(MediaType.FLOOR_PLAN)
                        .url(result.url()).orderIndex(2).isPrimary(false).title("foto.jpg")
                        .build();
                saved.setId(11L);
                when(mediaRepository.save(any())).thenReturn(saved);

                PropertyMediaResponse response = service.upload(1L,
                        new MockMultipartFile("file", "foto.jpg", "image/jpeg", "data".getBytes()));

                assertNotNull(response);
                assertEquals(MediaType.FLOOR_PLAN, response.getType());
            }
        }
    }

    // ─── uploadGeneric() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("uploadGeneric()")
    class UploadGeneric {

        @Test
        @DisplayName("PDF pendiente → retorna respuesta sin propertyId")
        void uploadGeneric_pdf_returnsResponse() {
            StorageService.UploadResult result = new StorageService.UploadResult(
                    "https://storage.test/pending/plano.pdf",
                    "floor-plans/pending/plano.pdf",
                    4000L, "application/pdf");
            when(storageService.upload(any(), anyString())).thenReturn(result);

            PropertyMediaResponse response = service.uploadGeneric(pdf("plano.pdf"));

            assertNotNull(response);
            assertEquals(result.url(), response.getUrl());
            assertEquals(MediaType.FLOOR_PLAN, response.getType());
            assertNull(response.getPropertyId());
            verify(storageService).upload(any(), eq("floor-plans/pending"));
        }

        @Test
        @DisplayName("Tipo inválido → BadRequestException incluso en modo genérico")
        void uploadGeneric_invalidType_throws() {
            MockMultipartFile bad = new MockMultipartFile("file", "bad.exe",
                    "application/octet-stream", "data".getBytes());

            assertThrows(BadRequestException.class, () -> service.uploadGeneric(bad));
        }
    }

    // ─── getByPropertyId() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getByPropertyId()")
    class GetByPropertyId {

        @Test
        @DisplayName("Propiedad no encontrada → ResourceNotFoundException")
        void get_propertyNotFound_throws() {
            when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getByPropertyId(99L));
        }

        @Test
        @DisplayName("Solo devuelve items FLOOR_PLAN, filtra otros tipos")
        void get_filtersOnlyFloorPlans() {
            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

            PropertyMedia floorPlan = PropertyMedia.builder()
                    .property(property).type(MediaType.FLOOR_PLAN)
                    .url("https://u.test/plano.pdf").isPrimary(false).orderIndex(0)
                    .build();
            floorPlan.setId(1L);

            PropertyMedia photo = PropertyMedia.builder()
                    .property(property).type(MediaType.PHOTO)
                    .url("https://u.test/foto.jpg").isPrimary(true).orderIndex(0)
                    .build();
            photo.setId(2L);

            when(mediaRepository.findByPropertyIdOrderByOrderIndexAsc(1L))
                    .thenReturn(List.of(floorPlan, photo));

            List<PropertyMediaResponse> result = service.getByPropertyId(1L);

            assertEquals(1, result.size());
            assertEquals(MediaType.FLOOR_PLAN, result.get(0).getType());
        }

        @Test
        @DisplayName("Sin planos → lista vacía")
        void get_noFloorPlans_returnsEmpty() {
            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
            when(mediaRepository.findByPropertyIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

            assertTrue(service.getByPropertyId(1L).isEmpty());
        }
    }

    // ─── delete() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Propiedad no encontrada → ResourceNotFoundException")
        void delete_propertyNotFound_throws() {
            when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.delete(99L, 1L));
        }

        @Test
        @DisplayName("Media no encontrada → ResourceNotFoundException")
        void delete_mediaNotFound_throws() {
            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
            when(mediaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.delete(1L, 999L));
        }

        @Test
        @DisplayName("Media pertenece a otra propiedad → BadRequestException")
        void delete_wrongProperty_throws() {
            Property other = new Property(); other.setId(2L);
            PropertyMedia media = PropertyMedia.builder()
                    .property(other).type(MediaType.FLOOR_PLAN).build();
            media.setId(50L);

            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
            when(mediaRepository.findById(50L)).thenReturn(Optional.of(media));

            assertThrows(BadRequestException.class, () -> service.delete(1L, 50L));
        }

        @Test
        @DisplayName("Media no es FLOOR_PLAN (es PHOTO) → BadRequestException")
        void delete_notFloorPlan_throws() {
            PropertyMedia photo = PropertyMedia.builder()
                    .property(property).type(MediaType.PHOTO).build();
            photo.setId(50L);

            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
            when(mediaRepository.findById(50L)).thenReturn(Optional.of(photo));

            assertThrows(BadRequestException.class, () -> service.delete(1L, 50L));
        }

        @Test
        @DisplayName("Éxito → elimina de DB y programa borrado físico en Storage")
        void delete_success_deletesFromRepoAndSchedulesStorage() {
            try (var mocked = mockStatic(TransactionSynchronizationManager.class)) {
                mocked.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

                PropertyMedia media = PropertyMedia.builder()
                        .property(property).type(MediaType.FLOOR_PLAN)
                        .storageKey("properties/1/floor-plans/p.pdf").build();
                media.setId(50L);

                when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
                when(mediaRepository.findById(50L)).thenReturn(Optional.of(media));

                service.delete(1L, 50L);

                verify(mediaRepository).delete(media);
                mocked.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", "pdf-content".getBytes());
    }
}
