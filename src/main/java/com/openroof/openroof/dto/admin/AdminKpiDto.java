package com.openroof.openroof.dto.admin;

/**
 * KPI para el panel de administración. {@code valueLabel} es el texto principal;
 * {@code subtitle} es opcional (ej. “692 activos”, “Este mes”).
 */
public record AdminKpiDto(
        String valueLabel,
        String subtitle,
        Double trendPercent,
        String weekHint) {
}
