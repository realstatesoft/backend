package com.openroof.openroof.controller.test;

import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.Visibility;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.search.SearchPreference;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.SearchPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AlertDiagnosticController {

    private final PropertyRepository propertyRepository;
    private final SearchPreferenceRepository searchPreferenceRepository;

    @GetMapping("/alerts-diagnostic")
    public Map<String, Object> runDiagnostic() {
        Map<String, Object> report = new HashMap<>();
        
        // 1. Obtener propiedades (sin límite de tiempo para el test)
        List<Property> properties = propertyRepository.findByDeletedAtIsNullAndStatusAndVisibilityAndTrashedAtIsNull(
                PropertyStatus.PUBLISHED, Visibility.PUBLIC);
        
        // 2. Obtener preferencias activas
        List<SearchPreference> preferences = searchPreferenceRepository.findByNotificationsEnabledTrue();
        
        report.put("totalPropertiesChecked", properties.size());
        report.put("totalPreferencesChecked", preferences.size());
        
        List<Map<String, Object>> analysis = new ArrayList<>();
        
        for (Property prop : properties) {
            for (SearchPreference pref : preferences) {
                Map<String, Object> matchResult = new HashMap<>();
                matchResult.put("propertyId", prop.getId());
                matchResult.put("preferenceId", pref.getId());
                matchResult.put("preferenceName", pref.getName());
                
                boolean ownerMatch = prop.getOwner().getId().equals(pref.getUser().getId());
                matchResult.put("isSameUser", ownerMatch);
                
                if (ownerMatch) {
                    matchResult.put("status", "SKIPPED (Same User)");
                } else {
                    List<String> failures = new ArrayList<>();
                    boolean matches = checkMatch(prop, pref, failures);
                    matchResult.put("matches", matches);
                    matchResult.put("failures", failures);
                    matchResult.put("status", matches ? "SUCCESS" : "FAILED");
                }
                
                analysis.add(matchResult);
            }
        }
        
        report.put("analysis", analysis);
        return report;
    }

    private boolean checkMatch(Property prop, SearchPreference pref, List<String> failures) {
        Map<String, Object> filters = pref.getFilters();
        if (filters == null || filters.isEmpty()) {
            failures.add("No filters defined");
            return false;
        }

        String prefType = (String) filters.get("propertyType");
        if (prefType != null) {
            if (prop.getPropertyType() == null) {
                failures.add("PropertyType mismatch: expected " + prefType + " but property has no type");
            } else if (!prop.getPropertyType().name().equalsIgnoreCase(prefType)) {
                failures.add("PropertyType mismatch: expected " + prefType + " but got " + prop.getPropertyType());
            }
        }

        String prefCat = (String) filters.get("category");
        if (prefCat != null) {
            if (prop.getCategory() == null) {
                failures.add("Category mismatch: expected " + prefCat + " but property has no category");
            } else if (!prop.getCategory().name().equalsIgnoreCase(prefCat)) {
                failures.add("Category mismatch: expected " + prefCat + " but got " + prop.getCategory());
            }
        }

        BigDecimal price = prop.getPrice();
        Number minPrice = (Number) filters.get("minPrice");
        Number maxPrice = (Number) filters.get("maxPrice");
        
        if (minPrice != null) {
            BigDecimal minVal = new BigDecimal(minPrice.toString());
            if (price.compareTo(minVal) < 0) {
                failures.add("Price too low: " + price + " < " + minPrice);
            }
        }
        if (maxPrice != null) {
            BigDecimal maxVal = new BigDecimal(maxPrice.toString());
            if (price.compareTo(maxVal) > 0) {
                failures.add("Price too high: " + price + " > " + maxPrice);
            }
        }

        Number minBed = (Number) filters.get("minBedrooms");
        if (minBed != null && prop.getBedrooms() < minBed.intValue()) {
            failures.add("Bedrooms too low: " + prop.getBedrooms() + " < " + minBed);
        }

        String prefCity = (String) filters.get("city");
        if (prefCity != null) {
            if (prop.getLocation() == null || prop.getLocation().getCity() == null) {
                failures.add("City mismatch: expected " + prefCity + " but property location or city is missing");
            } else if (!prop.getLocation().getCity().equalsIgnoreCase(prefCity)) {
                failures.add("City mismatch: expected " + prefCity + " but got " + prop.getLocation().getCity());
            }
        }

        return failures.isEmpty();
    }
}
