package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.property.*;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final ExteriorFeatureRepository exteriorFeatureRepository;
    private final InteriorFeatureRepository interiorFeatureRepository;
    private final PropertyMapper propertyMapper;

    // ─── CREATE ───────────────────────────────────────────────────

    public PropertyResponse create(CreatePropertyRequest request) {
        // Resolver owner (obligatorio)
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con ID: " + request.ownerId()));

        // Crear entidad base
        Property property = propertyMapper.toEntity(request);
        property.setOwner(owner);

        // Resolver agent (opcional)
        if (request.agentId() != null) {
            AgentProfile agent = agentProfileRepository.findById(request.agentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Agente no encontrado con ID: " + request.agentId()));
            property.setAgent(agent);
        }

        // Resolver location (opcional)
        if (request.locationId() != null) {
            Location location = locationRepository.findById(request.locationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ubicación no encontrada con ID: " + request.locationId()));
            property.setLocation(location);
        }

        // Resolver exterior features (opcional)
        if (request.exteriorFeatureIds() != null && !request.exteriorFeatureIds().isEmpty()) {
            List<ExteriorFeature> features = exteriorFeatureRepository
                    .findAllById(request.exteriorFeatureIds());
            if (features.size() != request.exteriorFeatureIds().size()) {
                throw new BadRequestException("Algunas características exteriores no fueron encontradas");
            }
            property.setExteriorFeatures(features);
        }

        // Guardar primero para tener el ID
        property = propertyRepository.save(property);

        // Crear rooms (con interior features)
        if (request.rooms() != null && !request.rooms().isEmpty()) {
            List<PropertyRoom> rooms = buildRooms(request.rooms(), property);
            property.getRooms().addAll(rooms);
        }

        // Crear media
        if (request.media() != null && !request.media().isEmpty()) {
            List<PropertyMedia> mediaList = buildMedia(request.media(), property);
            property.getMedia().addAll(mediaList);
        }

        property = propertyRepository.save(property);
        return propertyMapper.toResponse(property);
    }

    // ─── READ ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PropertyResponse getById(Long id) {
        Property property = findPropertyOrThrow(id);
        return propertyMapper.toResponse(property);
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getAll(String propertyType, String status, Pageable pageable) {
        PropertyType type = propertyType != null ? PropertyType.valueOf(propertyType) : null;
        PropertyStatus st = status != null ? PropertyStatus.valueOf(status) : null;

        if (type == null && st == null) {
            return propertyRepository.findAllByTrashedAtIsNull(pageable)
                    .map(propertyMapper::toSummaryResponse);
        }

        return propertyRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (type != null)
                predicates.add(cb.equal(root.get("propertyType"), type));
            if (st != null)
                predicates.add(cb.equal(root.get("status"), st));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable).map(propertyMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getByOwner(Long ownerId, Pageable pageable) {
        return propertyRepository.findByOwner_IdAndTrashedAtIsNull(ownerId, pageable)
                .map(propertyMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return propertyRepository.findAll(pageable)
                    .map(propertyMapper::toSummaryResponse);
        }
        return propertyRepository.searchByKeyword(keyword.trim(), pageable)
                .map(propertyMapper::toSummaryResponse);
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    public PropertyResponse update(Long id, UpdatePropertyRequest request) {
        Property property = findPropertyOrThrow(id);

        // Actualizar campos básicos via mapper
        propertyMapper.updateEntity(property, request);

        // Resolver agent (si cambia)
        if (request.agentId() != null) {
            AgentProfile agent = agentProfileRepository.findById(request.agentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Agente no encontrado con ID: " + request.agentId()));
            property.setAgent(agent);
        }

        // Resolver location (si cambia)
        if (request.locationId() != null) {
            Location location = locationRepository.findById(request.locationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ubicación no encontrada con ID: " + request.locationId()));
            property.setLocation(location);
        }

        // Reemplazar exterior features (si se envían)
        if (request.exteriorFeatureIds() != null) {
            List<ExteriorFeature> features = request.exteriorFeatureIds().isEmpty()
                    ? new ArrayList<>()
                    : exteriorFeatureRepository.findAllById(request.exteriorFeatureIds());
            property.setExteriorFeatures(features);
        }

        // Reemplazar rooms (si se envían)
        if (request.rooms() != null) {
            property.getRooms().clear();
            if (!request.rooms().isEmpty()) {
                List<PropertyRoom> rooms = buildRooms(request.rooms(), property);
                property.getRooms().addAll(rooms);
            }
        }

        // Reemplazar media (si se envían)
        if (request.media() != null) {
            property.getMedia().clear();
            if (!request.media().isEmpty()) {
                List<PropertyMedia> mediaList = buildMedia(request.media(), property);
                property.getMedia().addAll(mediaList);
            }
        }

        property = propertyRepository.save(property);
        return propertyMapper.toResponse(property);
    }

    // ─── DELETE (Soft) ────────────────────────────────────────────

    public void delete(Long id) {
        Property property = findPropertyOrThrow(id);
        property.softDelete();
        propertyRepository.save(property);
    }

    public PropertyResponse trash(Long id) {
        Property property = findPropertyOrThrow(id);

        // check if property is already deleted or in trashcan
        if (property.isDeleted()) throw new BadRequestException("La propiedad ya ha sido eliminada definitivamente");
        if (property.getTrashedAt() != null) throw new BadRequestException("La propiedad ya está en la papelera");

        // else set trashed at date
        property.setTrashedAt(LocalDateTime.now());
        propertyRepository.save(property);
        return propertyMapper.toResponse(property);
    }

    public PropertyResponse restoreFromTrashcan(Long id) {
        Property property = findPropertyOrThrow(id);

        // check if property is already deleted or in trashcan
        if (property.isDeleted()) throw new BadRequestException("La propiedad ya ha sido eliminada definitivamente");
        if (property.getTrashedAt() == null) throw new BadRequestException("La propiedad no se encuentra en la papelera");

        // else set trashedAt == null
        property.setTrashedAt(null);
        propertyRepository.save(property);
        return propertyMapper.toResponse(property);
    }

    @Scheduled(cron = "0 0 3 * * *") // every day at 3 am
    public void cleanExpiredTrash() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(10);
        propertyRepository.deleteExpiredTrash(threshold, LocalDateTime.now());
    }

    // clear trashcan of a given user, returns deleted count
    public int clearTrashcanForUser(Long ownerId) {
        return propertyRepository.clearTrashcanByOwner(ownerId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getTrashcan(Long ownerId, Pageable pageable) {
        return propertyRepository.findByOwnerIdAndTrashedAtIsNotNull(ownerId, pageable)
                .map(propertyMapper::toSummaryResponse);
    }


    // ─── CHANGE STATUS ────────────────────────────────────────────

    public PropertyResponse changeStatus(Long id, PropertyStatus newStatus) {
        Property property = findPropertyOrThrow(id);
        PropertyStatus currentStatus = property.getStatus();

        // Validar transición usando la máquina de estados del enum
        PropertyStatus validated = currentStatus.transitionTo(newStatus);
        property.setStatus(validated);

        // Si se publica, registrar fecha de publicación
        if (validated == PropertyStatus.PUBLISHED && property.getTrashedAt() == null) {
            property.setTrashedAt(LocalDateTime.now());
        }

        property = propertyRepository.save(property);
        return propertyMapper.toResponse(property);
    }


    
    // ─── Helpers privados ─────────────────────────────────────────

    private Property findPropertyOrThrow(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propiedad no encontrada con ID: " + id));
    }

    private List<PropertyRoom> buildRooms(List<PropertyRoomDto> roomDtos, Property property) {
        return roomDtos.stream().map(dto -> {
            PropertyRoom room = PropertyRoom.builder()
                    .property(property)
                    .name(dto.name())
                    .area(dto.area())
                    .build();

            // Resolver interior features
            if (dto.interiorFeatureIds() != null && !dto.interiorFeatureIds().isEmpty()) {
                List<InteriorFeature> features = interiorFeatureRepository
                        .findAllById(dto.interiorFeatureIds());
                room.setFeatures(features);
            }

            return room;
        }).toList();
    }

    private List<PropertyMedia> buildMedia(List<PropertyMediaDto> mediaDtos, Property property) {
        return mediaDtos.stream().map(dto -> PropertyMedia.builder()
                .property(property)
                .type(dto.type())
                .url(dto.url())
                .thumbnailUrl(dto.thumbnailUrl())
                .isPrimary(dto.isPrimary() != null ? dto.isPrimary() : false)
                .orderIndex(dto.orderIndex() != null ? dto.orderIndex() : 0)
                .title(dto.title())
                .build()).toList();
    }
}
