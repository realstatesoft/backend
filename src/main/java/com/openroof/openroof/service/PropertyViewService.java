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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PropertyViewService {

    private static final int RECENT_LIMIT = 10;

    private final PropertyRepository propertyRepository;
    private final PropertyViewRepository propertyViewRepository;
    private final PropertyMapper propertyMapper;

    public void registerRecentView(Long propertyId, User user) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        // Eliminar vistas previas de este usuario para esta propiedad para evitar duplicados
        // y prevenir ObjectOptimisticLockingFailureException en accesos concurrentes.
        propertyViewRepository.deleteByUserIdAndPropertyId(user.getId(), propertyId);

        PropertyView view = PropertyView.builder()
                .property(property)
                .user(user)
                .build();

        try {
            propertyViewRepository.save(view);
        } catch (DataIntegrityViolationException e) {
            log.warn("Vista de propiedad duplicada detectada para user={} property={}", user.getId(), propertyId);
        }
    }

    @Transactional(readOnly = true)
    public List<PropertySummaryResponse> getRecentProperties(Long userId) {
        Map<Long, Property> orderedUnique = new LinkedHashMap<>();
        PageRequest recentPage = PageRequest.of(0, RECENT_LIMIT, Sort.by("createdAt").descending());

        for (PropertyView view : propertyViewRepository.findRecentByUserId(userId, recentPage)) {
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
