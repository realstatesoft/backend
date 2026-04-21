package com.openroof.openroof.dto.config;

import java.math.BigDecimal;

public record RentConfigResponse(
        int depositMonths,
        BigDecimal commissionPercent
) {}