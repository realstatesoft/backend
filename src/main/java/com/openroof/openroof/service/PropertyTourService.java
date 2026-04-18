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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyTourService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository mediaRepository;
    private final SupabaseStorageService storageService;

    @Transactional
    public PropertyMediaResponse upload360Image(Long propertyId, MultipartFile file) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        validate360Image(file);
        
        String folder = "properties/" + propertyId + "/tour/scenes";
        String url = storageService.upload(file, folder).url();

        PropertyMedia media = PropertyMedia.builder()
                .property(property)
                .type(MediaType.IMAGE_360)
                .url(url)
                .isPrimary(false)
                .orderIndex(100)
                .title(file.getOriginalFilename())
                .build();

        PropertyMedia saved = mediaRepository.save(media);
        return mapToResponse(saved);
    }

    public PropertyMediaResponse upload360ImageGeneric(MultipartFile file) {
        validate360Image(file);
        String folder = "properties/pending/tour/scenes";
        String url = storageService.upload(file, folder).url();

        return PropertyMediaResponse.builder()
                .url(url)
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

        // Delete existing config and file if any (only one tour config per property)
        mediaRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId).stream()
                .filter(m -> m.getType() == MediaType.VIRTUAL_TOUR_CONFIG)
                .forEach(m -> {
                    try {
                        // Extraer el key del URL o manejarlo según tu implementación de storage
                        // SupabaseStorageService.delete ya maneja internamente si es URL completa o path
                        storageService.delete(m.getUrl());
                    } catch (Exception e) {
                        log.warn("No se pudo borrar el archivo físico del tour anterior: {}", e.getMessage());
                    }
                    mediaRepository.delete(m);
                });

        String folder = "properties/" + propertyId + "/tour";
        String url = storageService.upload(file, folder).url();

        PropertyMedia media = PropertyMedia.builder()
                .property(property)
                .type(MediaType.VIRTUAL_TOUR_CONFIG)
                .url(url)
                .isPrimary(false)
                .orderIndex(0)
                .title("Configuración de Tour Virtual")
                .build();

        PropertyMedia saved = mediaRepository.save(media);
        return mapToResponse(saved);
    }

    public PropertyMediaResponse uploadTourConfigGeneric(MultipartFile file) {
        validateJsonConfig(file);
        String folder = "properties/pending/tour";
        String url = storageService.upload(file, folder).url();

        return PropertyMediaResponse.builder()
                .url(url)
                .type(MediaType.VIRTUAL_TOUR_CONFIG)
                .title("Configuración de Tour Virtual")
                .isPrimary(false)
                .orderIndex(0)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PropertyMediaResponse> getTourMedia(Long propertyId) {
        return mediaRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId).stream()
                .filter(m -> m.getType() == MediaType.IMAGE_360 || m.getType() == MediaType.VIRTUAL_TOUR_CONFIG)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validate360Image(MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("El archivo está vacío");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("El archivo debe ser una imagen");
        }
    }

    private void validateJsonConfig(MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("El archivo está vacío");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".json")) {
            throw new BadRequestException("La configuración debe ser un archivo .json");
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
