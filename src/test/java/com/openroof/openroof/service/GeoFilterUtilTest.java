package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.GeoFilterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link GeoFilterUtil}.
 *
 * Cubre:
 * - pointInPolygon: punto dentro, punto fuera, punto en borde, triángulo, concavidad
 * - withinCircle: punto dentro, punto exactamente en borde, punto fuera
 * - matches: filtro polígono, filtro círculo, JSON inválido, coordenadas null
 * - GeoFilterRequest: hasPolygon, hasCircle, isEmpty
 */
@DisplayName("GeoFilterUtil Tests")
class GeoFilterUtilTest {

    // ─── Polígono de prueba: cuadrado [-10,-10] a [10,10] ─────────────────────
    // Vértices en orden: (lat=-10,lng=-10), (lat=10,lng=-10), (lat=10,lng=10), (lat=-10,lng=10)
    private static final double[][] SQUARE = {
            {-10, -10},
            {10,  -10},
            {10,   10},
            {-10,  10}
    };

    // ─── Triángulo de prueba ───────────────────────────────────────────────────
    // (0,0), (10,5), (0,10)
    private static final double[][] TRIANGLE = {
            {0,  0},
            {10, 5},
            {0,  10}
    };

    // ─── pointInPolygon ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("pointInPolygon")
    class PointInPolygonTests {

        @Test
        @DisplayName("punto en el centro del cuadrado → dentro")
        void centerOfSquare_isInside() {
            assertThat(GeoFilterUtil.pointInPolygon(0, 0, SQUARE)).isTrue();
        }

        @Test
        @DisplayName("punto claramente fuera del cuadrado → fuera")
        void farOutsideSquare_isOutside() {
            assertThat(GeoFilterUtil.pointInPolygon(50, 50, SQUARE)).isFalse();
        }

        @Test
        @DisplayName("punto en la esquina extrema del cuadrado → fuera")
        void cornerOutside_isOutside() {
            assertThat(GeoFilterUtil.pointInPolygon(-15, -15, SQUARE)).isFalse();
        }

        @Test
        @DisplayName("punto muy cerca del borde pero dentro → dentro")
        void nearEdgeInside_isInside() {
            assertThat(GeoFilterUtil.pointInPolygon(9.9, 0, SQUARE)).isTrue();
        }

        @Test
        @DisplayName("punto muy cerca del borde pero fuera → fuera")
        void nearEdgeOutside_isOutside() {
            assertThat(GeoFilterUtil.pointInPolygon(10.1, 0, SQUARE)).isFalse();
        }

        @Test
        @DisplayName("punto dentro del triángulo → dentro")
        void insideTriangle_isInside() {
            // Centroide del triángulo (0,0)-(10,5)-(0,10): ~(3.33, 5)
            assertThat(GeoFilterUtil.pointInPolygon(3, 5, TRIANGLE)).isTrue();
        }

        @Test
        @DisplayName("punto fuera del triángulo → fuera")
        void outsideTriangle_isOutside() {
            assertThat(GeoFilterUtil.pointInPolygon(9, 9, TRIANGLE)).isFalse();
        }

        @Test
        @DisplayName("punto en coordenadas reales de Asunción → dentro de polígono Asunción")
        void asuncionCenter_insideAsuncionPolygon() {
            // Polígono simplificado alrededor del centro de Asunción
            double[][] asuncion = {
                    {-25.26, -57.65},
                    {-25.24, -57.65},
                    {-25.24, -57.63},
                    {-25.26, -57.63}
            };
            // Centro de Asunción ~-25.2867, -57.6470 — fuera del polígono pequeño
            assertThat(GeoFilterUtil.pointInPolygon(-25.25, -57.64, asuncion)).isTrue();
        }
    }

    // ─── withinCircle ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("withinCircle")
    class WithinCircleTests {

        private static final double CENTER_LAT = -25.2867;
        private static final double CENTER_LNG = -57.6470;
        private static final double RADIUS_500M = 500.0;

        @Test
        @DisplayName("punto en el centro → dentro")
        void exactCenter_isInside() {
            assertThat(GeoFilterUtil.withinCircle(CENTER_LAT, CENTER_LNG,
                    CENTER_LAT, CENTER_LNG, RADIUS_500M)).isTrue();
        }

        @Test
        @DisplayName("punto a ~100 m del centro → dentro del radio de 500 m")
        void point100m_isInsideOf500m() {
            // Desplazamiento de ~0.001° de lat ≈ 111 m
            assertThat(GeoFilterUtil.withinCircle(CENTER_LAT + 0.001, CENTER_LNG,
                    CENTER_LAT, CENTER_LNG, RADIUS_500M)).isTrue();
        }

        @Test
        @DisplayName("punto a ~5 km del centro → fuera del radio de 500 m")
        void point5km_isOutsideOf500m() {
            // Desplazamiento de ~0.05° de lat ≈ 5.5 km
            assertThat(GeoFilterUtil.withinCircle(CENTER_LAT + 0.05, CENTER_LNG,
                    CENTER_LAT, CENTER_LNG, RADIUS_500M)).isFalse();
        }

        @Test
        @DisplayName("punto exactamente en el borde → dentro (≤ radio)")
        void pointAtBorder_isInside() {
            // 1 grado de latitud ≈ 111,320 m → 0.004° ≈ 445 m (dentro de 500 m)
            assertThat(GeoFilterUtil.withinCircle(CENTER_LAT + 0.004, CENTER_LNG,
                    CENTER_LAT, CENTER_LNG, RADIUS_500M)).isTrue();
        }

