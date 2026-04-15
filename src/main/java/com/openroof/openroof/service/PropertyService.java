package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.UserRole;
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
import java.util.Comparator;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import com.openroof.openroof.model.preference.UserPreference;

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
    private final UserPreferenceRepository userPreferenceRepository;
    private final PropertyRelevanceService propertyRelevanceService;
    private static final double EARTH_RADIUS = 6371;

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
    public Page<PropertySummaryResponse> getAll(PropertyFilterRequest filter, Pageable pageable, Long userId) {
        Specification<Property> spec = PropertySpecification.buildFilter(filter);
        
        // 1. Obtener todas las propiedades que coincidan con los filtros
        List<Property> properties = propertyRepository.findAll(spec);

        // 2. Ordenar por relevancia si el usuario tiene preferencias
        if (userId != null) {
            Optional<UserPreference> prefOpt = userPreferenceRepository.findByUserId(userId);
            if (prefOpt.isPresent()) {
                UserPreference pref = prefOpt.get();
                properties = properties.stream()
                        .sorted(Comparator.comparingInt(
                                (Property p) -> propertyRelevanceService.calculateScore(p, pref)
                        ).reversed())
                        .collect(Collectors.toList());

                // 3. Paginación manual sobre lista ordenada
                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), properties.size());
                List<Property> pageContent = start >= properties.size()
                        ? Collections.emptyList()
                        : properties.subList(start, end);

                // 4. Mapear a DTO con relevanceScore incluido
                List<PropertySummaryResponse> dtos = pageContent.stream()
                        .map(p -> propertyMapper.toSummaryResponse(p, propertyRelevanceService.calculateScore(p, pref)))
                        .collect(Collectors.toList());

                return new PageImpl<>(dtos, pageable, properties.size());
            }
        }

        // Si no hay usuario o no tiene preferencias, paginación normal en base de datos
        return propertyRepository.findAll(spec, sanitizePageable(pageable))
                .map(p -> propertyMapper.toSummaryResponse(p, 0));
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getByOwner(Long ownerId, Pageable pageable) {
        return propertyRepository.findByOwner_IdAndTrashedAtIsNull(ownerId, pageable)
                .map(p -> propertyMapper.toSummaryResponse(p, 0));
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> search(String keyword, Pageable pageable, Long userId) {
        List<Property> properties;
        if (keyword == null || keyword.isBlank()) {
            properties = propertyRepository.findAll();
        } else {
            properties = propertyRepository.searchByKeyword(keyword.trim(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        }

        if (userId != null) {
            Optional<UserPreference> prefOpt = userPreferenceRepository.findByUserId(userId);
            if (prefOpt.isPresent()) {
                UserPreference pref = prefOpt.get();
                properties = properties.stream()
                        .sorted(Comparator.comparingInt(
                                (Property p) -> propertyRelevanceService.calculateScore(p, pref)
                        ).reversed())
                        .collect(Collectors.toList());

                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), properties.size());
                List<Property> pageContent = start >= properties.size()
                        ? Collections.emptyList()
                        : properties.subList(start, end);

                List<PropertySummaryResponse> dtos = pageContent.stream()
                        .map(p -> propertyMapper.toSummaryResponse(p, propertyRelevanceService.calculateScore(p, pref)))
                        .collect(Collectors.toList());

                return new PageImpl<>(dtos, pageable, properties.size());
            }
        }

        if (keyword == null || keyword.isBlank()) {
            return propertyRepository.findAll(sanitizePageable(pageable))
                    .map(p -> propertyMapper.toSummaryResponse(p, 0));
        }
        return propertyRepository.searchByKeyword(keyword.trim(), sanitizePageable(pageable))
                .map(p -> propertyMapper.toSummaryResponse(p, 0));
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    public PropertyResponse update(Long id, UpdatePropertyRequest request, Long callerId, UserRole callerRole) {
        checkOwnership(id, callerId, callerRole);
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

    public void delete(Long id, Long callerId, UserRole callerRole) {
        checkOwnership(id, callerId, callerRole);
        Property property = findPropertyOrThrow(id);
        property.softDelete();
        propertyRepository.save(property);
    }

    public PropertyResponse trash(Long id, Long callerId, UserRole callerRole) {
        checkOwnership(id, callerId, callerRole);
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

    public PropertyResponse restoreFromTrashcan(Long id, Long callerId, UserRole callerRole) {
        checkOwnership(id, callerId, callerRole);
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
    public int clearTrashcanForUser(Long ownerId, Long callerId, UserRole callerRole) {
        // ADMIN puede vaciar la papelera de cualquier usuario.
        // USER solo puede vaciar la suya propia.
        if (callerRole != UserRole.ADMIN && !callerId.equals(ownerId)) {
            throw new ForbiddenException("Solo puedes vaciar tu propia papelera");
        }
        return propertyRepository.clearTrashcanByOwner(ownerId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getTrashcan(Long ownerId, Pageable pageable) {
        return propertyRepository.findByOwnerIdAndTrashedAtIsNotNull(ownerId, pageable)
                .map(propertyMapper::toSummaryResponse);
    }

    // ─── CHANGE STATUS ────────────────────────────────────────────

    public PropertyResponse changeStatus(Long id, PropertyStatus newStatus, UserRole callerRole) {
        if (callerRole != UserRole.ADMIN) {
            throw new ForbiddenException("Solo el administrador puede cambiar el estado de una propiedad");
        }
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

    public List<PropertySummaryResponse> findSimilarProperties(Long propertyId, int limit) {
        if (limit <= 0 || limit > 20) {
            throw new BadRequestException("El limite debe estar entre 1 y 20");
        }

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propiedad no encontrada con ID: " + propertyId));

        BigDecimal basePrice = property.getPrice();
        if (basePrice == null || basePrice.signum() <= 0) {
            log.warn("Propiedad {} sin precio válido, no se pueden buscar similares", propertyId);
            return List.of();
        }
        BigDecimal minPrice = basePrice.multiply(BigDecimal.valueOf(1 - PRICE_VARIATION));
        BigDecimal maxPrice = basePrice.multiply(BigDecimal.valueOf(1 + PRICE_VARIATION));
        String propertyType = property.getPropertyType().name();

        Set<Long> seenIds = new HashSet<>();
        seenIds.add(propertyId);
        List<Property> candidates = new ArrayList<>();

        // 1. search by coordinates
        if (property.hasCoordinates()) {
            BoundingBox bbox = calculateBoundingBox(
                    property.getLat(), property.getLng(), MAX_DISTANCE_KM);

            List<Property> nearby = propertyRepository.findNearbyProperties(
                    propertyId,
                    property.getLat(), property.getLng(),
                    bbox.getMinLat(), bbox.getMaxLat(),
                    bbox.getMinLng(), bbox.getMaxLng(),
                    propertyType,
                    minPrice, maxPrice, basePrice,
                    MAX_DISTANCE_KM,
                    limit * 3
            );

            nearby.stream()
                    .filter(p -> seenIds.add(p.getId()))
                    .forEach(candidates::add);

            log.debug("Nearby search encontró {} candidatos dentro de {}km",
                    candidates.size(), MAX_DISTANCE_KM);
        }

        // 2. fallback: same city
        if (candidates.size() < limit && property.getLocation() != null) {
            int needed = (limit - candidates.size()) * 2;

            propertyRepository.findByCity(
                            propertyId,
                            property.getLocation().getCity(),
                            propertyType,
                            minPrice, maxPrice, basePrice,
                            needed)
                    .stream()
                    .filter(p -> seenIds.add(p.getId()))
                    .forEach(candidates::add);

            log.debug("Fallback ciudad: {} candidatos acumulados", candidates.size());
        }

        // 3. last resort: property of same type
        if (candidates.size() < limit) {
            int needed = (limit - candidates.size()) * 2;
            BigDecimal wideMin = basePrice.multiply(BigDecimal.valueOf(1 - PRICE_VARIATION * 2));
            BigDecimal wideMax = basePrice.multiply(BigDecimal.valueOf(1 + PRICE_VARIATION * 2));

            propertyRepository.findByPropertyTypeOnly(
                            propertyId,
                            propertyType,
                            wideMin, wideMax, basePrice,
                            needed)
                    .stream()
                    .filter(p -> seenIds.add(p.getId()))
                    .forEach(candidates::add);

            log.debug("Fallback tipo: {} candidatos acumulados", candidates.size());
        }

        return rankAndLimit(candidates, property, limit).stream()
                .map(propertyMapper::toSummaryResponse)
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
        // check if properties have location or coordinates
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
        double baseBathrooms = Objects.requireNonNullElse(base.getBathrooms(), BigDecimal.ZERO).doubleValue();
        double candidateBathrooms = Objects.requireNonNullElse(candidate.getBathrooms(), BigDecimal.ZERO).doubleValue();
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

    /**
     * Verifica que el llamante tenga permiso para operar sobre la propiedad dada.
     * - ADMIN: acceso irrestricto.
     * - USER / AGENT: solo si es el propietario (owner) de la propiedad.
     *
     * @throws ResourceNotFoundException si la propiedad no existe.
     * @throws ForbiddenException        si el llamante no es el propietario.
     */
    private void checkOwnership(Long propertyId, Long callerId, UserRole callerRole) {
        if (callerRole == UserRole.ADMIN) return;   // ADMIN siempre puede
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propiedad no encontrada con ID: " + propertyId));
        if (!property.getOwner().getId().equals(callerId)) {
            throw new ForbiddenException("No tienes permiso para modificar esta propiedad");
        }
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

