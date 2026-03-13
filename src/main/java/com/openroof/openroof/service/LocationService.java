package com.openroof.openroof.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openroof.openroof.dto.location.LocationDto;
import com.openroof.openroof.mapper.LocationMapper;
import com.openroof.openroof.model.property.Location;
import com.openroof.openroof.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    public List<LocationDto> matchByCity(String city) {
        if (city == null || city.isBlank()) {
            return List.of();
        }
        
        // As a simple match for now, we find by exact name or we could use containing.
        // Assuming location names are like city or city, department
        // Let's find all by city or name. Since we don't have findByCity in repo, we'll fetch all and filter, or we add findByCity to repo.
        // Given we are creating a new service, let's use a repository method that we will add: findByCityIgnoreCase
        return locationRepository.findByCityIgnoreCase(city.trim())
                .stream()
                .map(locationMapper::toDto)
                .toList();
    }

    @Transactional
    public LocationDto findOrCreate(String city, String department, String country, Double lat, Double lng) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }

        // Check if there is already a matching city
        List<Location> existing = locationRepository.findByCityIgnoreCase(city.trim());
        if (!existing.isEmpty()) {
            // Prefer match with same department if available
            Location loc = existing.stream()
                    .filter(l -> department == null || department.isBlank() 
                            || department.trim().equalsIgnoreCase(l.getDepartment()))
                    .findFirst()
                    .orElse(existing.get(0));
            
            // Si la location existe pero no tiene coordenadas, las seteamos
            if ((loc.getGeoLocation() == null || loc.getGeoLocation().getLat() == null) && lat != null && lng != null) {
                com.openroof.openroof.common.embeddable.GeoLocation geo = new com.openroof.openroof.common.embeddable.GeoLocation();
                geo.setLat(new java.math.BigDecimal(lat.toString()));
                geo.setLng(new java.math.BigDecimal(lng.toString()));
                loc.setGeoLocation(geo);
                loc = locationRepository.save(loc);
            }
            
            return locationMapper.toDto(loc); // Return the first match
        }

        // Create new location
        Location newLocation = Location.builder()
                .name(city.trim() + (department != null && !department.isBlank() ? ", " + department.trim() : ""))
                .city(city.trim())
                .department(department != null ? department.trim() : "")
                .country(country != null && !country.isBlank() ? country.trim() : "Paraguay")
                .build();
                
        // Set coordinates if provided
        if (lat != null && lng != null) {
             com.openroof.openroof.common.embeddable.GeoLocation geo = new com.openroof.openroof.common.embeddable.GeoLocation();
             geo.setLat(new java.math.BigDecimal(lat.toString()));
             geo.setLng(new java.math.BigDecimal(lng.toString()));
             newLocation.setGeoLocation(geo);
        }

        newLocation = locationRepository.save(newLocation);
        return locationMapper.toDto(newLocation);
    }
}
