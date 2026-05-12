package com.openroof.openroof.security;

import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

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

    /**
     * Verifica que el usuario sea ADMIN, landlord o primary tenant del lease.
     * Lanza {@link AccessDeniedException} con mensaje descriptivo si no aplica.
     *
     * @param userId  id del usuario autenticado
     * @param leaseId id del lease a verificar
     * @throws AccessDeniedException si no tiene acceso
     */
    public void assertLeaseAccess(Long userId, Long leaseId) {
        if (userId == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Usuario " + userId + " no encontrado"));

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Lease " + leaseId + " no encontrado o sin acceso"));

        boolean isLandlord = lease.getLandlord() != null
                && lease.getLandlord().getId() != null
                && lease.getLandlord().getId().equals(userId);
        boolean isTenant = lease.getPrimaryTenant() != null
                && lease.getPrimaryTenant().getId() != null
                && lease.getPrimaryTenant().getId().equals(userId);

        if (!isLandlord && !isTenant) {
            throw new AccessDeniedException(
                    "Usuario " + userId + " no es landlord ni tenant del lease " + leaseId);
        }
    }

    /**
     * Versión boolean para {@code @PreAuthorize}. No lanza.
     */
    public boolean hasLeaseAccess(Long userId, Long leaseId) {
        try {
            assertLeaseAccess(userId, leaseId);
            return true;
        } catch (AccessDeniedException ex) {
            return false;
        }
    }
}
