package com.openroof.openroof.dto.report;

import com.openroof.openroof.model.enums.UserReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para crear un reporte de usuario")
public record CreateUserReportRequest(

        @NotNull(message = "El ID del usuario reportado es obligatorio")
        @Positive(message = "El ID del usuario reportado debe ser un número positivo")
        @Schema(description = "ID del usuario a reportar", requiredMode = Schema.RequiredMode.REQUIRED)
        Long reportedUserId,

        @NotNull(message = "El motivo del reporte es obligatorio")
        @Schema(description = "Motivo del reporte", requiredMode = Schema.RequiredMode.REQUIRED)
        UserReportReason reason,

        @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
        @Schema(description = "Descripción adicional del reporte (opcional)")
        String description
) {
}
