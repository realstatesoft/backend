package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.property.*;
import com.openroof.openroof.model.search.PropertySpecification;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PropertyService {

    /**
     * Campos permitidos para ordenar. Cualquier otro valor se reemplaza por
     * 'createdAt'.
     */
    private static final Set<String> VALID_SORT_FIELDS = Set.of(
            "createdAt", "price", "bedrooms", "bathrooms", "surfaceArea", "title");

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final ExteriorFeatureRepository exteriorFeatureRepository;
    private final InteriorFeatureRepository interiorFeatureRepository;
    private final PropertyMapper propertyMapper;

    private static final double EARTH_RADIUS = 6371;

    // progressive radius for searching similar properties
    private static final double[] SEARCH_RADIUS = {1.0, 2.0, 3.0, 5.0, 8.0, 10.0, 15.0, 20.0, 30.0, 50.0};

    // price variation percentage for searching similar properties
    private static final double PRICE_VARIATION = 0.30;

    private static final double DISTANCE_WEIGHT = 0.5;
    private static final double PRICE_WEIGHT = 0.3;
    private static final double BEDROOMS_WEIGHT = 0.1;
    private static final double BATHROOMS_WEIGHT = 0.1;
    private static final double MAX_DISTANCE_KM = 10.0;

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
    public Page<PropertySummaryResponse> getAll(PropertyFilterRequest filter, Pageable pageable) {
        Specification<Property> spec = PropertySpecification.buildFilter(filter);
        return propertyRepository.findAll(spec, sanitizePageable(pageable))
                .map(propertyMapper::toSummaryResponse);
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
        if (property.isDeleted())
            throw new BadRequestException("La propiedad ya ha sido eliminada definitivamente");
        if (property.getTrashedAt() != null)
            throw new BadRequestException("La propiedad ya está en la papelera");

        // else set trashed at date
        property.setTrashedAt(LocalDateTime.now());
        propertyRepository.save(property);
        return propertyMapper.toResponse(property);
    }

    public PropertyResponse restoreFromTrashcan(Long id) {
        Property property = findPropertyOrThrow(id);

        // check if property is already deleted or in trashcan
        if (property.isDeleted())
            throw new BadRequestException("La propiedad ya ha sido eliminada definitivamente");
        if (property.getTrashedAt() == null)
            throw new BadRequestException("La propiedad no se encuentra en la papelera");

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
        if (limit <= 0 || limit > 20) {
            throw new BadRequestException("El limite debe estar entre 1 y 20");
        }

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada con ID: " + propertyId));

        Location location = property.getLocation();
        if (location == null) {
            log.warn("Property with id: {} has no location, using fallback search", propertyId);
            return fallbackSearch(property, limit).stream()
                    .map(propertyMapper::toResponse)
                    .toList();
        }

        // PRICE RANGE
        BigDecimal basePrice = property.getPrice();
        BigDecimal minPrice = basePrice.multiply(BigDecimal.valueOf(1 - PRICE_VARIATION));
        BigDecimal maxPrice = basePrice.multiply(BigDecimal.valueOf(1 + PRICE_VARIATION));

        String propertyType = property.getPropertyType().name();

        // COORDINATES CHECK
        if (!location.hasCoordinates()) {
            log.warn("Property with id: {} has no coordinates, using fallback search method", propertyId);
            return fallbackSearch(property, limit).stream()
                    .map(propertyMapper::toResponse)
                    .toList();
        }

        Double baseLat = location.getLat();
        Double baseLng = location.getLng();

        Set<Long> seenPropertyIds = new HashSet<>();
        seenPropertyIds.add(propertyId); // exclude base property

        List<Property> allSuggestions = new ArrayList<>();

        // SEARCH WITH INCREASING RADIUS
        for (double radius : SEARCH_RADIUS) {
            // Calculate bounding box for current radius
            BoundingBox bbox = calculateBoundingBox(baseLat, baseLng, radius);

            List<Property> suggestions = propertyRepository.findNearbyProperties(
                    propertyId,
                    baseLat,
                    baseLng,
                    bbox.getMinLat(),
                    bbox.getMaxLat(),
                    bbox.getMinLng(),
                    bbox.getMaxLng(),
                    propertyType,
                    minPrice,
                    maxPrice,
                    basePrice,
                    radius,
                    limit * 3
            );

            // filter duplicates before adding to suggestions
            List<Property> newSuggestions = suggestions.stream()
                    .filter(p -> !seenPropertyIds.contains(p.getId()))
                    .peek(p -> seenPropertyIds.add(p.getId()))
                    .toList();

            allSuggestions.addAll(newSuggestions);

            // when we have enough properties, rank them
            if (allSuggestions.size() >= limit) {
                log.debug("Accumulated {} unique suggestions within {}km", allSuggestions.size(), radius);
                return rankAndLimit(allSuggestions, property, limit).stream()
                        .map(propertyMapper::toResponse)
                        .toList();
            }
        }

        // FALLBACK TO CITY SEARCH
        if (allSuggestions.size() < limit) {
            log.debug("Not enough nearby properties ({}), expanding to city search", allSuggestions.size());
            int remainingNeeded = limit - allSuggestions.size();

            // add more properties if needed
            List<Property> citySuggestions = fallbackSearch(property, remainingNeeded * 2)
                    .stream()
                    .filter(p -> !seenPropertyIds.contains(p.getId()))
                    .peek(p -> seenPropertyIds.add(p.getId()))
                    .limit(remainingNeeded)
                    .toList();

            allSuggestions.addAll(citySuggestions);
        }

        return rankAndLimit(allSuggestions, property, limit).stream()
                .map(propertyMapper::toResponse)
                .toList();
    }

    // rank candidates according to their score
    private List<Property> rankAndLimit(List<Property> candidates, Property base, int limit) {
        // distinct, rank and limit
        return candidates.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(candidate -> new ScoredProperty(candidate, calculateRelevanceScore(base, candidate)))
                .sorted((s1, s2) -> Double.compare(s2.getScore(), s1.getScore()))
                .limit(limit)
                .map(ScoredProperty::getProperty)
                .toList();
    }

    // Weighted similarity scoring
    private double calculateRelevanceScore(Property base, Property candidate) {
        if (candidate == null) return 0.0;

        double distanceScore = calculateDistanceScore(base, candidate);
        double priceScore = calculatePriceScore(base, candidate);
        double bedroomsScore = calculateBedroomsScore(base, candidate);
        double bathroomsScore = calculateBathroomsScore(base, candidate);

        // calculate final weighted score
        return (distanceScore * DISTANCE_WEIGHT) +
                (priceScore * PRICE_WEIGHT) +
                (bedroomsScore * BEDROOMS_WEIGHT) +
                (bathroomsScore * BATHROOMS_WEIGHT);
    }

    // helpers for calculating all criteria
    private double calculateDistanceScore(Property base, Property candidate) {
        // check if properties have location and coordinates
        if ((base.getLocation() == null || candidate.getLocation() == null)
                && (base.getGeoLocation() == null || candidate.getGeoLocation() == null)) {
            return 0.5; // Neutral score
        }

        if (!base.hasCoordinates() || !candidate.hasCoordinates()) {
            return 0.5; // Neutral score
        }

        double distance = haversineDistance(
                base.getLat(),
                base.getLng(),
                candidate.getLat(),
                candidate.getLng()
        );

        // 1.0 for 0km, 0.5 for 5km, 0.0 for 10km+
        return Math.max(0, 1.0 - (distance / MAX_DISTANCE_KM));
    }

    private double calculatePriceScore(Property base, Property candidate) {
        if (base.getPrice() == null || base.getPrice().signum() <= 0) {
            return candidate.getPrice() != null && candidate.getPrice().signum() > 0 ? 1.0 : 0.0;
        }

        if (candidate.getPrice() == null || candidate.getPrice().signum() <= 0) {
            return 0.0;
        }

        double priceDiff = Math.abs(
                base.getPrice().doubleValue() - candidate.getPrice().doubleValue()
        );
        double maxDiff = base.getPrice().doubleValue() * PRICE_VARIATION;
        return Math.max(0, 1.0 - (priceDiff / maxDiff));
    }

    private double calculateBedroomsScore(Property base, Property candidate) {
        int baseBedrooms = Objects.requireNonNullElse(base.getBedrooms(), 0);
        int candidateBedrooms = Objects.requireNonNullElse(candidate.getBedrooms(), 0);
        int diff = Math.abs(baseBedrooms - candidateBedrooms);

        // assign scores according to difference
        return switch (diff) {
            case 0 -> 1.0;
            case 1 -> 0.7;
            case 2 -> 0.4;
            default -> 0.1;
        };
    }

    private double calculateBathroomsScore(Property base, Property candidate) {
        double baseBathrooms = base.getBathrooms() != null ? base.getBathrooms().doubleValue() : 0.0;
        double candidateBathrooms = candidate.getBathrooms() != null ? candidate.getBathrooms().doubleValue() : 0.0;
        double diff = Math.abs(baseBathrooms - candidateBathrooms);

        if (diff <= 0.5) return 1.0;
        if (diff <= 1.0) return 0.7;
        if (diff <= 2.0) return 0.4;
        return 0.1;
    }

    // haversine distance: used to find distance between two points in a sphere
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    // search for property without coordinates
    private List<Property> fallbackSearch(Property property, int limit) {
        if (limit <= 0) return List.of();

        if (property.getLocation() == null) {
            log.warn("Property {} has no location, cannot perform city search", property.getId());
            return List.of();
        }

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

    /**
     * Filtra los campos de sort del Pageable dejando solo los que están en
     * VALID_SORT_FIELDS. Si ninguno es válido (p.ej. Swagger envía "string"),
     * usa el default: createdAt DESC.
     */
    private Pageable sanitizePageable(Pageable pageable) {
        List<Sort.Order> safeOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (VALID_SORT_FIELDS.contains(order.getProperty())) {
                safeOrders.add(order);
            }
        }
        Sort safeSort = safeOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(safeOrders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
    }

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


    // calculate a 'bounding box' around given location using spherical aproximation
    private BoundingBox calculateBoundingBox(double lat, double lng, double radiusKm) {
        double latDelta = Math.toDegrees(radiusKm / EARTH_RADIUS);
        double lngDelta = Math.toDegrees(radiusKm / (EARTH_RADIUS * Math.cos(Math.toRadians(lat))));

        return BoundingBox.builder()
                .minLat(lat - latDelta)
                .maxLat(lat + latDelta)
                .minLng(lng - lngDelta)
                .maxLng(lng + lngDelta)
                .build();
    }

    @Builder
    @Getter
    private static class BoundingBox {
        private double minLat;
        private double maxLat;
        private double minLng;
        private double maxLng;

        // latitude and longitude limits (with wrap-around for 90 / 180 degrees)
        public double getMinLat() { return Math.max(-90, minLat); }
        public double getMaxLat() { return Math.min(90, maxLat); }

        public double getMinLng() { return Math.max(-180, minLng); }
        public double getMaxLng() { return Math.min(180, maxLng); }
    }
}

