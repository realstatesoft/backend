package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.PropertySummaryResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyView;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.PropertyViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyViewService {

    private static final int RECENT_LIMIT = 10;

    private final PropertyRepository propertyRepository;
    private final PropertyViewRepository propertyViewRepository;
    private final PropertyMapper propertyMapper;

    public void registerRecentView(Long propertyId, User user) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        propertyViewRepository.findFirstByUser_IdAndProperty_IdOrderByCreatedAtDesc(user.getId(), propertyId)
                .ifPresent(propertyViewRepository::delete);

        PropertyView view = PropertyView.builder()
                .property(property)
                .user(user)
                .build();

        propertyViewRepository.save(view);
    }

    @Transactional(readOnly = true)
    public List<PropertySummaryResponse> getRecentProperties(Long userId) {
        Map<Long, Property> orderedUnique = new LinkedHashMap<>();

        for (PropertyView view : propertyViewRepository.findRecentByUserId(userId)) {
            Property property = view.getProperty();
            if (property == null || property.getId() == null) {
                continue;
            }
            orderedUnique.putIfAbsent(property.getId(), property);
            if (orderedUnique.size() >= RECENT_LIMIT) {
                break;
            }
        }

        return orderedUnique.values().stream()
                .map(propertyMapper::toSummaryResponse)
                .toList();
    }
}
