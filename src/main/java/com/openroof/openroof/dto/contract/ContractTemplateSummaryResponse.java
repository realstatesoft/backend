package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractType;

import java.time.LocalDateTime;

/**
 * Listado admin de plantillas (sin cuerpo largo).
 */
public record ContractTemplateSummaryResponse(
        Long id,
        String name,
        ContractType contractType,
        String templateVersion,
        Boolean active,
        LocalDateTime updatedAt
) {
}
