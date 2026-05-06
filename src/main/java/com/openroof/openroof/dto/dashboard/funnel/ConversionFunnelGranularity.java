package com.openroof.openroof.dto.dashboard.funnel;

/**
 * Granularidad para la serie temporal del embudo (PostgreSQL {@code date_trunc}).
 */
public enum ConversionFunnelGranularity {
    DAY,
    WEEK,
    MONTH
}
