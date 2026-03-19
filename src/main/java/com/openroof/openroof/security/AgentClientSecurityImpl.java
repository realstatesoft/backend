package com.openroof.openroof.security;

import org.springframework.stereotype.Component;

import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentClientRepository;
import com.openroof.openroof.repository.AgentProfileRepository;

import lombok.RequiredArgsConstructor;

/**
 * Componente para evaluar reglas de seguridad sobre AgentClient
 */
@Component("agentClientSecurity")
@RequiredArgsConstructor
public class AgentClientSecurityImpl implements AgentClientSecurity {

    private final AgentClientRepository agentClientRepository;
    private final AgentProfileRepository agentProfileRepository;

    @Override
    public boolean canAccess(Long agentClientId, Object principal) {
        if (!(principal instanceof User currentUser)) {
            return false;
        }

        // 1. ADMIN tiene acceso a todo
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }

        // 2. AGENT solo si es el agente asignado a este cliente
        if (currentUser.getRole() == UserRole.AGENT) {
            return agentClientRepository.findByIdWithAgentAndUser(agentClientId)
                    .map(client -> client.getAgent() != null &&
                                   client.getAgent().getUser().getId().equals(currentUser.getId()))
                    .orElse(false);
        }

        // USER no tiene acceso a esta entidad
        return false;
    }

    @Override
    public boolean canManageAgent(Long agentId, Object principal) {
        if (!(principal instanceof User currentUser)) {
            return false;
        }

        // 1. ADMIN tiene acceso a todo
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }

        // 2. AGENT solo si está operando sobre su propio agentId
        if (currentUser.getRole() == UserRole.AGENT) {
            return agentProfileRepository.findByIdWithUser(agentId)
                    .map(agentProfile -> agentProfile.getUser().getId().equals(currentUser.getId()))
                    .orElse(false);
        }

        return false;
    }

    @Override
    public boolean isAgent(Object principal) {
        if (!(principal instanceof User currentUser)) {
            return false;
        }
        return currentUser.getRole() == UserRole.AGENT;
    }
}
