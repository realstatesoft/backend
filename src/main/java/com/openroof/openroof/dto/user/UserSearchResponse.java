package com.openroof.openroof.dto.user;

/**
 * DTO mínimo para búsqueda de usuarios — solo expone datos públicos necesarios
 * para que un agente identifique al usuario antes de vincularlo como cliente.
 */
public record UserSearchResponse(
        Long id,
        String name,
        String email
) {}
