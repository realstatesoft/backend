package com.openroof.openroof.service;

import com.openroof.openroof.dto.dashboard.funnel.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Métricas de embudo para el panel del agente (PostgreSQL {@code date_trunc}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConversionFunnelService {

    private static final int MAX_RANGE_DAYS = 731;

    /**
     * Alineado con {@link com.openroof.openroof.repository.PropertyRepository#findByAgentScope}
     * más propiedades del usuario dueño = usuario del perfil agente (listadas en el panel aunque {@code agent_id} sea null).
     */
    private static final String SQL_PROPERTY_IN_AGENT_SCOPE = """
            AND (
                p.agent_id = :agentId
                OR p.owner_id IN (
                    SELECT ac.user_id FROM agent_clients ac
                    WHERE ac.agent_id = :agentId AND ac.deleted_at IS NULL
                )
                OR p.owner_id = (
                    SELECT ap.user_id FROM agent_profiles ap
                    WHERE ap.id = :agentId AND ap.deleted_at IS NULL
                )
            )
            """;

    private final NamedParameterJdbcTemplate namedJdbc;
    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;

    public ConversionFunnelSummaryResponse getSummary(
            String agentEmail,
            LocalDate from,
            LocalDate to,
            ConversionFunnelGranularity granularity,
            boolean comparePrevious,
            Long locationId,
            String propertyType,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        validateDates(from, to);
        long agentId = resolveAgentProfileId(agentEmail);
        PeriodWindow current = PeriodWindow.of(from, to);
        PeriodWindow previous = comparePrevious ? current.previousSibling() : null;

        FunnelBind curBind = funnelBind(agentId, current.start(), current.endExclusive(), locationId, propertyType, minPrice, maxPrice);

        ConversionFunnelStagesResponse currStages = aggregateStages(curBind);
        FunnelBind prevBind = previous != null
                ? funnelBind(agentId, previous.start(), previous.endExclusive(), locationId, propertyType, minPrice, maxPrice)
                : null;
        ConversionFunnelStagesResponse prevStages = prevBind != null ? aggregateStages(prevBind) : null;
        ConversionFunnelKpisResponse kpis = buildKpis(
                currStages,
                prevStages,
                curBind,
                prevBind
        );

        String truncLit = truncationLiteral(granularity);
        List<ConversionFunnelSeriesPointResponse> series = mergeSeries(granularity, loadBuckets(curBind, truncLit));

        return new ConversionFunnelSummaryResponse(
                currStages,
                computeRates(currStages),
                prevStages,
                prevStages != null ? computeRates(prevStages) : null,
                kpis,
                series);
    }

    public PropertyFunnelPageResponse getTopProperties(
            String agentEmail,
            LocalDate from,
            LocalDate to,
            int page,
            int size,
            Long locationId,
            String propertyType,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        validateDates(from, to);
        if (page < 0 || size < 1 || size > 50) {
            throw new BadRequestException("Parámetros de paginación inválidos (size 1–50)");
        }
        long agentId = resolveAgentProfileId(agentEmail);
        PeriodWindow window = PeriodWindow.of(from, to);

        FunnelBind fb = funnelBind(agentId, window.start(), window.endExclusive(), locationId, propertyType, minPrice, maxPrice);
        MapSqlParameterSource bp = copyNamedParams(fb.params());
        bp.addValue("lim", size);
        bp.addValue("off", page * (long) size);

        Number totalObj = namedJdbc.queryForObject(
                """
                        SELECT COUNT(*) FROM properties p
                        WHERE p.deleted_at IS NULL AND p.trashed_at IS NULL
                        """ + SQL_PROPERTY_IN_AGENT_SCOPE + fb.propertyPredicatesTail(),
                bp,
                Number.class
        );
        long total = totalObj != null ? totalObj.longValue() : 0L;

        String listSql = """
                SELECT p.id AS pid, p.title, p.address, CAST(p.status AS text) AS st, p.price,
                  (SELECT COUNT(*) FROM property_views pv
                   WHERE pv.property_id = p.id AND pv.deleted_at IS NULL
                     AND pv.created_at >= :start AND pv.created_at < :endExclusive) AS vc,
                  (SELECT COUNT(*) FROM visit_requests vr
                   WHERE vr.property_id = p.id AND vr.deleted_at IS NULL
                     AND vr.created_at >= :start AND vr.created_at < :endExclusive) AS vic,
                  (SELECT COUNT(*) FROM offers o
                   WHERE o.property_id = p.id AND o.deleted_at IS NULL
                     AND o.created_at >= :start AND o.created_at < :endExclusive) AS oc,
                  (SELECT COUNT(*) FROM contracts c
                   WHERE c.property_id = p.id AND c.deleted_at IS NULL
                     AND c.status = 'SIGNED'
                     AND c.updated_at >= :start AND c.updated_at < :endExclusive) AS sc
                FROM properties p
                WHERE p.deleted_at IS NULL AND p.trashed_at IS NULL
                """ + SQL_PROPERTY_IN_AGENT_SCOPE + fb.propertyPredicatesTail() + """
                 ORDER BY sc DESC, oc DESC, vc DESC, p.id DESC
                LIMIT :lim OFFSET :off
                """;

        List<PropertyFunnelRowResponse> rows = namedJdbc.query(listSql, bp, (rs, rowNum) -> mapPropertyRow(rs));

        return new PropertyFunnelPageResponse(rows, total, page, size);
    }

    private PropertyFunnelRowResponse mapPropertyRow(ResultSet rs) throws java.sql.SQLException {
        long views = rs.getLong("vc");
        long visits = rs.getLong("vic");
        long offers = rs.getLong("oc");
        long sales = rs.getLong("sc");
        Double conv = views > 0 ? round2(100.0 * sales / views) : (sales > 0 ? 100.0 : null);
        return new PropertyFunnelRowResponse(
                rs.getLong("pid"),
                rs.getString("title"),
                rs.getString("address"),
                rs.getString("st"),
                rs.getBigDecimal("price"),
                views,
                visits,
                offers,
                sales,
                conv
        );
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Las fechas from y to son obligatorias");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("La fecha final debe ser posterior o igual a la inicial");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BadRequestException("El rango máximo permitido es " + MAX_RANGE_DAYS + " días");
        }
    }

    private long resolveAgentProfileId(String email) {
        var user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return agentProfileRepository.findByUser_Id(user.getId())
                .map(agent -> agent.getId())
                .orElseThrow(() -> new BadRequestException("El usuario no es un agente con perfil registrado"));
    }

    /**
     * Parámetros nombrados y fragmento SQL de filtros sobre {@code properties} como {@code p}.
     * Evita {@code (... IS NULL OR p.col = :param)}, que provoca errores de tipos en PostgreSQL.
     */
    private record FunnelBind(MapSqlParameterSource params, String propertyPredicatesTail) {}

    private FunnelBind funnelBind(
            long agentId,
            LocalDateTime start,
            LocalDateTime endExclusive,
            Long locationId,
            String propertyType,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        String cleanedType = normalizePropertyType(propertyType);
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("agentId", agentId)
                .addValue("start", java.sql.Timestamp.valueOf(start))
                .addValue("endExclusive", java.sql.Timestamp.valueOf(endExclusive));

        StringBuilder tail = new StringBuilder();
        if (locationId != null) {
            tail.append(" AND p.location_id = :locationId");
            p.addValue("locationId", locationId);
        }
        if (cleanedType != null) {
            tail.append(" AND p.property_type = :propertyTypeClean");
            p.addValue("propertyTypeClean", cleanedType);
        }
        if (minPrice != null) {
            tail.append(" AND p.price >= :minPrice");
            p.addValue("minPrice", minPrice);
        }
        if (maxPrice != null) {
            tail.append(" AND p.price <= :maxPrice");
            p.addValue("maxPrice", maxPrice);
        }
        return new FunnelBind(p, tail.toString());
    }

    private static MapSqlParameterSource copyNamedParams(MapSqlParameterSource src) {
        MapSqlParameterSource dst = new MapSqlParameterSource();
        for (String name : src.getParameterNames()) {
            dst.addValue(name, src.getValue(name));
        }
        return dst;
    }

    private static String normalizePropertyType(String propertyType) {
        if (propertyType == null || propertyType.isBlank()) {
            return null;
        }
        return propertyType.trim();
    }

    private ConversionFunnelStagesResponse aggregateStages(FunnelBind b) {

        String tail = b.propertyPredicatesTail();
        MapSqlParameterSource p = b.params();

        String viewsSql = """
                SELECT COUNT(*) FROM property_views pv
                INNER JOIN properties p ON p.id = pv.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                WHERE pv.deleted_at IS NULL
                """ + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND pv.created_at >= :start AND pv.created_at < :endExclusive
                """ + tail;

        String visitsSql = """
                SELECT COUNT(*) FROM visit_requests vr
                INNER JOIN properties p ON p.id = vr.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                WHERE vr.deleted_at IS NULL
                """ + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND vr.created_at >= :start AND vr.created_at < :endExclusive
                """ + tail;

        String offersSql = """
                SELECT COUNT(*) FROM offers o
                INNER JOIN properties p ON p.id = o.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                WHERE o.deleted_at IS NULL
                """ + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND o.created_at >= :start AND o.created_at < :endExclusive
                """ + tail;

        String salesSql = """
                SELECT COUNT(*) FROM contracts c
                INNER JOIN properties p ON p.id = c.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                WHERE c.deleted_at IS NULL AND c.status = 'SIGNED'
                  AND c.updated_at >= :start AND c.updated_at < :endExclusive
                """ + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND (c.listing_agent_id = :agentId OR c.listing_agent_id IS NULL)
                """ + tail;

        return new ConversionFunnelStagesResponse(
                countScalar(viewsSql, p),
                countScalar(visitsSql, p),
                countScalar(offersSql, p),
                countScalar(salesSql, p)
        );
    }

    private long countScalar(String sql, MapSqlParameterSource p) {
        Number n = namedJdbc.queryForObject(sql, p, Number.class);
        return n != null ? n.longValue() : 0L;
    }

    private ConversionFunnelRatesResponse computeRates(ConversionFunnelStagesResponse x) {
        return new ConversionFunnelRatesResponse(
                pct(x.visits(), x.views()),
                pct(x.offers(), x.visits()),
                pct(x.sales(), x.offers()),
                pct(x.sales(), x.views())
        );
    }

    private Double pct(long num, long den) {
        if (den <= 0) {
            return null;
        }
        return round2(100.0 * num / den);
    }

    private static Double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private ConversionFunnelKpisResponse buildKpis(
            ConversionFunnelStagesResponse cur,
            ConversionFunnelStagesResponse prevStages,
            FunnelBind curBind,
            FunnelBind prevBind
    ) {
        Double globalCur = pct(cur.sales(), cur.views());

        Double globalDelta = null;
        Double medianDelta = null;
        Double volumePctChange = null;

        Double medianDays = medianDaysFirstViewToSigned(curBind);

        BigDecimal volume = signedVolume(curBind);

        if (prevStages != null && prevBind != null) {
            Double globalPrev = pct(prevStages.sales(), prevStages.views());
            if (globalCur != null && globalPrev != null) {
                globalDelta = round2(globalCur - globalPrev);
            }
            Double medianPrev = medianDaysFirstViewToSigned(prevBind);
            if (medianDays != null && medianPrev != null) {
                medianDelta = round2(medianDays - medianPrev);
            }
            volumePctChange = pctChange(volume, signedVolume(prevBind));
        }

        return new ConversionFunnelKpisResponse(globalCur, medianDays, volume, globalDelta, medianDelta, volumePctChange);
    }

    private Double pctChange(BigDecimal cur, BigDecimal prev) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) {
            return cur != null && cur.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : null;
        }
        BigDecimal safeCur = cur != null ? cur : BigDecimal.ZERO;
        return round2(safeCur.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue());
    }

    private BigDecimal signedVolume(FunnelBind b) {
        String tail = b.propertyPredicatesTail();
        String sql = """
                SELECT COALESCE(SUM(c.amount), 0) FROM contracts c
                INNER JOIN properties p ON p.id = c.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                WHERE c.deleted_at IS NULL AND c.status = 'SIGNED'
                  AND c.updated_at >= :start AND c.updated_at < :endExclusive
                """ + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND (c.listing_agent_id = :agentId OR c.listing_agent_id IS NULL)
                """ + tail;
        BigDecimal amt = namedJdbc.queryForObject(sql, b.params(), BigDecimal.class);
        return amt != null ? amt : BigDecimal.ZERO;
    }

    private Double medianDaysFirstViewToSigned(FunnelBind b) {
        String tail = b.propertyPredicatesTail();
        String sql = """
                SELECT percentile_disc(0.5) WITHIN GROUP (
                    ORDER BY EXTRACT(epoch FROM (c.updated_at - mv.first_view)) / 86400.0
                )
                FROM contracts c
                INNER JOIN properties p ON p.id = c.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                INNER JOIN (
                    SELECT pv.property_id AS pid, MIN(pv.created_at) AS first_view
                    FROM property_views pv
                    WHERE pv.deleted_at IS NULL
                    GROUP BY pv.property_id
                ) mv ON mv.pid = c.property_id
                WHERE c.deleted_at IS NULL AND c.status = 'SIGNED'
                  AND c.updated_at >= :start AND c.updated_at < :endExclusive
                """ + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND (c.listing_agent_id = :agentId OR c.listing_agent_id IS NULL)
                  AND mv.first_view <= c.updated_at
                """ + tail;
        try {
            Double med = namedJdbc.query(sql, b.params(), rs -> {
                if (rs.next()) {
                    if (rs.getObject(1) == null) {
                        return null;
                    }
                    return rs.getDouble(1);
                }
                return null;
            });
            return med != null ? round2(med) : null;
        } catch (Exception e) {
            log.debug("medianDaysFirstViewToSigned: {}", e.getMessage());
            return null;
        }
    }

    private String truncationLiteral(ConversionFunnelGranularity granularity) {
        return switch (granularity) {
            case DAY -> "day";
            case WEEK -> "week";
            case MONTH -> "month";
        };
    }

    private Map<Metric, Map<LocalDate, Long>> loadBuckets(FunnelBind b, String truncLit) {
        String tail = b.propertyPredicatesTail();
        MapSqlParameterSource p = b.params();

        String views = """
                SELECT date_trunc('%s', pv.created_at) AS b, COUNT(*)::bigint AS cnt
                FROM property_views pv
                INNER JOIN properties p ON p.id = pv.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                WHERE pv.deleted_at IS NULL
                """.formatted(truncLit) + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND pv.created_at >= :start AND pv.created_at < :endExclusive
                """ + tail + """
                 GROUP BY 1 ORDER BY 1
                """;

        String visits = """
                SELECT date_trunc('%s', vr.created_at) AS b, COUNT(*)::bigint AS cnt
                FROM visit_requests vr
                INNER JOIN properties p ON p.id = vr.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                WHERE vr.deleted_at IS NULL
                """.formatted(truncLit) + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND vr.created_at >= :start AND vr.created_at < :endExclusive
                """ + tail + """
                 GROUP BY 1 ORDER BY 1
                """;

        String offers = """
                SELECT date_trunc('%s', o.created_at) AS b, COUNT(*)::bigint AS cnt
                FROM offers o
                INNER JOIN properties p ON p.id = o.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                WHERE o.deleted_at IS NULL
                """.formatted(truncLit) + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND o.created_at >= :start AND o.created_at < :endExclusive
                """ + tail + """
                 GROUP BY 1 ORDER BY 1
                """;

        String sales = """
                SELECT date_trunc('%s', c.updated_at) AS b, COUNT(*)::bigint AS cnt
                FROM contracts c
                INNER JOIN properties p ON p.id = c.property_id AND p.deleted_at IS NULL AND p.trashed_at IS NULL
                WHERE c.deleted_at IS NULL AND c.status = 'SIGNED'
                  AND c.updated_at >= :start AND c.updated_at < :endExclusive
                """.formatted(truncLit) + SQL_PROPERTY_IN_AGENT_SCOPE + """
                  AND (c.listing_agent_id = :agentId OR c.listing_agent_id IS NULL)
                """ + tail + """
                 GROUP BY 1 ORDER BY 1
                """;

        Map<Metric, Map<LocalDate, Long>> out = new EnumMap<>(Metric.class);
        out.put(Metric.VIEWS, bucketMap(views, p));
        out.put(Metric.VISITS, bucketMap(visits, p));
        out.put(Metric.OFFERS, bucketMap(offers, p));
        out.put(Metric.SALES, bucketMap(sales, p));
        return out;
    }

    private enum Metric {
        VIEWS, VISITS, OFFERS, SALES
    }

    private Map<LocalDate, Long> bucketMap(String sql, MapSqlParameterSource p) {
        ResultSetExtractor<Map<LocalDate, Long>> ex = rs -> {
            Map<LocalDate, Long> m = new LinkedHashMap<>();
            while (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("b");
                LocalDate ld = ts != null ? ts.toLocalDateTime().toLocalDate() : null;
                if (ld != null) {
                    m.put(ld, rs.getLong("cnt"));
                }
            }
            return m;
        };
        return namedJdbc.query(sql, p, ex);
    }

    private List<ConversionFunnelSeriesPointResponse> mergeSeries(
            ConversionFunnelGranularity granularity,
            Map<Metric, Map<LocalDate, Long>> buckets
    ) {
        Set<LocalDate> keys = new TreeSet<>();
        buckets.values().forEach(m -> keys.addAll(m.keySet()));

        List<ConversionFunnelSeriesPointResponse> list = new ArrayList<>();
        for (LocalDate d : keys) {
            long v = buckets.get(Metric.VIEWS).getOrDefault(d, 0L);
            long vi = buckets.get(Metric.VISITS).getOrDefault(d, 0L);
            long o = buckets.get(Metric.OFFERS).getOrDefault(d, 0L);
            long s = buckets.get(Metric.SALES).getOrDefault(d, 0L);
            list.add(new ConversionFunnelSeriesPointResponse(labelFor(granularity, d), d, v, vi, o, s));
        }
        return list;
    }

    private String labelFor(ConversionFunnelGranularity gran, LocalDate d) {
        return switch (gran) {
            case DAY -> d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case WEEK -> d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case MONTH -> d.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        };
    }

    private record PeriodWindow(LocalDateTime start, LocalDateTime endExclusive, LocalDate from, LocalDate to) {
        static PeriodWindow of(LocalDate from, LocalDate to) {
            return new PeriodWindow(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), from, to);
        }

        PeriodWindow previousSibling() {
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            LocalDate prevTo = from.minusDays(1);
            LocalDate prevFrom = prevTo.minusDays(days - 1);
            return of(prevFrom, prevTo);
        }
    }
}
