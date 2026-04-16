package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.report.CreateUserReportRequest;
import com.openroof.openroof.dto.report.UpdateUserReportStatusRequest;
import com.openroof.openroof.dto.report.UserReportResponse;
import com.openroof.openroof.model.enums.ReportStatus;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.UserReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints para reportar usuarios y gestionar los reportes.
 *
 * Base URL (con context-path /api): /api/user-reports
 */
@RestController
@RequestMapping("/user-reports")
@RequiredArgsConstructor
@Tag(name = "User Reports", description = "Reporte y gestión de usuarios inapropiados")
public class UserReportController {

    private final UserReportService userReportService;

    // ─── USER: crear reporte ───────────────────────────────────────────────

    /**
     * POST /user-reports
     * Cualquier usuario autenticado puede reportar a otro usuario.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reportar un usuario", description = "Crea un reporte contra otro usuario")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Reporte creado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Solicitud inválida o reporte duplicado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario reportado no encontrado")
    })
    public ResponseEntity<ApiResponse<UserReportResponse>> createReport(
            @Valid @RequestBody CreateUserReportRequest request,
            @AuthenticationPrincipal User currentUser) {

        UserReportResponse response = userReportService.createReport(request, currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Reporte creado exitosamente"));
    }

    // ─── ADMIN: listar reportes ────────────────────────────────────────────

    /**
     * GET /user-reports
     * Lista todos los reportes, con paginación y filtro opcional por status.
     * Solo ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar reportes de usuarios (solo ADMIN)", description = "Devuelve todos los reportes con paginación. Filtra por status si se indica.")
    public ResponseEntity<ApiResponse<Page<UserReportResponse>>> getAllReports(
            @Parameter(description = "Filtrar por estado: PENDIENTE, RESUELTO, DESESTIMADO")
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<UserReportResponse> page = userReportService.getAllReports(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    // ─── ADMIN: cambiar status ─────────────────────────────────────────────

    /**
     * PUT /user-reports/{id}/status
     * Cambia el estado de un reporte. Solo ADMIN.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de un reporte (solo ADMIN)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reporte no encontrado")
    })
    public ResponseEntity<ApiResponse<UserReportResponse>> updateReportStatus(
            @Parameter(description = "ID del reporte") @PathVariable Long id,
            @Valid @RequestBody UpdateUserReportStatusRequest request) {

        UserReportResponse response = userReportService.updateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.ok(response, "Estado del reporte actualizado exitosamente"));
    }
}
