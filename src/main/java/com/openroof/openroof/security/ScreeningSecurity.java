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

import java.util.Objects;

/**
 * Reglas de acceso para tenant screenings.
 *
 * Lectura por id (legacy): ADMIN o AGENT asignado a la property de la application.
 * Lectura por applicationId: ADMIN, owner de la property o AGENT asignado.
 * Escritura por applicationId: ADMIN, owner de la property o AGENT asignado.
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
        return rentalApplicationRepository.findById(applicationId)
                .map(app -> isOwnerOfApplication(app, currentUser)
                        || isAgentOfApplication(app, currentUser))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canManageByApplication(Long applicationId, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        return rentalApplicationRepository.findById(applicationId)
                .map(app -> isOwnerOfApplication(app, currentUser)
                        || isAgentOfApplication(app, currentUser))
                .orElse(false);
    }

    private boolean isOwnerOfApplication(RentalApplication application, User currentUser) {
        if (application == null || currentUser == null) {
            return false;
        }
        Property property = application.getProperty();
        if (property == null) {
            return false;
        }
        User owner = property.getOwner();
        if (owner == null || owner.getId() == null) {
            return false;
        }
        return Objects.equals(owner.getId(), currentUser.getId());
    }

    private boolean isAgentOfApplication(RentalApplication application, User currentUser) {
        if (application == null || currentUser == null) {
            return false;
        }
        Property property = application.getProperty();
        if (property == null || property.getAgent() == null) {
            return false;
        }
        User agentUser = property.getAgent().getUser();
        if (agentUser == null || agentUser.getId() == null) {
            return false;
        }
        return Objects.equals(agentUser.getId(), currentUser.getId());
    }
}
