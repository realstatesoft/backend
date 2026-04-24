package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.PropertyMediaResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.MediaType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyMedia;
import com.openroof.openroof.repository.PropertyMediaRepository;
import com.openroof.openroof.repository.PropertyRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyModelService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository mediaRepository;
    private final SupabaseStorageService storageService;

    @Value("${upload.models.allowed-types:model/gltf-binary,model/gltf+json}")
    private String allowedTypesCsv;

    @Value("${upload.models.max-file-size:20MB}")
    private String maxModelFileSizeRaw;

    private DataSize maxModelFileSize;
    private Set<String> normalizedAllowedTypes;

    @PostConstruct
    void initConfig() {
        this.maxModelFileSize = DataSize.parse(maxModelFileSizeRaw.trim());
        this.normalizedAllowedTypes = Arrays.stream(allowedTypesCsv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Transactional
    public PropertyMediaResponse uploadModel(Long propertyId, MultipartFile file) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        String folder = "properties/" + propertyId + "/models";
        return uploadAndSave(file, folder, property);
    }

    @Transactional
    public PropertyMediaResponse uploadModelGeneric(MultipartFile file) {
        validateModelFile(file);

        // Upload to a generic pending folder
        String folder = "models/pending";
        StorageService.UploadResult result = storageService.upload(file, folder);

        return PropertyMediaResponse.builder()
                .url(result.url())
                .storageKey(result.filename())
                .type(MediaType.MODEL_3D)
                .isPrimary(false)
                .orderIndex(100)
                .title("Modelo 3D (Pendiente)")
                .build();
    }

    private PropertyMediaResponse uploadAndSave(MultipartFile file, String folder, Property property) {
        validateModelFile(file);
        StorageService.UploadResult result = storageService.upload(file, folder);

        PropertyMedia media = PropertyMedia.builder()
                .property(property)
                .type(MediaType.MODEL_3D)
                .url(result.url())
                .storageKey(result.filename())
                .isPrimary(false)
                .orderIndex(100)
                .title("Modelo 3D")
                .build();

        PropertyMedia saved = mediaRepository.save(media);
        log.info("Modelo 3D guardado en DB y Storage: {}", result.url());

        // Register rollback compensation: delete from storage if DB transaction fails
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try {
                        storageService.delete(result.filename());
                        log.info("Purgado de Storage '{}' exitoso tras rollback", result.filename());
                    } catch (Exception ex) {
                        log.error("Fallo al purgar de Storage '{}' tras rollback: {}", result.filename(), ex.getMessage());
                    }
                }
            }
        });

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PropertyMediaResponse> getModelsByPropertyId(Long propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Propiedad no encontrada");
        }
        return mediaRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId).stream()
                .filter(m -> m.getType() == MediaType.MODEL_3D)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteModel(Long propertyId, Long mediaId) {
        PropertyMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo no encontrado"));

        if (!media.getProperty().getId().equals(propertyId)) {
            throw new BadRequestException("El modelo no pertenece a esta propiedad");
        }

        if (media.getType() != MediaType.MODEL_3D) {
            throw new BadRequestException("El archivo no es un modelo 3D");
        }

        String storageKey = media.getStorageKey();
        mediaRepository.delete(media);
        
        // Finalize deletion in storage AFTER commit to ensure data integrity
        if (storageKey != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        storageService.delete(storageKey);
                        log.info("Borrado físico de Storage '{}' exitoso tras deleteModel", storageKey);
                    } catch (Exception ex) {
                        log.warn("No se pudo borrar físicamente '{}' de Storage tras commit: {}", storageKey, ex.getMessage());
                    }
                }
            });
        }
    }

    private void validateModelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }

        if (file.getSize() > maxModelFileSize.toBytes()) {
            throw new BadRequestException("El modelo supera el tamaño máximo permitido de " + maxModelFileSizeRaw);
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean isAllowedType = contentType != null && normalizedAllowedTypes.contains(contentType.toLowerCase().trim());
        
        // Extension check (case-insensitive) as safety/fallback
        boolean hasValidExtension = filename != null && 
                (filename.toLowerCase().endsWith(".glb") || filename.toLowerCase().endsWith(".gltf"));

        if (!isAllowedType && !hasValidExtension) {
            throw new BadRequestException("Tipo de archivo no permitido: " + contentType + ". Solo se admiten .glb y .gltf");
        }
    }

    private PropertyMediaResponse mapToResponse(PropertyMedia m) {
        return PropertyMediaResponse.builder()
                .id(m.getId())
                .propertyId(m.getProperty().getId())
                .url(m.getUrl())
                .type(m.getType())
                .isPrimary(m.getIsPrimary())
                .orderIndex(m.getOrderIndex())
                .title(m.getTitle())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
