package com.openroof.openroof.service;

import com.openroof.openroof.dto.report.CreateUserReportRequest;
import com.openroof.openroof.dto.report.UserReportResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.admin.UserReport;
import com.openroof.openroof.model.enums.ReportStatus;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserReportService {

    private final UserReportRepository userReportRepository;
    private final UserService userService;

    // ─── CREATE ───────────────────────────────────────────────────────────────

    /**
     * Crea un nuevo reporte de usuario.
     * Valida que el usuario reportado exista, que no sea el mismo que reporta
     * y que no exista ya un reporte PENDIENTE del mismo reporter contra el mismo usuario.
     */
    public UserReportResponse createReport(CreateUserReportRequest request, User reporter) {
        if (reporter.getId().equals(request.reportedUserId())) {
            throw new BadRequestException("No puedes reportarte a ti mismo");
        }

        User reportedUser = userService.findById(request.reportedUserId());

        boolean alreadyReported = userReportRepository
                .existsByReportedUserIdAndReporterUserIdAndStatus(
                        reportedUser.getId(), reporter.getId(), ReportStatus.PENDIENTE);

        if (alreadyReported) {
            throw new BadRequestException("Ya tienes un reporte pendiente contra este usuario");
        }

        UserReport report = UserReport.builder()
                .reportedUser(reportedUser)
                .reporterUser(reporter)
                .reason(request.reason())
                .description(request.description())
                .build();

        UserReport saved = userReportRepository.save(report);
        log.info("Usuario {} reportado por usuario {} por motivo {}", reportedUser.getId(), reporter.getId(), request.reason());
        return UserReportResponse.from(saved);
    }

    // ─── READ (solo ADMIN) ─────────────────────────────────────────────────

    /**
     * Devuelve todos los reportes de usuario paginados.
     * Si se proporciona un status, filtra por él.
     */
    @Transactional(readOnly = true)
    public Page<UserReportResponse> getAllReports(ReportStatus status, Pageable pageable) {
        Page<UserReport> page = (status != null)
                ? userReportRepository.findByStatus(status, pageable)
                : userReportRepository.findAll(pageable);

        return page.map(UserReportResponse::from);
    }

    // ─── UPDATE STATUS (solo ADMIN) ────────────────────────────────────────

    /**
     * Cambia el estado de un reporte de usuario.
     */
    public UserReportResponse updateStatus(Long reportId, ReportStatus newStatus) {
        UserReport report = userReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con ID: " + reportId));

        report.setStatus(newStatus);
        UserReport saved = userReportRepository.save(report);
        log.info("Estado del reporte {} cambiado a {}", reportId, newStatus);
        return UserReportResponse.from(saved);
    }
}
