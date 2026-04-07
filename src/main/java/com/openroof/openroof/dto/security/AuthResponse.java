package com.openroof.openroof.dto.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Auth: Enrique Rios
 * Desc: Respuesta unificada para login, registro y refresco de token.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Long id;
    private String accessToken;
    private String refreshToken;
    private String email;
    private String role;
    /** ID del perfil de agente (solo presente cuando role == AGENT) */
    private Long agentProfileId;
}