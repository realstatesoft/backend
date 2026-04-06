package com.openroof.openroof.dto.dashboard;

import java.math.BigDecimal;

public record RawSalesData(
        int year,
        int month,
        BigDecimal totalAmount,
        long count
) {
}
