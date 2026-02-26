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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar imágenes de propiedades.
 * Sube a Supabase Storage y crea registros en property_media.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyImageService {

    private final StorageService storageService;
    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository propertyMediaRepository;

    private static final String STORAGE_FOLDER = "properties";
    private static final int MAX_IMAGES_PER_PROPERTY = 20;

    // ─── UPLOAD (una o varias imágenes) ──────────────────────────

    /**
     * Sube una o varias imágenes para una propiedad.
     *
     * @param propertyId  ID de la propiedad
     * @param files       archivos de imagen
     * @param isPrimary   la primera imagen es la principal?
     * @return lista de PropertyMediaResponse
     */
    @Transactional
    public List<PropertyMediaResponse> uploadImages(
            Long propertyId,
            List<MultipartFile> files,
            boolean isPrimary
    ) {
        Property property = findPropertyOrThrow(propertyId);

        // Validar límite
        long currentCount = propertyMediaRepository.countByPropertyId(propertyId);
        if (currentCount + files.size() > MAX_IMAGES_PER_PROPERTY) {
            throw new BadRequestException(
                    "Límite de imágenes excedido. Máximo " + MAX_IMAGES_PER_PROPERTY
                            + ", actuales: " + currentCount + ", intentando agregar: " + files.size());
        }

        // Si se marca como primary, desmarcar la actual
        if (isPrimary) {
            propertyMediaRepository.findByPropertyIdAndIsPrimaryTrue(propertyId)
                    .ifPresent(existing -> {
                        existing.setIsPrimary(false);
                        propertyMediaRepository.save(existing);
                    });
        }

        int nextOrder = (int) currentCount;
        List<PropertyMediaResponse> responses = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String folder = STORAGE_FOLDER + "/" + propertyId;

            // Subir a Storage
            StorageService.UploadResult result = storageService.upload(file, folder);

            // Crear registro en property_media
            PropertyMedia media = PropertyMedia.builder()
                    .property(property)
                    .type(MediaType.PHOTO)
                    .url(result.url())
                    .isPrimary(isPrimary && i == 0) // solo la primera si isPrimary=true
                    .orderIndex(nextOrder + i)
                    .title(file.getOriginalFilename())
                    .build();

            media = propertyMediaRepository.save(media);

            responses.add(toResponse(media, propertyId));

            log.info("Imagen subida para propiedad {}: {} ({} bytes)",
                    propertyId, result.url(), result.size());
        }

        return responses;
    }

    // ─── GET: todas las imágenes de una propiedad ────────────────

    @Transactional(readOnly = true)
    public List<PropertyMediaResponse> getByPropertyId(Long propertyId) {
        // Verificar que la propiedad existe
        findPropertyOrThrow(propertyId);

        return propertyMediaRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId)
                .stream()
                .map(media -> toResponse(media, propertyId))
                .toList();
    }

    // ─── GET: imagen principal de una propiedad ──────────────────

    @Transactional(readOnly = true)
    public PropertyMediaResponse getPrimaryImage(Long propertyId) {
        findPropertyOrThrow(propertyId);

        PropertyMedia media = propertyMediaRepository.findByPropertyIdAndIsPrimaryTrue(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró imagen principal para la propiedad: " + propertyId));

        return toResponse(media, propertyId);
    }

    // ─── SET PRIMARY ─────────────────────────────────────────────

    @Transactional
    public PropertyMediaResponse setPrimaryImage(Long propertyId, Long mediaId) {
        findPropertyOrThrow(propertyId);

        // Desmarcar actual primary
        propertyMediaRepository.findByPropertyIdAndIsPrimaryTrue(propertyId)
                .ifPresent(existing -> {
                    existing.setIsPrimary(false);
                    propertyMediaRepository.save(existing);
                });

        // Marcar nueva
        PropertyMedia media = propertyMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Media no encontrada con ID: " + mediaId));

        if (!media.getProperty().getId().equals(propertyId)) {
            throw new BadRequestException("La media no pertenece a la propiedad indicada");
        }

        media.setIsPrimary(true);
        media = propertyMediaRepository.save(media);

        return toResponse(media, propertyId);
    }

    // ─── DELETE ──────────────────────────────────────────────────

    @Transactional
    public void deleteImage(Long propertyId, Long mediaId) {
        findPropertyOrThrow(propertyId);

        PropertyMedia media = propertyMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Media no encontrada con ID: " + mediaId));

        if (!media.getProperty().getId().equals(propertyId)) {
            throw new BadRequestException("La media no pertenece a la propiedad indicada");
        }

        propertyMediaRepository.delete(media);
        log.info("Imagen eliminada: propertyId={}, mediaId={}", propertyId, mediaId);
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private Property findPropertyOrThrow(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propiedad no encontrada con ID: " + id));
    }

    private PropertyMediaResponse toResponse(PropertyMedia media, Long propertyId) {
        return PropertyMediaResponse.builder()
                .id(media.getId())
                .propertyId(propertyId)
                .url(media.getUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .type(media.getType())
                .isPrimary(media.getIsPrimary())
                .orderIndex(media.getOrderIndex())
                .title(media.getTitle())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
