package com.openroof.openroof.common.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

/**
 * Value object para coordenadas geográficas (latitud/longitud).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoLocation {

    @Column(precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(precision = 11, scale = 8)
    private BigDecimal lng;

    // getters
    public Double getLatAsDouble() {
        return lat != null ? lat.doubleValue() : null;
    }

    public Double getLngAsDouble() {
        return lng != null ? lng.doubleValue() : null;
    }

    // convenience methods
    public boolean isValid() {
        return lat != null && lng != null
                && lat.compareTo(new BigDecimal("-90")) >= 0
                && lat.compareTo(new BigDecimal("90")) <= 0
                && lng.compareTo(new BigDecimal("-180")) >= 0
                && lng.compareTo(new BigDecimal("180")) <= 0;
    }

    public boolean hasCoordinates() {
        return lat != null && lng != null;
    }

    //
    @Override
    public String toString() {
        return String.format("GeoLocation{lat=%s, lng=%s}", lat, lng);
    }
}
