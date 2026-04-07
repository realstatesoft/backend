package com.openroof.openroof.dto.dashboard;

import java.math.BigDecimal;

public record StatItem(
    Object value,
    double trend
) {
    public static StatItem of(long value, double trend) {
        return new StatItem(value, trend);
    }

    public static StatItem of(BigDecimal value, double trend) {
        return new StatItem(value, trend);
    }
}
