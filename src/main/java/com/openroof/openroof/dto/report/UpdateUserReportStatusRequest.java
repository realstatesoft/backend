package com.openroof.openroof.dto.report;

import com.openroof.openroof.model.enums.ReportStatus;
import com.openroof.openroof.model.enums.UserReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitud para cambiar el estado de un reporte de usuario")
public record UpdateUserReportStatusRequest(

        @NotNull(message = "El estado del reporte es obligatorio")
        @Schema(description = "Nuevo estado del reporte", requiredMode = Schema.RequiredMode.REQUIRED)
        ReportStatus status
) {
}
