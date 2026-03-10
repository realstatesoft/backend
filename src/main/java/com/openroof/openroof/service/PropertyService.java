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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final ExteriorFeatureRepository exteriorFeatureRepository;
    private final InteriorFeatureRepository interiorFeatureRepository;
    private final PropertyMapper propertyMapper;

    // progressive radius for searching similar properties
    private static final double[] SEARCH_RADIUS = {1.0, 2.0, 3.0, 5.0, 8.0, 10.0, 15.0, 20.0, 30.0, 50.0};

    // price variation percentage for searching similar properties
    private static final double PRICE_VARIATION = 0.30;

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

    // ─── RECOMMENDATION ALGORITHM ─────────────────────────────────
    // Uses weighted score system
    // 1. find nearby properties of the same type, to apply similarity criteria to them
    // 2. Each criterion is scored and normalized in a value from 1.0 (completely equal) to 0.0 (unequal)
    // 3. each criterion is weighted to get the final similarity score

    public List<PropertyResponse> findSimilarProperties(Long propertyId, int limit) {
        Property property = propertyRepository.findById(propertyId).orElseThrow();

        // check if property has coordinates
        if (!property.getLocation().hasCoordinates()) {
            log.warn("Property with id: {} has no coordinates, using fallback search method", propertyId);
            return fallbackSearch(property, limit).stream().map(propertyMapper::toResponse).toList();
        }

        // price range
        BigDecimal basePrice = property.getPrice();
        BigDecimal minPrice = basePrice.multiply(BigDecimal.valueOf(1 - PRICE_VARIATION));
        BigDecimal maxPrice = basePrice.multiply(BigDecimal.valueOf(1 + PRICE_VARIATION));

        List<Property> allSuggestions = new ArrayList<>();

        // search nearby properties with increasing radius
        // get limit * 2 to rank by similarity score
        for (double radius : SEARCH_RADIUS) {
            List<Property> suggestions = propertyRepository.findNearbyProperties(
                    propertyId,
                    property.getLocation().getLat(),
                    property.getLocation().getLng(),
                    property.getPropertyType().name(),
                    minPrice,
                    maxPrice,
                    basePrice,
                    radius,
                    limit * 2
            );

            // rank nearby properties according to  similarity criteria
            if (suggestions.size() >= limit) {
                log.debug("Found {} suggestions within {}km", suggestions.size(), radius);
                return rankAndLimit(suggestions, property, limit).stream().map(propertyMapper::toResponse).toList();
            }

            allSuggestions.addAll(suggestions);

            if (allSuggestions.size() >= limit) {
                log.debug("Accumulated {} suggestions", allSuggestions.size());
                return rankAndLimit(allSuggestions, property, limit).stream().map(propertyMapper::toResponse).toList();
            }
        }

        // if not enough properties are found searching by radius, search by city
        if (allSuggestions.size() < limit) {
            log.debug("Not enough nearby properties, expanding to city search");
            List<Property> citySuggestions = fallbackSearch(property, limit - allSuggestions.size());
            allSuggestions.addAll(citySuggestions);
        }

        return rankAndLimit(allSuggestions, property, limit).stream().map(propertyMapper::toResponse).toList();
    }

    // rank candidates according to their score
    private List<Property> rankAndLimit(List<Property> candidates, Property base, int limit) {
        return candidates.stream()
                .map(candidate -> new ScoredProperty(candidate, calculateRelevanceScore(base, candidate)))
                .sorted((s1, s2) -> Double.compare(s2.getScore(), s1.getScore()))
                .limit(limit)
                .map(ScoredProperty::getProperty)
                .distinct()
                .toList();
    }

    // Weighted similarity scoring
    private double calculateRelevanceScore(Property base, Property candidate) {
        // calculate scores for all criteria
        double distanceScore = calculateDistanceScore(base, candidate);
        double priceScore = calculatePriceScore(base, candidate);
        double bedroomsScore = calculateBedroomsScore(base, candidate);
        double bathroomsScore = calculateBathroomsScore(base, candidate);

        // calculate final weighted score
        return (distanceScore * 0.5) + // 50%
                (priceScore * 0.3) + // 30%
                (bedroomsScore * 0.1) + // 10%
                (bathroomsScore * 0.1); // 10%
    }

    // helpers for calculating all criteria
    // each one returns a double between 0.0 or 1.0
    private double calculateDistanceScore(Property base, Property candidate) {
        // if no coordinates, neutral score
        if (!base.getLocation().hasCoordinates() || !candidate.getLocation().hasCoordinates()) {
            return 0.5;
        }

        double distance = haversineDistance(
                base.getLocation().getLat(),
                base.getLocation().getLng(),
                candidate.getLocation().getLat(),
                candidate.getLocation().getLng()
        );

        // 1.0 for 0km, 0.5 for 5km, 0.0 for 10km+
        return Math.max(0, 1.0 - (distance / 10.0));
    }


    private double calculatePriceScore(Property base, Property candidate) {
        double priceDiff = Math.abs(
                base.getPrice().doubleValue() - candidate.getPrice().doubleValue()
        );
        double maxDiff = base.getPrice().doubleValue() * PRICE_VARIATION;
        return Math.max(0, 1.0 - (priceDiff / maxDiff));
    }

    private double calculateBedroomsScore(Property base, Property candidate) {
        int diff = Math.abs(base.getBedrooms() - candidate.getBedrooms());

        // assign score according to difference in rooms
        return diff == 0 ? 1.0 : diff == 1 ? 0.7 : diff == 2 ? 0.4 : 0.1;
    }

    private double calculateBathroomsScore(Property base, Property candidate) {
        double diff = Math.abs(
                base.getBathrooms().doubleValue() - candidate.getBathrooms().doubleValue()
        );
        // assign score according to difference in bathrooms
        return diff <= 0.5 ? 1.0 : diff <= 1.0 ? 0.7 : diff <= 2.0 ? 0.4 : 0.1;
    }

    // haversine distance: used to find distance between two points in a sphere
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // earth radius

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // search for property without coordinates
    private List<Property> fallbackSearch(Property property, int limit) {
        if (limit <= 0) return List.of();

        BigDecimal basePrice = property.getPrice();
        BigDecimal minPrice = basePrice.multiply(BigDecimal.valueOf(1 - PRICE_VARIATION));
        BigDecimal maxPrice = basePrice.multiply(BigDecimal.valueOf(1 + PRICE_VARIATION));

        return propertyRepository.findByCity(
                property.getId(),
                property.getLocation().getCity(),
                property.getPropertyType().name(),
                minPrice,
                maxPrice,
                basePrice,
                limit
        );
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

    // helper class for recommendation algorithm
    @lombok.Value
    private static class ScoredProperty {
        Property property;
        double score;
    }
}
