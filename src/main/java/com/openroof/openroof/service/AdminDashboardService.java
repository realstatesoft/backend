package com.openroof.openroof.service;

import com.openroof.openroof.dto.admin.*;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Panel admin: KPIs reales (usuarios, propiedades + subtítulos); transacciones/ingresos demo;
 * acciones rápidas y actividad mayormente demo salvo conteo de pendientes; atención real (3).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final Locale ES = Locale.forLanguageTag("es-PY");

    private static final AdminKpiDto STATIC_TRANSACTIONS = new AdminKpiDto(
            "189",
            "$2.1M",
            15.7,
            "+12 esta semana");

    private static final AdminKpiDto STATIC_REVENUE = new AdminKpiDto(
            "$89.450",
            "Este mes",
            22.1,
            "+$12.340 esta semana");

    private static final List<AdminActivityItemDto> STATIC_ACTIVITY = List.of(
            new AdminActivityItemDto(
                    "Nuevo usuario registrado",
                    "María González",
                    "30/09/2025 11:26",
                    "user"),
            new AdminActivityItemDto(
                    "Propiedad marcada por múltiples reportes",
                    "Casa ocupada",
                    "30/09/2025 11:20",
                    "flag"),
            new AdminActivityItemDto(
                    "Pago de suscripción Premium recibido",
                    "$100",
                    "30/09/2025 10:50",
                    "payment"));

    private static final int MAX_ATTENTION_ITEMS = 3;
    private static final List<PropertyStatus> ATTENTION_STATUSES = List.of(
            PropertyStatus.PENDING,
            PropertyStatus.APPROVED,
            PropertyStatus.REJECTED);

    private static final List<PropertyStatus> ACTIVE_PROPERTY_STATUSES = List.of(
            PropertyStatus.PUBLISHED,
            PropertyStatus.RENTED);

    private static final Map<PropertyStatus, Integer> STATUS_ORDER = new EnumMap<>(PropertyStatus.class);

    static {
        STATUS_ORDER.put(PropertyStatus.PENDING, 0);
        STATUS_ORDER.put(PropertyStatus.APPROVED, 1);
        STATUS_ORDER.put(PropertyStatus.REJECTED, 2);
    }

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    public AdminDashboardOverviewResponse getOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime twoWeeksAgo = now.minusDays(14);

        long userCount = userRepository.count();
        long propertyCount = propertyRepository.count();
        long pendingProperties = propertyRepository.countByStatus(PropertyStatus.PENDING);
        long activeUsers = userRepository.countActiveUsers(now);
        long activeProperties = propertyRepository.countByStatusIn(ACTIVE_PROPERTY_STATUSES);

        long usersThisWeek = userRepository.countCreatedBetween(weekAgo, now);
        long usersPrevWeek = userRepository.countCreatedBetween(twoWeeksAgo, weekAgo);
        long propsThisWeek = propertyRepository.countCreatedBetween(weekAgo, now);
        long propsPrevWeek = propertyRepository.countCreatedBetween(twoWeeksAgo, weekAgo);

        NumberFormat nf = NumberFormat.getIntegerInstance(ES);

        AdminKpiDto users = new AdminKpiDto(
                nf.format(userCount),
                nf.format(activeUsers) + " activos",
                computeTrendPercent(usersThisWeek, usersPrevWeek),
                formatPlusThisWeek(usersThisWeek, false));

        AdminKpiDto properties = new AdminKpiDto(
                nf.format(propertyCount),
                nf.format(activeProperties) + " activas",
                computeTrendPercent(propsThisWeek, propsPrevWeek),
                formatPlusThisWeek(propsThisWeek, true));

        List<AdminQuickActionDto> quickActions = List.of(
                new AdminQuickActionDto(
                        "Gestionar usuarios",
                        "pendientes",
                        "users",
                        null),
                new AdminQuickActionDto(
                        "Aprobar propiedades",
                        nf.format(pendingProperties) + " pendientes",
                        "check",
                        "/admin/approval"),
                new AdminQuickActionDto(
                        "Moderar contenido",
                        "23 marcados",
                        "warning",
                        null),
                new AdminQuickActionDto(
                        "Ver reportes",
                        "Análisis detallado",
                        "reports",
                        "/properties"),
                new AdminQuickActionDto(
                        "Gestión de pagos",
                        "Suscripciones y transacciones",
                        "card",
                        null),
                new AdminQuickActionDto(
                        "Configuración",
                        "Settings del sistema",
                        "settings",
                        "/profile"),
                new AdminQuickActionDto(
                        "Logs del sistema",
                        "Monitoreo y auditoría",
                        "document",
                        "/admin/audit-logs"));

        List<AdminAttentionItemDto> attentionItems = buildAttentionItems();

        return new AdminDashboardOverviewResponse(
                users,
                properties,
                STATIC_TRANSACTIONS,
                STATIC_REVENUE,
                quickActions,
                STATIC_ACTIVITY,
                attentionItems);
    }

    /**
     * @param feminine true → "nuevas", false → "nuevos"
     */
    private static String formatPlusThisWeek(long count, boolean feminine) {
        String unit = feminine ? "nuevas" : "nuevos";
        return "+" + count + " " + unit + " esta semana";
    }

    private static Double computeTrendPercent(long current, long previous) {
        if (current == 0 && previous == 0) {
            return null;
        }
        if (previous == 0) {
            return current > 0 ? 100.0 : null;
        }
        return ((current - previous) * 100.0) / previous;
    }

    private List<AdminAttentionItemDto> buildAttentionItems() {
        List<Property> raw = propertyRepository.findByDeletedAtIsNullAndTrashedAtIsNullAndStatusIn(ATTENTION_STATUSES);
        raw.sort(Comparator
                .comparing((Property p) -> STATUS_ORDER.getOrDefault(p.getStatus(), 99))
                .thenComparing(Property::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        List<AdminAttentionItemDto> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Property p : raw) {
            if (out.size() >= MAX_ATTENTION_ITEMS) {
                break;
            }
            out.add(toAttentionItem(p, today));
        }
        return out;
    }

    private AdminAttentionItemDto toAttentionItem(Property p, LocalDate today) {
        PropertyStatus st = p.getStatus();
        String title = p.getTitle() != null ? p.getTitle() : "Propiedad #" + p.getId();

        Long id = p.getId();
        LocalDateTime created = p.getCreatedAt();
        LocalDateTime updated = p.getUpdatedAt() != null ? p.getUpdatedAt() : created;

        long daysPending = created != null ? ChronoUnit.DAYS.between(created.toLocalDate(), today) : 0;
        long daysSinceUpdate = updated != null ? ChronoUnit.DAYS.between(updated.toLocalDate(), today) : 0;

        return switch (st) {
            case PENDING -> new AdminAttentionItemDto(
                    id,
                    "Pendiente",
                    title,
                    "Pendiente de aprobación desde hace " + formatDaysPhrase(daysPending),
                    daysPending > 7 ? "Urgente" : "Alta",
                    daysPending > 7 ? "urgent" : "high");
            case APPROVED -> new AdminAttentionItemDto(
                    id,
                    "Aprobada",
                    title,
                    "Aprobada, pendiente de publicación — hace " + formatDaysPhrase(daysSinceUpdate),
                    "Media",
                    "medium");
            case REJECTED -> new AdminAttentionItemDto(
                    id,
                    "Rechazada",
                    title,
                    "Rechazada hace " + formatDaysPhrase(daysSinceUpdate)
                            + ". Puede volver a enviarse a revisión.",
                    "Baja",
                    "low");
            default -> throw new IllegalStateException("Estado no esperado en atención admin: " + st);
        };
    }

    private static String formatDaysPhrase(long days) {
        if (days <= 0) {
            return "menos de un día";
        }
        if (days == 1) {
            return "1 día";
        }
        return days + " días";
    }
}
