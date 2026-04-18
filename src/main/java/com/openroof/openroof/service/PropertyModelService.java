package com.openroof.openroof.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    @Value("${upload.models.allowed-types}")
    private String allowedTypesCsv;

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
        String url = storageService.upload(file, folder).url();

        return PropertyMediaResponse.builder()
                .url(url)
                .type(MediaType.MODEL_3D)
                .isPrimary(false)
                .orderIndex(100)
                .title("Modelo 3D (Pendiente)")
                .build();
    }

    private PropertyMediaResponse uploadAndSave(MultipartFile file, String folder, Property property) {
        validateModelFile(file);
        String url = storageService.upload(file, folder).url();

        PropertyMedia media = PropertyMedia.builder()
                .property(property)
                .type(MediaType.MODEL_3D)
                .url(url)
                .isPrimary(false)
                .orderIndex(100)
                .title("Modelo 3D")
                .build();

        PropertyMedia saved = mediaRepository.save(media);
        log.info("Modelo 3D guardado: {}", url);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PropertyMediaResponse> getModelsByPropertyId(Long propertyId) {
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

        mediaRepository.delete(media);
        log.info("Modelo 3D {} eliminado de la propiedad {}", mediaId, propertyId);
    }

    private void validateModelFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }

        String contentType = file.getContentType();
        Set<String> allowed = Set.of(allowedTypesCsv.split(","));

        if (contentType == null || !allowed.contains(contentType)) {
            // Check extension as fallback
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".glb") && !filename.endsWith(".gltf"))) {
                throw new BadRequestException("Tipo de archivo no permitido: " + contentType + ". Solo se admiten .glb y .gltf");
            }
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
