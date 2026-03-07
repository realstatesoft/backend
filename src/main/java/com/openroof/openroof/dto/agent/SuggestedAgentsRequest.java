package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request para obtener agentes sugeridos basados en criterios de la propiedad.
 */
@Schema(description = "Criterios para sugerir agentes")
public record SuggestedAgentsRequest(

        @Schema(description = "Tipo de propiedad", example = "HOUSE")
        PropertyType propertyType,

        @Schema(description = "Categoría (venta/alquiler)", example = "SALE")
        PropertyCategory category,

        @Schema(description = "Ciudad o zona de la propiedad", example = "Asunción")
        String city,

        @Schema(description = "ID de la ubicación (opcional)", example = "1")
        Long locationId,

        @Schema(description = "Cantidad máxima de agentes a retornar", example = "5")
        Integer limit
) {
    public SuggestedAgentsRequest {
        // Defaults
        if (limit == null || limit <= 0) {
            limit = 5;
        }
        if (limit > 20) {
            limit = 20;
        }
    }
}
