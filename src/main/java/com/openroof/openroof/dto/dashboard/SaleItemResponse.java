package com.openroof.openroof.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleItemResponse(
    Long id,
    String property,
    String client,
    BigDecimal amount,
    BigDecimal commission,
    LocalDate date,
    String status
) {}
