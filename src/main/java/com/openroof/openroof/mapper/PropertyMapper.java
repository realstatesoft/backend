package com.openroof.openroof.mapper;

import com.openroof.openroof.common.embeddable.ConstructionDetails;
import com.openroof.openroof.common.embeddable.GeoLocation;
import com.openroof.openroof.common.embeddable.UtilityInfo;
import com.openroof.openroof.dto.property.*;
import com.openroof.openroof.model.property.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Mapper manual para conversiones entre Property (entidad) y DTOs.
 */
@Component
public class PropertyMapper {

    // ─── Entity → Response ────────────────────────────────────────

    public PropertyResponse toResponse(Property p) {
        ConstructionDetails c = p.getConstruction();
        UtilityInfo u = p.getUtilities();
        GeoLocation g = p.getGeoLocation();

        return new PropertyResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                enumName(p.getPropertyType()),
                enumName(p.getCategory()),
                p.getAddress(),
                g != null ? g.getLat() : null,
                g != null ? g.getLng() : null,
                p.getPrice(),
                p.getBedrooms(),
                p.getBathrooms(),
                p.getHalfBathrooms(),
                p.getFullBathrooms(),
                p.getSurfaceArea(),
                p.getBuiltArea(),
                p.getParkingSpaces(),
                p.getFloorsCount(),
                // Construcción
                c != null ? c.getYear() : null,
                c != null ? enumName(c.getStatus()) : null,
                c != null ? c.getStructureMaterial() : null,
                c != null ? c.getWallsMaterial() : null,
                c != null ? c.getFloorMaterial() : null,
                c != null ? c.getRoofMaterial() : null,
                // Servicios
                u != null ? u.getWaterConnection() : null,
                u != null ? u.getSanitaryInstallation() : null,
                u != null ? u.getElectricityInstallation() : null,
                // Estado
                enumName(p.getStatus()),
                enumName(p.getVisibility()),
                enumName(p.getAvailability()),
                p.getHighlighted(),
                p.getHighlightedUntil(),
                p.getViewCount(),
                p.getFavoriteCount(),
                // Relaciones
                p.getOwner() != null ? p.getOwner().getId() : null,
                p.getOwner() != null ? p.getOwner().getName() : null,
                p.getAgent() != null ? p.getAgent().getId() : null,
                p.getLocation() != null ? p.getLocation().getId() : null,
                p.getLocation() != null ? p.getLocation().getName() : null,
                // Colecciones
                mapRooms(p.getRooms()),
                mapMedia(p.getMedia()),
                mapExteriorFeatureIds(p.getExteriorFeatures()),
                // Audit
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getTrashedAt(),
                p.getTrashedAt());
    }

    public PropertySummaryResponse toSummaryResponse(Property p) {
        return toSummaryResponse(p, 0);
    }

    public PropertySummaryResponse toSummaryResponse(Property p, Integer relevanceScore) {
        String primaryImage = Optional.ofNullable(p.getMedia())
                .orElse(Collections.emptyList())
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsPrimary()))
                .findFirst()
                .map(PropertyMedia::getUrl)
                .orElse(null);

        return new PropertySummaryResponse(
                p.getId(),
                p.getTitle(),
                p.getPrice(),
                enumName(p.getPropertyType()),
                enumName(p.getCategory()),
                p.getAddress(),
                primaryImage,
                p.getBedrooms(),
                p.getBathrooms(),
                p.getSurfaceArea(),
                enumName(p.getStatus()),
                p.getLocation() != null ? p.getLocation().getName() : null,
                p.getGeoLocation() != null ? p.getGeoLocation().getLat() : null,
                p.getGeoLocation() != null ? p.getGeoLocation().getLng() : null,
                p.getTrashedAt(),
                relevanceScore);
    }

    // ─── Request → Entity ─────────────────────────────────────────

    public Property toEntity(CreatePropertyRequest req) {
        Property.PropertyBuilder builder = Property.builder()
                .title(req.title())
                .description(req.description())
                .propertyType(req.propertyType())
                .category(req.category())
                .address(req.address())
                .price(req.price());

        // GeoLocation
        if (req.lat() != null && req.lng() != null) {
            builder.geoLocation(GeoLocation.builder()
                    .lat(req.lat())
                    .lng(req.lng())
                    .build());
        }

        // Campos opcionales con defaults
        if (req.bedrooms() != null)
            builder.bedrooms(req.bedrooms());
        if (req.bathrooms() != null)
            builder.bathrooms(req.bathrooms());
        if (req.halfBathrooms() != null)
            builder.halfBathrooms(req.halfBathrooms());
        if (req.fullBathrooms() != null)
            builder.fullBathrooms(req.fullBathrooms());
        if (req.surfaceArea() != null)
            builder.surfaceArea(req.surfaceArea());
        if (req.builtArea() != null)
            builder.builtArea(req.builtArea());
        if (req.parkingSpaces() != null)
            builder.parkingSpaces(req.parkingSpaces());
        if (req.floorsCount() != null)
            builder.floorsCount(req.floorsCount());
        if (req.availability() != null)
            builder.availability(req.availability());

        // ConstructionDetails
        if (hasConstructionData(req)) {
            builder.construction(ConstructionDetails.builder()
                    .year(req.constructionYear())
                    .status(req.constructionStatus())
                    .structureMaterial(req.structureMaterial())
                    .wallsMaterial(req.wallsMaterial())
                    .floorMaterial(req.floorMaterial())
                    .roofMaterial(req.roofMaterial())
                    .build());
        }

        // UtilityInfo
        if (hasUtilityData(req)) {
            builder.utilities(UtilityInfo.builder()
                    .waterConnection(req.waterConnection())
                    .sanitaryInstallation(req.sanitaryInstallation())
                    .electricityInstallation(req.electricityInstallation())
                    .build());
        }

        return builder.build();
    }

    // ─── Actualización parcial ────────────────────────────────────

    public void updateEntity(Property property, UpdatePropertyRequest req) {
        if (req.title() != null)
            property.setTitle(req.title());
        if (req.description() != null)
            property.setDescription(req.description());
        if (req.propertyType() != null)
            property.setPropertyType(req.propertyType());
        if (req.category() != null)
            property.setCategory(req.category());
        if (req.address() != null)
            property.setAddress(req.address());
        if (req.price() != null)
            property.setPrice(req.price());
        if (req.bedrooms() != null)
            property.setBedrooms(req.bedrooms());
        if (req.bathrooms() != null)
            property.setBathrooms(req.bathrooms());
        if (req.halfBathrooms() != null)
            property.setHalfBathrooms(req.halfBathrooms());
        if (req.fullBathrooms() != null)
            property.setFullBathrooms(req.fullBathrooms());
        if (req.surfaceArea() != null)
            property.setSurfaceArea(req.surfaceArea());
        if (req.builtArea() != null)
            property.setBuiltArea(req.builtArea());
        if (req.parkingSpaces() != null)
            property.setParkingSpaces(req.parkingSpaces());
        if (req.floorsCount() != null)
            property.setFloorsCount(req.floorsCount());
        if (req.availability() != null)
            property.setAvailability(req.availability());
        if (req.visibility() != null)
            property.setVisibility(req.visibility());

        // GeoLocation
        if (req.lat() != null && req.lng() != null) {
            property.setGeoLocation(GeoLocation.builder()
                    .lat(req.lat())
                    .lng(req.lng())
                    .build());
        } else if (req.lat() == null && req.lng() == null) {
            property.setGeoLocation(null);
        }

        // ConstructionDetails: actualizar campos individualmente
        if (hasAnyConstructionData(req)) {
            ConstructionDetails existing = property.getConstruction();
            if (existing == null) {
                existing = new ConstructionDetails();
            }
            if (req.constructionYear() != null)
                existing.setYear(req.constructionYear());
            if (req.constructionStatus() != null)
                existing.setStatus(req.constructionStatus());
            if (req.structureMaterial() != null)
                existing.setStructureMaterial(req.structureMaterial());
            if (req.wallsMaterial() != null)
                existing.setWallsMaterial(req.wallsMaterial());
            if (req.floorMaterial() != null)
                existing.setFloorMaterial(req.floorMaterial());
            if (req.roofMaterial() != null)
                existing.setRoofMaterial(req.roofMaterial());
            property.setConstruction(existing);
        }

        // UtilityInfo: actualizar campos individualmente
        if (hasAnyUtilityData(req)) {
            UtilityInfo existing = property.getUtilities();
            if (existing == null) {
                existing = new UtilityInfo();
            }
            if (req.waterConnection() != null)
                existing.setWaterConnection(req.waterConnection());
            if (req.sanitaryInstallation() != null)
                existing.setSanitaryInstallation(req.sanitaryInstallation());
            if (req.electricityInstallation() != null)
                existing.setElectricityInstallation(req.electricityInstallation());
            property.setUtilities(existing);
        }
    }

    // ─── Helpers privados ─────────────────────────────────────────

    private List<PropertyRoomDto> mapRooms(List<PropertyRoom> rooms) {
        if (rooms == null)
            return Collections.emptyList();
        return rooms.stream()
                .map(r -> new PropertyRoomDto(
                        r.getName(),
                        r.getArea(),
                        r.getFeatures() != null
                                ? r.getFeatures().stream().map(InteriorFeature::getId).toList()
                                : Collections.emptyList()))
                .toList();
    }

    private List<PropertyMediaDto> mapMedia(List<PropertyMedia> media) {
        if (media == null)
            return Collections.emptyList();
        return media.stream()
                .map(m -> new PropertyMediaDto(
                        m.getType(),
                        m.getUrl(),
                        m.getThumbnailUrl(),
                        m.getIsPrimary(),
                        m.getOrderIndex(),
                        m.getTitle()))
                .toList();
    }

    private List<Long> mapExteriorFeatureIds(List<ExteriorFeature> features) {
        if (features == null)
            return Collections.emptyList();
        return features.stream().map(ExteriorFeature::getId).toList();
    }

    private String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }

    private boolean hasConstructionData(CreatePropertyRequest req) {
        return req.constructionYear() != null || req.constructionStatus() != null
                || req.structureMaterial() != null || req.wallsMaterial() != null
                || req.floorMaterial() != null || req.roofMaterial() != null;
    }

    private boolean hasUtilityData(CreatePropertyRequest req) {
        return req.waterConnection() != null || req.sanitaryInstallation() != null
                || req.electricityInstallation() != null;
    }

    private boolean hasAnyConstructionData(UpdatePropertyRequest req) {
        return req.constructionYear() != null || req.constructionStatus() != null
                || req.structureMaterial() != null || req.wallsMaterial() != null
                || req.floorMaterial() != null || req.roofMaterial() != null;
    }

    private boolean hasAnyUtilityData(UpdatePropertyRequest req) {
        return req.waterConnection() != null || req.sanitaryInstallation() != null
                || req.electricityInstallation() != null;
    }
}
