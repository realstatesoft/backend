package com.openroof.openroof.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleItemResponse(
    Long id,
    String property,
    String buyer,
    String seller,
    String contractType,
    BigDecimal amount,
    BigDecimal totalCommission,
    BigDecimal myCommission,
    String myRole,
    LocalDate date,
    String status
) {}
