package com.openroof.openroof.security;

import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.screening.TenantScreening;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.RentalApplicationRepository;
import com.openroof.openroof.repository.TenantScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de acceso de lectura para tenant screenings.
 * Escritura se controla con hasRole('ADMIN') directamente en el controller.
 * ADMIN ⇒ acceso total. AGENT ⇒ solo si está asignado a la property de la application.
 */
@Component("screeningSecurity")
@RequiredArgsConstructor
public class ScreeningSecurity {

    private final TenantScreeningRepository screeningRepository;
    private final RentalApplicationRepository rentalApplicationRepository;

    @Transactional(readOnly = true)
    public boolean hasReadAccess(Long screeningId, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (currentUser.getRole() != UserRole.AGENT) {
            return false;
        }
        return screeningRepository.findById(screeningId)
                .map(TenantScreening::getApplication)
                .map(application -> isAgentOfApplication(application, currentUser))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasReadAccessByApplication(Long applicationId, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (currentUser.getRole() != UserRole.AGENT) {
            return false;
        }
        return rentalApplicationRepository.findById(applicationId)
                .map(application -> isAgentOfApplication(application, currentUser))
                .orElse(false);
    }

    private boolean isAgentOfApplication(RentalApplication application, User currentUser) {
        Property property = application.getProperty();
        if (property == null || property.getAgent() == null || property.getAgent().getUser() == null) {
            return false;
        }
        return property.getAgent().getUser().getId().equals(currentUser.getId());
    }
}
