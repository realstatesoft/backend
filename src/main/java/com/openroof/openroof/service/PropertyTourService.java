package com.openroof.openroof.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.dto.property.PropertyMediaResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.MediaType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyMedia;
import com.openroof.openroof.repository.PropertyMediaRepository;
import com.openroof.openroof.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyTourService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository mediaRepository;
    private final SupabaseStorageService storageService;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    @Transactional
    public PropertyMediaResponse upload360Image(Long propertyId, MultipartFile file) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        validate360Image(file);
        
        String folder = "properties/" + propertyId + "/tour/scenes";
        StorageService.UploadResult result = storageService.upload(file, folder);

        PropertyMedia media = PropertyMedia.builder()
                .property(property)
                .type(MediaType.IMAGE_360)
                .url(result.url())
                .storageKey(result.filename())
                .isPrimary(false)
                .orderIndex(100)
                .title(file.getOriginalFilename())
                .build();

        PropertyMedia saved = mediaRepository.save(media);
        
        // Cleanup storage if DB save fails
        registerRollbackPurge(result.filename());
        
        return mapToResponse(saved);
    }

    @Transactional
    public PropertyMediaResponse upload360ImageGeneric(MultipartFile file) {
        validate360Image(file);
        String folder = "properties/pending/tour/scenes";
        StorageService.UploadResult result = storageService.upload(file, folder);

        return PropertyMediaResponse.builder()
                .url(result.url())
                .storageKey(result.filename())
                .type(MediaType.IMAGE_360)
                .title(file.getOriginalFilename())
                .isPrimary(false)
                .orderIndex(100)
                .build();
    }

    @Transactional
    public PropertyMediaResponse uploadTourConfig(Long propertyId, MultipartFile file) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        validateJsonConfig(file);

        // Find existing tour configs to delete later
        List<PropertyMedia> oldConfigs = mediaRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId).stream()
                .filter(m -> m.getType() == MediaType.VIRTUAL_TOUR_CONFIG)
                .toList();

        String folder = "properties/" + propertyId + "/tour";
        StorageService.UploadResult result = storageService.upload(file, folder);

        PropertyMedia media = PropertyMedia.builder()
                .property(property)
                .type(MediaType.VIRTUAL_TOUR_CONFIG)
                .url(result.url())
                .storageKey(result.filename())
                .isPrimary(false)
                .orderIndex(0)
                .title("Configuración de Tour Virtual")
                .build();

        PropertyMedia saved = mediaRepository.save(media);
        
        // Remove old entities from DB
        if (!oldConfigs.isEmpty()) {
            mediaRepository.deleteAll(oldConfigs);
        }

        // 1. Cleanup storage if NEW file fails to persist in DB
        registerRollbackPurge(result.filename());

        // 2. Cleanup OLD files from storage ONLY after successful commit
        oldConfigs.forEach(m -> {
            if (m.getStorageKey() != null) {
                registerPostCommitDeletion(m.getStorageKey());
            } else if (m.getUrl() != null) {
                // Fallback for legacy items without storageKey
                registerPostCommitDeletion(m.getUrl());
            }
        });

        return mapToResponse(saved);
    }

    @Transactional
    public PropertyMediaResponse uploadTourConfigGeneric(MultipartFile file) {
        validateJsonConfig(file);
        String folder = "properties/pending/tour";
        StorageService.UploadResult result = storageService.upload(file, folder);

        return PropertyMediaResponse.builder()
                .url(result.url())
                .storageKey(result.filename())
                .type(MediaType.VIRTUAL_TOUR_CONFIG)
                .title("Configuración de Tour Virtual")
                .isPrimary(false)
                .orderIndex(0)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PropertyMediaResponse> getTourMedia(Long propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Propiedad no encontrada");
        }
        return mediaRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId).stream()
                .filter(m -> m.getType() == MediaType.IMAGE_360 || m.getType() == MediaType.VIRTUAL_TOUR_CONFIG)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validate360Image(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("El archivo está vacío");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Tipo de imagen no permitido: " + contentType + ". Formatos aceptados: JPEG, PNG, WebP.");
        }
    }

    private void validateJsonConfig(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("El archivo está vacío");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".json")) {
            throw new BadRequestException("La configuración debe ser un archivo .json");
        }
        
        try {
            // Read and parse JSON to ensure syntax correctness
            objectMapper.readTree(file.getInputStream());
        } catch (Exception e) {
            log.error("Error validando JSON del tour: {}", e.getMessage());
            throw new BadRequestException("Sintaxis JSON inválida en el archivo de configuración.");
        }
    }

    private void registerRollbackPurge(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try {
                        storageService.delete(storageKey);
                        log.info("Purgado de Storage tras rollback exitoso: {}", storageKey);
                    } catch (Exception e) {
                        log.warn("Fallo al purgar de Storage tras rollback: {}", storageKey);
                    }
                }
            }
        });
    }

    private void registerPostCommitDeletion(String storageKeyOrUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storageService.delete(storageKeyOrUrl);
                    log.info("Eliminación física de Storage post-commit exitosa: {}", storageKeyOrUrl);
                } catch (Exception e) {
                    log.warn("No se pudo eliminar de Storage post-commit: {}", storageKeyOrUrl);
                }
            }
        });
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
