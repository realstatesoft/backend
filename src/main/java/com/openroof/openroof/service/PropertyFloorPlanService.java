package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.PropertyMediaResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.MediaType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyMedia;
import com.openroof.openroof.repository.PropertyMediaRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.upload.FileUploadValidator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Servicio para gestionar planos de propiedades (floor plans).
 * <p>
 * Acepta PDF, JPG, PNG y WebP. Almacena en Supabase Storage y registra
 * cada archivo como {@link PropertyMedia} con {@code type = FLOOR_PLAN}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyFloorPlanService {

    private final PropertyRepository         propertyRepository;
    private final PropertyMediaRepository    mediaRepository;
    private final SupabaseStorageService     storageService;
    private final FileUploadValidator        fileUploadValidator;

    @Value("${upload.floor-plans.max-file-size:10MB}")
    private String maxFileSizeRaw;

    @Value("${upload.floor-plans.allowed-types:application/pdf,image/jpeg,image/png,image/webp}")
    private String allowedTypesRaw;

    @Value("${upload.floor-plans.max-per-property:5}")
    private int maxPerProperty;

    private long maxFileSizeBytes;

    @PostConstruct
    void initConfig() {
        maxFileSizeBytes = DataSize.parse(maxFileSizeRaw.trim()).toBytes();
    }

    // ─── UPLOAD (ligado a propiedad) ─────────────────────────────────────────

    @Transactional
    public PropertyMediaResponse upload(Long propertyId, MultipartFile file) {
        validateFile(file);
        Property property = findPropertyOrThrow(propertyId);

        long current = mediaRepository.countByPropertyIdAndType(propertyId, MediaType.FLOOR_PLAN);
        if (current >= maxPerProperty) {
            throw new BadRequestException(
                    "Límite de planos alcanzado. Máximo " + maxPerProperty + " por propiedad.");
        }

        String folder = "properties/" + propertyId + "/floor-plans";
        return uploadAndSave(file, folder, property);
    }

    // ─── UPLOAD GENÉRICO (sin propertyId todavía) ────────────────────────────

    @Transactional
    public PropertyMediaResponse uploadGeneric(MultipartFile file) {
        validateFile(file);
        String folder = "floor-plans/pending";
        StorageService.UploadResult result = storageService.upload(file, folder);

        return PropertyMediaResponse.builder()
                .url(result.url())
                .storageKey(result.filename())
                .type(MediaType.FLOOR_PLAN)
                .isPrimary(false)
                .orderIndex(0)
                .title(file.getOriginalFilename())
                .build();
    }

    // ─── GET ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PropertyMediaResponse> getByPropertyId(Long propertyId) {
        findPropertyOrThrow(propertyId);
        return mediaRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId)
                .stream()
                .filter(m -> m.getType() == MediaType.FLOOR_PLAN)
                .map(this::mapToResponse)
                .toList();
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long propertyId, Long mediaId) {
        findPropertyOrThrow(propertyId);

        PropertyMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano no encontrado con ID: " + mediaId));

        if (!media.getProperty().getId().equals(propertyId)) {
            throw new BadRequestException("El plano no pertenece a la propiedad indicada.");
        }
        if (media.getType() != MediaType.FLOOR_PLAN) {
            throw new BadRequestException("El archivo indicado no es un plano.");
        }

        String storageKey = media.getStorageKey();
        mediaRepository.delete(media);
        log.info("Plano eliminado: propertyId={}, mediaId={}", propertyId, mediaId);

        // Borrado físico en Supabase DESPUÉS del commit
        if (storageKey != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        storageService.delete(storageKey);
                        log.info("Borrado físico de '{}' exitoso tras delete de plano", storageKey);
                    } catch (Exception ex) {
                        log.warn("No se pudo borrar '{}' de Storage: {}", storageKey, ex.getMessage());
                    }
                }
            });
        }
    }

    // ─── Helpers privados ────────────────────────────────────────────────────

    private PropertyMediaResponse uploadAndSave(MultipartFile file, String folder, Property property) {
        StorageService.UploadResult result = storageService.upload(file, folder);

        long order = mediaRepository.countByPropertyIdAndType(property.getId(), MediaType.FLOOR_PLAN);

        PropertyMedia media = PropertyMedia.builder()
                .property(property)
                .type(MediaType.FLOOR_PLAN)
                .url(result.url())
                .storageKey(result.filename())
                .isPrimary(false)
                .orderIndex((int) order)
                .title(file.getOriginalFilename())
                .build();

        PropertyMedia saved = mediaRepository.save(media);
        log.info("Plano guardado: propertyId={}, url={}", property.getId(), result.url());

        // Rollback compensation: si la tx falla, borrar del Storage
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try {
                        storageService.delete(result.filename());
                        log.info("Purgado '{}' tras rollback", result.filename());
                    } catch (Exception ex) {
                        log.error("Fallo al purgar '{}' tras rollback: {}", result.filename(), ex.getMessage());
                    }
                }
            }
        });

        return mapToResponse(saved);
    }

    private void validateFile(MultipartFile file) {
        var allowedKinds = FileUploadValidator.kindsFromCsv(allowedTypesRaw);
        if (allowedKinds.isEmpty()) {
            allowedKinds = FileUploadValidator.documentKinds();
        }
        fileUploadValidator.validate(file, maxFileSizeBytes, maxFileSizeRaw.trim(), allowedKinds);
    }

    private Property findPropertyOrThrow(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada con ID: " + id));
    }

    private PropertyMediaResponse mapToResponse(PropertyMedia m) {
        return PropertyMediaResponse.builder()
                .id(m.getId())
                .propertyId(m.getProperty().getId())
                .url(m.getUrl())
                .storageKey(m.getStorageKey())
                .type(m.getType())
                .isPrimary(m.getIsPrimary())
                .orderIndex(m.getOrderIndex())
                .title(m.getTitle())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
