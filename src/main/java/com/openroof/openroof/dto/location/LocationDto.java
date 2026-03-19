package com.openroof.openroof.dto.location;

public record LocationDto(
        Long id,
        String name,
        String city,
        String department,
        String country,
        Double lat,
        Double lng,
        boolean isNew
) {
}
