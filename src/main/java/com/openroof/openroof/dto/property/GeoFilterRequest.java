package com.openroof.openroof.dto.property;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Parámetros de filtro geoespacial opcionales para los endpoints
 * {@code GET /properties} y {@code GET /properties/search}.
 *
 * <p>Admite dos modos mutuamente excluyentes:
 * <ul>
 *   <li><b>Polígono</b>: lista de vértices codificada en JSON
 *       {@code [[lat1,lng1],[lat2,lng2],...]}</li>
 *   <li><b>Círculo</b>: centro (circleLat, circleLng) + radio en metros</li>
 * </ul>
 */
public record GeoFilterRequest(

        @Schema(description = "Vértices del polígono serializado como JSON: [[lat1,lng1],[lat2,lng2],...]")
        String polygon,

        @Schema(description = "Latitud del centro del círculo")
        Double circleLat,

        @Schema(description = "Longitud del centro del círculo")
        Double circleLng,

        @Schema(description = "Radio del círculo en metros (> 0)")
        Double circleRadiusMeters) {

    public boolean hasPolygon() {
        return polygon != null && !polygon.isBlank();
    }

    public boolean hasCircle() {
        return circleLat != null && circleLng != null
                && circleRadiusMeters != null && circleRadiusMeters > 0;
    }

    public boolean isEmpty() {
        return !hasPolygon() && !hasCircle();
    }
}
