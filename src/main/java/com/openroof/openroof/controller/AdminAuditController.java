package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.admin.AuditEntityOptionResponse;
import com.openroof.openroof.dto.admin.AuditLogResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.enums.AuditAction;
import com.openroof.openroof.model.enums.AuditEntityType;
import com.openroof.openroof.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin — Auditoría", description = "Consulta de registros de auditoría")
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping("/audit-logs/entity-options")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar entidades para filtro de auditoría (selector)")
    public ResponseEntity<ApiResponse<List<AuditEntityOptionResponse>>> entityOptions(
            @Parameter(description = "USER, PROPERTY o CONTRACT", required = true) @RequestParam String entityType,
            @Parameter(description = "Texto de búsqueda (título, nombre, email o id de contrato)") @RequestParam(required = false) String q,
            @Parameter(description = "Máximo de resultados (1–50)") @RequestParam(required = false, defaultValue = "40") Integer limit) {

        AuditEntityType type = parseEnum(entityType, AuditEntityType.class, "entityType");
        int safeLimit = limit == null ? 40 : Math.min(50, Math.max(1, limit));
        List<AuditEntityOptionResponse> options = auditService.suggestAuditEntities(type, q, safeLimit);
        return ResponseEntity.ok(ApiResponse.ok(options));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar registros de auditoría (paginado, filtros opcionales)")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> list(
            @Parameter(description = "ID del usuario actor (opcional; alternativa a userSearch)") @RequestParam(required = false) Long userId,
            @Parameter(description = "Nombre o email del actor (coincidencia parcial, insensible a mayúsculas)") @RequestParam(required = false) String userSearch,
            @Parameter(description = "Tipo de entidad (USER, PROPERTY, CONTRACT)") @RequestParam(required = false) String entityType,
            @Parameter(description = "ID numérico de la entidad") @RequestParam(required = false) Long entityId,
            @Parameter(description = "Acción (LOGIN, CREATE, …)") @RequestParam(required = false) String action,
            @Parameter(description = "Inicio del rango (ISO-8601)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fin del rango (ISO-8601)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        AuditEntityType entityTypeEnum = parseEnum(entityType, AuditEntityType.class, "entityType");
        AuditAction actionEnum = parseEnum(action, AuditAction.class, "action");

        Page<AuditLogResponse> page = auditService.search(
                userId, userSearch, entityTypeEnum, entityId, actionEnum, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    private static <E extends Enum<E>> E parseEnum(String raw, Class<E> type, String paramName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Valor inválido para " + paramName + ": " + raw);
        }
    }
}
