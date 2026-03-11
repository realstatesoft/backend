package com.openroof.openroof.security;

import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Componente de seguridad para validar el acceso a recursos de leads.
 * Se usa junto con {@code @PreAuthorize} en los controladores.
 */
@Component("leadSecurity")
@RequiredArgsConstructor
public class LeadSecurity {

    private final AgentProfileRepository agentProfileRepository;
    private final LeadRepository leadRepository;

    /**
     * Verifica si el usuario autenticado es el propietario del perfil de agente dado.
     *
     * @param currentUser el usuario autenticado (principal)
     * @param agentId     el ID del perfil de agente a verificar
     * @return {@code true} si el usuario es ADMIN o es el agente dueño del perfil
     */
    public boolean isAgentOwner(User currentUser, Long agentId) {
        if (currentUser.getRole() == UserRole.ADMIN) return true;
        return agentProfileRepository.findByUser_Id(currentUser.getId())
                .map(profile -> profile.getId().equals(agentId))
                .orElse(false);
    }

    /**
     * Verifica si el usuario autenticado es el agente propietario del lead dado.
     *
     * @param currentUser el usuario autenticado (principal)
     * @param leadId      el ID del lead a verificar
     * @return {@code true} si el usuario es ADMIN o es el agente dueño del lead
     */
    public boolean isLeadOwner(User currentUser, Long leadId) {
        if (currentUser.getRole() == UserRole.ADMIN) return true;
        return leadRepository.existsByIdAndAgent_User_Id(leadId, currentUser.getId());
    }
}
