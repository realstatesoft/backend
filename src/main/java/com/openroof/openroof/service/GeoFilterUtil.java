package com.openroof.openroof.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.dto.property.GeoFilterRequest;
import com.openroof.openroof.model.property.Property;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Utilidades estáticas para filtrado geoespacial.
 *
 * <ul>
 *   <li>{@link #pointInPolygon} — algoritmo de ray-casting (Jordan curve test)</li>
 *   <li>{@link #withinCircle} — distancia Haversine</li>
 *   <li>{@link #matches} — evaluador combinado sobre un {@link GeoFilterRequest}</li>
 *   <li>{@link #buildBoundingBoxSpec} — {@link Specification} JPA de bounding-box para
 *       pre-filtrar en base de datos antes de aplicar la geometría exacta</li>
 * </ul>
 */
@Slf4j
public final class GeoFilterUtil {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /** Shared Jackson mapper — ObjectMapper es thread-safe. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GeoFilterUtil() {
    }

    // ─── Algoritmos geoespaciales ─────────────────────────────────────────────

    /**
     * Ray-casting algorithm (Jordan curve test) para punto dentro de polígono.
     *
     * <p>Los vértices se reciben como array {@code double[2]}: índice 0 = latitud,
     * índice 1 = longitud. El polígono no necesita repetir el primer vértice al final.
     *
     * @param lat     latitud del punto a comprobar
     * @param lng     longitud del punto a comprobar
     * @param polygon vértices del polígono ({@code [[lat0,lng0],[lat1,lng1],...]}),
     *                mínimo 3 vértices
     * @return {@code true} si el punto está dentro del polígono
     */
    public static boolean pointInPolygon(double lat, double lng, double[][] polygon) {
        int n = polygon.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double latI = polygon[i][0];
            double lngI = polygon[i][1];
            double latJ = polygon[j][0];
            double lngJ = polygon[j][1];
            // El eje del rayo es +lng (hacia el este).
            // El arco estraddle la línea lat=lat_p y la intersección lng > lng_p.
            if ((latI > lat) != (latJ > lat)) {
                double lngCrossing = lngI + (lat - latI) * (lngJ - lngI) / (latJ - latI);
                if (lng < lngCrossing) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    /**
     * Comprueba si un punto cae dentro de un círculo usando la fórmula Haversine.
     *
     * @param lat           latitud del punto
     * @param lng           longitud del punto
     * @param centerLat     latitud del centro del círculo
     * @param centerLng     longitud del centro del círculo
     * @param radiusMeters  radio del círculo en metros
     * @return {@code true} si la distancia al centro ≤ radio
     */
    public static boolean withinCircle(double lat, double lng,
                                        double centerLat, double centerLng,
                                        double radiusMeters) {
        double dLat = Math.toRadians(lat - centerLat);
        double dLng = Math.toRadians(lng - centerLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(centerLat)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double distanceMeters = EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return distanceMeters <= radiusMeters;
    }

    // ─── Evaluador combinado ──────────────────────────────────────────────────

    /**
     * Evalúa si un punto (lat, lng) satisface el {@link GeoFilterRequest}.
     *
     * <p>Retorna {@code false} si el punto no tiene coordenadas, si el JSON del
     * polígono es inválido o si el filtro está vacío.
     *
     * @param propLat coordenada lat de la propiedad (puede ser null)
     * @param propLng coordenada lng de la propiedad (puede ser null)
     * @param filter  filtro geoespacial activo
     * @return {@code true} si el punto está dentro del área
     */
    public static boolean matches(Double propLat, Double propLng, GeoFilterRequest filter) {
        if (propLat == null || propLng == null) {
            return false;
        }
        if (filter.hasPolygon()) {
            try {
                double[][] polygon = MAPPER.readValue(filter.polygon(), double[][].class);
                if (polygon.length < 3) {
                    log.warn("GeoFilter polygon has fewer than 3 vertices, ignoring");
                    return false;
                }
                return pointInPolygon(propLat, propLng, polygon);
            } catch (Exception e) {
                log.warn("Invalid polygon JSON in geo filter: {}", e.getMessage());
                return false;
            }
        }
        if (filter.hasCircle()) {
            return withinCircle(propLat, propLng,
                    filter.circleLat(), filter.circleLng(), filter.circleRadiusMeters());
        }
        return false;
    }

    // ─── Bounding box para pre-filtro en BD ──────────────────────────────────

    /**
     * Construye un {@link Specification} JPA que filtra por bounding-box.
     *
     * <p>El bounding-box es ligeramente más grande que el área exacta, por lo que
     * siempre incluye todos los candidatos reales. Tras aplicar este filtro en la
     * consulta SQL, el servicio aplica el check de geometría exacta en memoria.
     *
     * @param filter filtro geoespacial con polígono o círculo
     * @return especificación de bounding-box, o {@code null} si el filtro está vacío
     */
    public static Specification<Property> buildBoundingBoxSpec(GeoFilterRequest filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }

        double minLat, maxLat, minLng, maxLng;

        if (filter.hasPolygon()) {
            double[][] polygon;
            try {
                polygon = MAPPER.readValue(filter.polygon(), double[][].class);
            } catch (Exception e) {
                log.warn("Cannot build bounding box from invalid polygon JSON: {}", e.getMessage());
                return null;
            }
            minLat = Arrays.stream(polygon).mapToDouble(p -> p[0]).min().orElse(-90);
            maxLat = Arrays.stream(polygon).mapToDouble(p -> p[0]).max().orElse(90);
            minLng = Arrays.stream(polygon).mapToDouble(p -> p[1]).min().orElse(-180);
            maxLng = Arrays.stream(polygon).mapToDouble(p -> p[1]).max().orElse(180);
        } else {
            // Círculo: bounding box a partir de delta-grados
            double r = filter.circleRadiusMeters();
            double deltaLat = Math.toDegrees(r / EARTH_RADIUS_METERS);
            double deltaLng = Math.toDegrees(r / (EARTH_RADIUS_METERS
                    * Math.cos(Math.toRadians(filter.circleLat()))));
            minLat = filter.circleLat() - deltaLat;
            maxLat = filter.circleLat() + deltaLat;
            minLng = filter.circleLng() - deltaLng;
            maxLng = filter.circleLng() + deltaLng;
        }

        final BigDecimal fMinLat = BigDecimal.valueOf(minLat);
        final BigDecimal fMaxLat = BigDecimal.valueOf(maxLat);
        final BigDecimal fMinLng = BigDecimal.valueOf(minLng);
        final BigDecimal fMaxLng = BigDecimal.valueOf(maxLng);

        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("geoLocation").get("lat")),
                cb.isNotNull(root.get("geoLocation").get("lng")),
                cb.greaterThanOrEqualTo(root.get("geoLocation").get("lat"), fMinLat),
                cb.lessThanOrEqualTo(root.get("geoLocation").get("lat"), fMaxLat),
                cb.greaterThanOrEqualTo(root.get("geoLocation").get("lng"), fMinLng),
                cb.lessThanOrEqualTo(root.get("geoLocation").get("lng"), fMaxLng));
    }
}
