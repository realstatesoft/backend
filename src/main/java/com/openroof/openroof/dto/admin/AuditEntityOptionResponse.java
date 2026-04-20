package com.openroof.openroof.dto.admin;

import lombok.Builder;

/**
 * Opción para selectores de filtro de auditoría (id + etiqueta legible).
 */
@Builder
public record AuditEntityOptionResponse(Long id, String label) {
}