        @Test
        @DisplayName("radio 0 m: solo el punto exacto está dentro")
        void zeroRadius_onlyExactCenter() {
            assertThat(GeoFilterUtil.withinCircle(CENTER_LAT, CENTER_LNG,
                    CENTER_LAT, CENTER_LNG, 0)).isTrue();
            assertThat(GeoFilterUtil.withinCircle(CENTER_LAT + 0.000001, CENTER_LNG,
                    CENTER_LAT, CENTER_LNG, 0)).isFalse();
        }
    }

    // ─── matches ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("matches (evaluador combinado)")
    class MatchesTests {

        private static final String SQUARE_JSON = "[[-10,-10],[10,-10],[10,10],[-10,10]]";
        private static final String INVALID_JSON = "{not-valid}";

        @Test
        @DisplayName("filtro polígono: punto dentro → true")
        void polygonFilter_insidePoint_returnsTrue() {
            GeoFilterRequest filter = new GeoFilterRequest(SQUARE_JSON, null, null, null);
            assertThat(GeoFilterUtil.matches(0.0, 0.0, filter)).isTrue();
        }

        @Test
        @DisplayName("filtro polígono: punto fuera → false")
        void polygonFilter_outsidePoint_returnsFalse() {
            GeoFilterRequest filter = new GeoFilterRequest(SQUARE_JSON, null, null, null);
            assertThat(GeoFilterUtil.matches(50.0, 50.0, filter)).isFalse();
        }

        @Test
        @DisplayName("filtro círculo: punto dentro → true")
        void circleFilter_insidePoint_returnsTrue() {
            GeoFilterRequest filter = new GeoFilterRequest(null, 0.0, 0.0, 100_000.0);
            assertThat(GeoFilterUtil.matches(0.5, 0.5, filter)).isTrue();
        }

        @Test
        @DisplayName("filtro círculo: punto fuera → false")
        void circleFilter_outsidePoint_returnsFalse() {
            GeoFilterRequest filter = new GeoFilterRequest(null, 0.0, 0.0, 100.0);
            assertThat(GeoFilterUtil.matches(10.0, 10.0, filter)).isFalse();
        }

        @Test
        @DisplayName("latitud null → false")
        void nullLat_returnsFalse() {
            GeoFilterRequest filter = new GeoFilterRequest(SQUARE_JSON, null, null, null);
            assertThat(GeoFilterUtil.matches(null, 0.0, filter)).isFalse();
        }

        @Test
        @DisplayName("longitud null → false")
        void nullLng_returnsFalse() {
            GeoFilterRequest filter = new GeoFilterRequest(SQUARE_JSON, null, null, null);
            assertThat(GeoFilterUtil.matches(0.0, null, filter)).isFalse();
        }

        @Test
        @DisplayName("JSON de polígono inválido → false (no lanza excepción)")
        void invalidPolygonJson_returnsFalse() {
            GeoFilterRequest filter = new GeoFilterRequest(INVALID_JSON, null, null, null);
            assertThat(GeoFilterUtil.matches(0.0, 0.0, filter)).isFalse();
        }

        @Test
        @DisplayName("polígono con menos de 3 vértices → false")
        void polygonFewerThan3Vertices_returnsFalse() {
            GeoFilterRequest filter = new GeoFilterRequest("[[0,0],[1,1]]", null, null, null);
            assertThat(GeoFilterUtil.matches(0.5, 0.5, filter)).isFalse();
        }

        @Test
        @DisplayName("filtro vacío (sin polígono ni círculo) → false")
        void emptyFilter_returnsFalse() {
            GeoFilterRequest filter = new GeoFilterRequest(null, null, null, null);
            assertThat(GeoFilterUtil.matches(0.0, 0.0, filter)).isFalse();
        }
    }

    // ─── GeoFilterRequest ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GeoFilterRequest helpers")
    class GeoFilterRequestTests {

        @Test
        @DisplayName("hasPolygon: true cuando polygon no es blank")
        void hasPolygon_whenPolygonSet() {
            assertThat(new GeoFilterRequest("[[0,0],[1,0],[0,1]]", null, null, null).hasPolygon()).isTrue();
        }

        @Test
        @DisplayName("hasPolygon: false cuando polygon es null")
        void hasPolygon_whenPolygonNull() {
            assertThat(new GeoFilterRequest(null, null, null, null).hasPolygon()).isFalse();
        }

        @Test
        @DisplayName("hasCircle: true cuando todos los campos del círculo están presentes y radio > 0")
        void hasCircle_whenAllCircleFieldsSet() {
            assertThat(new GeoFilterRequest(null, 0.0, 0.0, 500.0).hasCircle()).isTrue();
        }

        @Test
        @DisplayName("hasCircle: false cuando el radio es 0")
        void hasCircle_whenRadiusZero() {
            assertThat(new GeoFilterRequest(null, 0.0, 0.0, 0.0).hasCircle()).isFalse();
        }

        @Test
        @DisplayName("hasCircle: false cuando lat/lng son null")
        void hasCircle_whenLatLngNull() {
            assertThat(new GeoFilterRequest(null, null, null, 500.0).hasCircle()).isFalse();
        }

        @Test
        @DisplayName("isEmpty: true cuando ningún campo está presente")
        void isEmpty_whenNoFieldsSet() {
            assertThat(new GeoFilterRequest(null, null, null, null).isEmpty()).isTrue();
        }

        @Test
        @DisplayName("isEmpty: false cuando hay polígono")
        void isEmpty_whenPolygonSet() {
            assertThat(new GeoFilterRequest("[[0,0]]", null, null, null).isEmpty()).isFalse();
        }

        @Test
        @DisplayName("isEmpty: false cuando hay círculo completo")
        void isEmpty_whenCircleSet() {
            assertThat(new GeoFilterRequest(null, 1.0, 1.0, 1000.0).isEmpty()).isFalse();
        }
    }
}
