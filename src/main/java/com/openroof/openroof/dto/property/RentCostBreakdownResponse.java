package com.openroof.openroof.dto.property;

import java.math.BigDecimal;

public record RentCostBreakdownResponse(
        Long propertyId,
        BigDecimal monthlyRent,
        int depositMonths,
        BigDecimal commissionPercent,
        BigDecimal deposit,
        BigDecimal firstMonth,
        BigDecimal commission,
        BigDecimal total
) {}