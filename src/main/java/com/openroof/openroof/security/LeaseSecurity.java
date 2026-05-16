package com.openroof.openroof.security;

import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.AssignmentStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.PropertyAssignmentRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Componente de seguridad para validar acceso a recursos de leases.
 * Usado vía {@code @PreAuthorize("@leaseSecurity.hasLeaseAccess(...)")} o
 * llamada directa a {@link #assertLeaseAccess} desde servicios.
 */
@Component("leaseSecurity")
@RequiredArgsConstructor
public class LeaseSecurity {

    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;
    private final PropertySecurity propertySecurity;
    private final AgentProfileRepository agentProfileRepository;
    private final PropertyAssignmentRepository propertyAssignmentRepository;

    /**
     * Verifica que el usuario sea ADMIN, landlord o primary tenant del lease.
     * Lanza {@link AccessDeniedException} con mensaje descriptivo si no aplica.
     *
     * @param userId  id del usuario autenticado
     * @param leaseId id del lease a verificar
     * @throws AccessDeniedException si no tiene acceso
     */
    public void assertLeaseAccess(Long userId, Long leaseId) {
        UserAndLease ul = loadOrDeny(userId, leaseId);
        if (ul == null) return;  // ADMIN

        boolean isLandlord = ul.lease().getLandlord() != null
                && ul.lease().getLandlord().getId() != null
                && ul.lease().getLandlord().getId().equals(userId);
        boolean isTenant = ul.lease().getPrimaryTenant() != null
                && ul.lease().getPrimaryTenant().getId() != null
                && ul.lease().getPrimaryTenant().getId().equals(userId);

        if (!isLandlord && !isTenant) {
            throw new AccessDeniedException(
                    "Usuario " + userId + " no es landlord ni tenant del lease " + leaseId);
        }
    }

    /**
     * Versión boolean para {@code @PreAuthorize}. No lanza.
     */
    public boolean hasLeaseAccess(Long userId, Long leaseId) {
        if (leaseId == null) {
            return false;
        }
        try {
            assertLeaseAccess(userId, leaseId);
            return true;
        } catch (AccessDeniedException ex) {
            return false;
        }
    }

    /**
     * Verifica que el usuario pueda leer las cuotas de un lease.
     * Aplica a: ADMIN, landlord, primary tenant, o agente con assignment ACCEPTED sobre la propiedad.
     */
    public void assertInstallmentAccess(Long userId, Long leaseId) {
        UserAndLease ul = loadOrDeny(userId, leaseId);
        if (ul == null) return;  // ADMIN

        Lease lease = ul.lease();
        if (lease.getLandlord() != null && userId.equals(lease.getLandlord().getId())) return;
        if (lease.getPrimaryTenant() != null && userId.equals(lease.getPrimaryTenant().getId())) return;

        if (ul.user().getRole() == UserRole.AGENT && lease.getProperty() != null) {
            Optional<AgentProfile> profile = agentProfileRepository.findByUser_Id(userId);
            if (profile.isPresent()) {
                boolean assigned = propertyAssignmentRepository.findActiveByPropertyAndAgent(
                        lease.getProperty().getId(),
                        profile.get().getId(),
                        List.of(AssignmentStatus.ACCEPTED)
                ).isPresent();
                if (assigned) return;
            }
        }

        throw new AccessDeniedException("Usuario " + userId + " no tiene acceso al lease " + leaseId);
    }

    public boolean hasInstallmentAccess(Long userId, Long leaseId) {
        if (leaseId == null) {
            return false;
        }
        try {
            assertInstallmentAccess(userId, leaseId);
            return true;
        } catch (AccessDeniedException ex) {
            return false;
        }
    }

    /** Null-safe loader: validates ids, loads user+lease, short-circuits for ADMIN (returns null). */
    private UserAndLease loadOrDeny(Long userId, Long leaseId) {
        if (userId == null) throw new AccessDeniedException("Usuario no autenticado");
        if (leaseId == null) throw new AccessDeniedException("Lease id nulo");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Usuario " + userId + " no encontrado"));

        if (user.getRole() == UserRole.ADMIN) return null;

        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Lease " + leaseId + " no encontrado o sin acceso"));

        return new UserAndLease(user, lease);
    }

    private record UserAndLease(User user, Lease lease) {}

    /**
     * Verifica que el usuario pueda gestionar (editar/activar/terminar) un lease,
     * es decir, que sea ADMIN, el owner de la propiedad, o el agente asignado a ella.
     * Acepta {@code Object} para compatibilidad con SpEL ({@code principal} tiene tipo
     * declarado {@code Object} en {@code SecurityExpressionRoot}).
     */
    public boolean canManageLease(Long leaseId, Object principalObj) {
        if (leaseId == null) return false;
        if (!(principalObj instanceof User currentUser)) return false;
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        Optional<Lease> lease = leaseRepository.findById(leaseId);
        if (lease.isEmpty() || lease.get().getProperty() == null) {
            return false;
        }
        return propertySecurity.canModify(lease.get().getProperty().getId(), currentUser);
    }

    /**
     * Verifica que el usuario pueda crear un lease sobre una propiedad.
     * Delega en {@link PropertySecurity#canModify}; acepta {@code Object} por la misma
     * razón que {@link #canManageLease}.
     */
    public boolean canCreateLease(Long propertyId, Object principalObj) {
        if (propertyId == null) return false;
        if (!(principalObj instanceof User currentUser)) return false;
        if (currentUser.getRole() == UserRole.ADMIN) return true;
        return propertySecurity.canModify(propertyId, currentUser);
    }
}
