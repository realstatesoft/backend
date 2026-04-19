package com.openroof.openroof.service;

import com.openroof.openroof.audit.AuditLogSpecification;
import com.openroof.openroof.audit.AuditPayloadSanitizer;
import com.openroof.openroof.dto.admin.AuditEntityOptionResponse;
import com.openroof.openroof.dto.admin.AuditLogResponse;
import com.openroof.openroof.model.admin.AuditLog;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.enums.AuditAction;
import com.openroof.openroof.model.enums.AuditEntityType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AuditLogRepository;
import com.openroof.openroof.repository.ContractRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int MAX_USER_SEARCH_LEN = 120;
    private static final int MAX_ENTITY_PICKER_QUERY_LEN = 80;
    private static final int MAX_ENTITY_PICKER_LIMIT = 50;

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final ContractRepository contractRepository;

    /**
     * Registra un evento de auditoría. Intenta rellenar IP y User-Agent desde el request HTTP actual.
     */
    @Transactional
    public void log(
            User actor,
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            Map<String, Object> oldValues,
            Map<String, Object> newValues) {

        String ip = null;
        String ua = null;
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            ip = resolveClientIp(req);
            ua = req.getHeader("User-Agent");
        }

        AuditLog entry = AuditLog.builder()
                .user(actor)
                .entityType(entityType.name())
                .entityId(entityId)
                .action(action.name())
                .oldValues(AuditPayloadSanitizer.sanitize(oldValues))
                .newValues(AuditPayloadSanitizer.sanitize(newValues))
                .ipAddress(ip)
                .userAgent(ua)
                .build();

        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            Long userId,
            String userSearch,
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            java.time.LocalDateTime createdFrom,
            java.time.LocalDateTime createdTo,
            Pageable pageable) {

        String normalizedSearch = normalizeUserSearch(userSearch);
        var spec = AuditLogSpecification.build(
                userId, normalizedSearch, entityType, entityId, action, createdFrom, createdTo);
        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /**
     * Opciones para selectores de filtro (admin): usuarios, propiedades o contratos con etiqueta legible.
     */
    @Transactional(readOnly = true)
    public List<AuditEntityOptionResponse> suggestAuditEntities(AuditEntityType entityType, String q, int limit) {
        int safeLimit = Math.min(MAX_ENTITY_PICKER_LIMIT, Math.max(1, limit));
        String query = q == null ? "" : q.trim();
        if (query.length() > MAX_ENTITY_PICKER_QUERY_LEN) {
            query = query.substring(0, MAX_ENTITY_PICKER_QUERY_LEN);
        }
        Pageable pageable = PageRequest.of(0, safeLimit);

        return switch (entityType) {
            case USER -> userRepository
                    .searchForAuditPicker(query.isEmpty() ? "" : query, pageable)
                    .stream()
                    .map(AuditService::userToOption)
                    .toList();
            case PROPERTY -> propertyRepository
                    .searchByTitleForAuditPicker(query.isEmpty() ? "" : query, pageable)
                    .stream()
                    .map(AuditService::propertyToOption)
                    .toList();
            case CONTRACT -> {
                long idOrNeg = -1L;
                if (!query.isEmpty()) {
                    try {
                        idOrNeg = Long.parseLong(query);
                    } catch (NumberFormatException ignored) {
                        idOrNeg = -1L;
                    }
                }
                yield contractRepository
                        .searchForAuditPicker(query.isEmpty() ? "" : query, idOrNeg, pageable)
                        .stream()
                        .map(AuditService::contractToOption)
                        .toList();
            }
        };
    }

    private static String normalizeUserSearch(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > MAX_USER_SEARCH_LEN) {
            return t.substring(0, MAX_USER_SEARCH_LEN);
        }
        return t;
    }

    private static AuditEntityOptionResponse userToOption(User u) {
        String name = u.getName();
        String email = u.getEmail();
        String label = (name != null && !name.isBlank()) ? name.trim() + " (" + email + ")" : email;
        return AuditEntityOptionResponse.builder().id(u.getId()).label(label).build();
    }

    private static AuditEntityOptionResponse propertyToOption(Property p) {
        String title = p.getTitle() != null ? p.getTitle().trim() : "";
        if (title.isEmpty()) {
            title = "Sin título";
        }
        return AuditEntityOptionResponse.builder()
                .id(p.getId())
                .label("#" + p.getId() + " — " + title)
                .build();
    }

    private static AuditEntityOptionResponse contractToOption(Contract c) {
        String title = c.getProperty() != null && c.getProperty().getTitle() != null
                ? c.getProperty().getTitle().trim()
                : "";
        if (title.isEmpty()) {
            title = "Propiedad";
        }
        String status = c.getStatus() != null ? c.getStatus().name() : "?";
        return AuditEntityOptionResponse.builder()
                .id(c.getId())
                .label("#" + c.getId() + " — " + title + " [" + status + "]")
                .build();
    }

    private AuditLogResponse toResponse(AuditLog row) {
        User u = row.getUser();
        return AuditLogResponse.builder()
                .id(row.getId())
                .createdAt(row.getCreatedAt())
                .userId(u != null ? u.getId() : null)
                .userEmail(u != null ? u.getEmail() : null)
                .entityType(row.getEntityType())
                .entityId(row.getEntityId())
                .action(row.getAction())
                .oldValues(row.getOldValues())
                .newValues(row.getNewValues())
                .ipAddress(row.getIpAddress())
                .userAgent(row.getUserAgent())
                .build();
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            ip = xff.split(",")[0].trim();
        }
        return ip;
    }
}
