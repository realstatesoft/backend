package com.openroof.openroof.security;

import org.springframework.stereotype.Component;


import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PropertyRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import com.openroof.openroof.model.property.Property;;;;

@Component("propertySecurity")
@RequiredArgsConstructor
public class PropertySecurity {

    private final PropertyRepository propertyRepository;

    public boolean canModify(Long propertyId, User currentUser) {
        // 1. Regla ADMIN: Si es administrador, tiene vía libre.
        if (currentUser.getRole() == UserRole.ADMIN) return true;

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Propiedad no encontrada"));

        // 2. Regla AGENT: Solo puede modificar si su perfil de agente está asignado a la propiedad
        if (currentUser.getRole() == UserRole.AGENT) {
            return property.getAgent() != null && 
                   property.getAgent().getUser().getId().equals(currentUser.getId());
        }

        // 3. Regla USER (OWNER): Solo puede modificar si es el dueño
        if (currentUser.getRole() == UserRole.USER) {
            return property.getOwner().getId().equals(currentUser.getId());
        }

        return false;
    }
}
