package com.openroof.openroof.security;

public interface AgentClientSecurity {
    boolean canAccess(Long agentClientId, Object principal);
    boolean canAccessExternal(Long externalClientId, Object principal);
    boolean canManageAgent(Long agentId, Object principal);
    boolean isAgent(Object principal);
}
