package com.openroof.openroof.dto.property;

import java.math.BigDecimal;

/**
 * DTO para los filtros avanzados del endpoint GET /properties.
 * Todos los campos son opcionales; los nulos simplemente no se aplican.
 */
public record PropertyFilterRequest(

        /**
         * Disponibilidad de la propiedad (enum Availability: IMMEDIATE, IN_30_DAYS,
         * IN_60_DAYS, TO_NEGOTIATE)
         */
        String availability,

        /**
         * Tipo de la propiedad (enum PropertyType: HOUSE, APARTMENT, LAND, OFFICE,
         * WAREHOUSE, FARM)
         */
        String propertyType,

        /** Estado de la propiedad (enum PropertyStatus) */
        String status,

        /** Precio mínimo (inclusive) */
        BigDecimal minPrice,

        /** Precio máximo (inclusive) */
        BigDecimal maxPrice,

        /** ID de la ubicación/zona */
        Long locationId,

        /** Cantidad mínima de baños */
        BigDecimal minBathrooms,

        /** Cantidad mínima de dormitorios */
        Integer minBedrooms) {
}
