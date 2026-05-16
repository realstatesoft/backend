package com.openroof.openroof.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record TenantPaymentsResponse(
    BigDecimal totalPaidYear,
    long installmentsOnTime,
    long installmentsLate,
    TenantDashboardResponse.NextInstallmentInfo nextPayment,
    List<TenantInstallmentItem> installments,
    long totalElements,
    int totalPages,
    int currentPage
) {}
