package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.location.LocationDto;
import com.openroof.openroof.model.property.Location;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public LocationDto toDto(Location location) {
        if (location == null) {
            return null;
        }

        return new LocationDto(
                location.getId(),
                location.getName(),
                location.getCity(),
                location.getDepartment(),
                location.getCountry(),
                location.getLat(),
                location.getLng()
        );
    }
}
