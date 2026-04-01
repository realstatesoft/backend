package com.openroof.openroof.dto.dashboard;

import java.math.BigDecimal;

public record YearData(
        BigDecimal amount,
        long count
) {
    public static YearData zero() {
        return new YearData(BigDecimal.ZERO, 0);
    }
}
