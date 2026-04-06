package com.openroof.openroof.dto.dashboard;

import java.math.BigDecimal;

public record MoneyStatItem(BigDecimal value, double trend) {
    public static MoneyStatItem of(BigDecimal value, double trend) {
        return new MoneyStatItem(value, trend);
    }
}
