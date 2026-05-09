package com.openroof.openroof.dto.property;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PropertyResponse(
        Long id,
        String title,
        String description,
        String propertyType,
        String category,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        BigDecimal price,
        String listingType,
        BigDecimal rentAmount,
        String rentCurrency,
        String rentFrequency,
        String rentBillingCycle,
        Integer bedrooms,
        BigDecimal bathrooms,
        Integer halfBathrooms,
        Integer fullBathrooms,
        BigDecimal surfaceArea,
        BigDecimal builtArea,
        Integer parkingSpaces,
        Integer floorsCount,

        // Construcción
        Integer constructionYear,
        String constructionStatus,
        String structureMaterial,
        String wallsMaterial,
        String floorMaterial,
        String roofMaterial,

        // Servicios
        String waterConnection,
        String sanitaryInstallation,
        String electricityInstallation,

        // Estado
        String status,
        String visibility,
        String availability,
        Boolean highlighted,
        LocalDateTime highlightedUntil,
        Integer viewCount,
        Integer favoriteCount,

        // Relaciones
        Long ownerId,
        String ownerName,
        Long agentId,
        Long locationId,
        String locationName,

        // Colecciones
        List<PropertyRoomDto> rooms,
        List<PropertyMediaDto> media,
        List<Long> exteriorFeatureIds,

        // Audit
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime trashedAt,
        LocalDateTime publishedAt) {
}
