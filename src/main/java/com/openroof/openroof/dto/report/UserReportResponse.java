package com.openroof.openroof.dto.report;

import com.openroof.openroof.model.admin.UserReport;
import com.openroof.openroof.model.enums.ReportStatus;
import com.openroof.openroof.model.enums.UserReportReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Información de un reporte de usuario")
public record UserReportResponse(

        @Schema(description = "ID del reporte")
        Long id,

        @Schema(description = "ID del usuario reportado")
        Long reportedUserId,

        @Schema(description = "Nombre del usuario reportado")
        String reportedUserName,

        @Schema(description = "Email del usuario reportado")
        String reportedUserEmail,

        @Schema(description = "ID del usuario que realizó el reporte")
        Long reporterUserId,

        @Schema(description = "Nombre del usuario que realizó el reporte")
        String reporterUserName,

        @Schema(description = "Motivo del reporte")
        UserReportReason reason,

        @Schema(description = "Descripción adicional del reporte")
        String description,

        @Schema(description = "Estado actual del reporte")
        ReportStatus status,

        @Schema(description = "Fecha de creación del reporte")
        LocalDateTime createdAt
) {
    public static UserReportResponse from(UserReport report) {
        return new UserReportResponse(
                report.getId(),
                report.getReportedUser().getId(),
                report.getReportedUser().getName(),
                report.getReportedUser().getEmail(),
                report.getReporterUser().getId(),
                report.getReporterUser().getName(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
