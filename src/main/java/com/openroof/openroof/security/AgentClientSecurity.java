package com.openroof.openroof.security;

import com.openroof.openroof.model.user.User;

public interface AgentClientSecurity {
    boolean canAccess(Long agentClientId, Object principal);
    boolean canManageAgent(Long agentId, Object principal);
}
