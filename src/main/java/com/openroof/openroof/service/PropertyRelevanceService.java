package com.openroof.openroof.service;

import com.openroof.openroof.model.preference.PreferenceOption;
import com.openroof.openroof.model.preference.UserPreference;
import com.openroof.openroof.model.preference.UserPreferenceRange;
import com.openroof.openroof.model.property.Property;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class PropertyRelevanceService {

    public int calculateScore(Property property, UserPreference userPreference) {
        int score = 0;

        Set<PreferenceOption> opciones = userPreference.getSelectedOptions();

        // Criterio 1 — Tipo de propiedad (peso: 30 puntos)
        boolean tienePreferenciaTipo = opciones.stream()
                .anyMatch(o -> "PROPERTY_TYPE".equals(o.getCategory().getCode()));

        if (tienePreferenciaTipo) {
            boolean coincide = opciones.stream()
                    .filter(o -> "PROPERTY_TYPE".equals(o.getCategory().getCode()))
                    .anyMatch(o -> o.getValue().equalsIgnoreCase(property.getPropertyType().name()));
            if (coincide) score += 30;
        }

        // Criterio 2 — Zona / Ciudad (peso: 25 puntos)
        boolean tienePreferenciaZona = opciones.stream()
                .anyMatch(o -> "ZONE".equals(o.getCategory().getCode()));

        if (tienePreferenciaZona && property.getAddress() != null) {
            String direccion = property.getAddress().toLowerCase();
            boolean coincide = opciones.stream()
                    .filter(o -> "ZONE".equals(o.getCategory().getCode()))
                    .anyMatch(o -> direccion.contains(o.getLabel().toLowerCase()));
            if (coincide) score += 25;
        }

        // Criterio 3 — Rango de precio (peso: 20 puntos)
        Optional<UserPreferenceRange> precioRange = userPreference.getRanges().stream()
                .filter(r -> "PRICE".equals(r.getFieldName())).findFirst();
        if (precioRange.isPresent() && property.getPrice() != null) {
            double precio = property.getPrice().doubleValue();
            Double min = precioRange.get().getMinValue();
            Double max = precioRange.get().getMaxValue();
            if ((min == null || precio >= min) && (max == null || precio <= max)) score += 20;
        }

        // Criterio 4 — Dormitorios (peso: 15 puntos)
        Optional<UserPreferenceRange> dormRange = userPreference.getRanges().stream()
                .filter(r -> "BEDROOMS".equals(r.getFieldName())).findFirst();
        if (dormRange.isPresent() && property.getBedrooms() != null) {
            double dorm = property.getBedrooms().doubleValue();
            Double min = dormRange.get().getMinValue();
            Double max = dormRange.get().getMaxValue();
            if ((min == null || dorm >= min) && (max == null || dorm <= max)) score += 15;
        }

        // Criterio 5 — Características del exterior (peso: 5 pts por match, máx 10)
        long exteriorMatches = opciones.stream()
                .filter(o -> "EXTERIOR_FEATURE".equals(o.getCategory().getCode()))
                .filter(o -> property.getExteriorFeatures() != null
                        && property.getExteriorFeatures()
                                .stream()
                                .anyMatch(f -> f.getName().equalsIgnoreCase(o.getLabel())))
                .count();
        score += (int) Math.min(exteriorMatches * 5, 10);

        return score;
    }
}
