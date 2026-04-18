package com.openroof.openroof.dto.report;

import com.openroof.openroof.model.admin.UserReport;
import com.openroof.openroof.model.enums.ReportStatus;
import com.openroof.openroof.model.enums.UserReportReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resumen público de un reporte de usuario")
public record UserReportSummary(
        @Schema(description = "ID del reporte")
        Long id,

        @Schema(description = "ID del usuario reportado")
        Long reportedUserId,

        @Schema(description = "Motivo del reporte")
        UserReportReason reason,

        @Schema(description = "Estado inicial del reporte")
        ReportStatus status,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt
) {
    public static UserReportSummary from(UserReport report) {
        return new UserReportSummary(
                report.getId(),
                report.getReportedUser().getId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
