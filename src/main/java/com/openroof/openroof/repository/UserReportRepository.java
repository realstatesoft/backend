package com.openroof.openroof.repository;

import com.openroof.openroof.model.admin.UserReport;
import com.openroof.openroof.model.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    /** Todos los reportes paginados (uso exclusivo ADMIN). */
    Page<UserReport> findAll(Pageable pageable);

    /** Reportes filtrados por status, paginados (uso exclusivo ADMIN). */
    Page<UserReport> findByStatus(ReportStatus status, Pageable pageable);

    /** Verifica si ya existe un reporte activo (PENDIENTE) del mismo reporter contra el mismo usuario reportado. */
    boolean existsByReportedUserIdAndReporterUserIdAndStatus(
            Long reportedUserId, Long reporterUserId, ReportStatus status);
}
