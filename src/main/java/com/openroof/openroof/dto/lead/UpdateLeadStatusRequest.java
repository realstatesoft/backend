package com.openroof.openroof.dto.lead;

import jakarta.validation.constraints.NotBlank;

/**
 * Actualización del estado pipeline de un prospecto (por nombre de {@code LeadStatus}).
 */
public record UpdateLeadStatusRequest(
        @NotBlank(message = "El nombre del estado es obligatorio") String statusName
) {
}
