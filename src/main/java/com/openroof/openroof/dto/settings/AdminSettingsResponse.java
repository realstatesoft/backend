package com.openroof.openroof.dto.settings;

import java.math.BigDecimal;

public record AdminSettingsResponse(
        CommissionSettings commissions,
        ReservationSettings reservations,
        PropertySettings properties,
        SystemSettings system
) {
    public record CommissionSettings(
            BigDecimal saleCommissionPercent,
            BigDecimal rentCommissionPercent,
            int rentDepositMonths
    ) {}

    public record ReservationSettings(
            int ttlHours,
            BigDecimal depositPercent
    ) {}

    public record PropertySettings(
            int maxImages
    ) {}

    public record SystemSettings(
            String defaultCurrency
    ) {}
}
