package com.openroof.openroof.service;

import com.openroof.openroof.dto.favorite.FavoriteActionResponse;
import com.openroof.openroof.dto.property.PropertySummaryResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.interaction.Favorite;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.FavoriteRepository;
import com.openroof.openroof.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private static final LocalDateTime DEFAULT_ADDED_FROM = LocalDate.of(1970, 1, 1).atStartOfDay();
    private static final LocalDateTime DEFAULT_ADDED_TO_EXCLUSIVE = LocalDate.of(9999, 12, 31).plusDays(1).atStartOfDay();

    private final FavoriteRepository favoriteRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;

    public FavoriteActionResponse addFavorite(Long propertyId, User user) {
        Property property = propertyRepository.findByIdForUpdate(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propiedad no encontrada con ID: " + propertyId));

        if (property.getTrashedAt() != null) {
            throw new BadRequestException("No se puede agregar a favoritos una propiedad en papelera");
        }

        boolean alreadyFavorited = favoriteRepository.existsByUser_IdAndProperty_Id(user.getId(), propertyId);
        if (alreadyFavorited) {
            return new FavoriteActionResponse(
                    propertyId,
                    true,
                    property.getFavoriteCount() != null ? property.getFavoriteCount() : 0);
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .property(property)
                .build();
        favoriteRepository.save(favorite);

        int currentCount = property.getFavoriteCount() != null ? property.getFavoriteCount() : 0;
        property.setFavoriteCount(currentCount + 1);
        propertyRepository.save(property);

        return new FavoriteActionResponse(propertyId, true, property.getFavoriteCount());
    }

    public FavoriteActionResponse removeFavorite(Long propertyId, User user) {
        Property property = propertyRepository.findByIdForUpdate(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propiedad no encontrada con ID: " + propertyId));

        int deleted = favoriteRepository.deleteByUserAndProperty(user.getId(), propertyId);

        if (deleted > 0) {
            int currentCount = property.getFavoriteCount() != null ? property.getFavoriteCount() : 0;
            property.setFavoriteCount(Math.max(0, currentCount - 1));
            propertyRepository.save(property);
        }

        return new FavoriteActionResponse(
                propertyId,
                false,
                property.getFavoriteCount() != null ? property.getFavoriteCount() : 0);
    }

    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getMyFavorites(
            User user,
            String status,
            LocalDate addedFrom,
            LocalDate addedTo,
            Pageable pageable) {

        PropertyStatus parsedStatus = parseStatus(status);
        LocalDateTime addedFromDateTime = addedFrom != null ? addedFrom.atStartOfDay() : DEFAULT_ADDED_FROM;
        LocalDateTime addedToExclusive = addedTo != null ? addedTo.plusDays(1).atStartOfDay() : DEFAULT_ADDED_TO_EXCLUSIVE;

        if (!addedFromDateTime.isBefore(addedToExclusive)) {
            throw new BadRequestException("El rango de fechas es inválido: addedFrom debe ser menor o igual a addedTo");
        }

        return favoriteRepository.findFavoritePropertiesByUserId(
                        user.getId(),
                        parsedStatus,
                        addedFromDateTime,
                        addedToExclusive,
                        pageable)
                .map(propertyMapper::toSummaryResponse);
    }

    private PropertyStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }

        try {
            return PropertyStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Estado inválido para favoritos: " + rawStatus);
        }
    }
}
