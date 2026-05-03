package com.openroof.openroof.dto.property;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PropertySummaryResponse(
        Long id,
        String title,
        BigDecimal price,
        String propertyType,
        String category,
        String address,
        String primaryImageUrl,
        Integer bedrooms,
        BigDecimal bathrooms,
        BigDecimal surfaceArea,
        String status,
        String locationName,
        BigDecimal lat,
        BigDecimal lng,
        LocalDateTime trashedAt,
        Integer relevanceScore,
        Boolean highlighted,
        LocalDateTime highlightedUntil
        ) {
}
