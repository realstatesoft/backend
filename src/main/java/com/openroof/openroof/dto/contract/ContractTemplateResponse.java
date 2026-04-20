package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractType;

import java.time.LocalDateTime;

/**
 * Detalle de plantilla incluyendo contenido.
 */
public record ContractTemplateResponse(
        Long id,
        String name,
        ContractType contractType,
        String content,
        String templateVersion,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
