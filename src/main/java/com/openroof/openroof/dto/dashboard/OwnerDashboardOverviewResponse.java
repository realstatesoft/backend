package com.openroof.openroof.dto.dashboard;

import com.openroof.openroof.dto.property.PropertySummaryResponse;
import com.openroof.openroof.dto.contract.ContractSummaryResponse;
import com.openroof.openroof.dto.visit.VisitRequestResponse;
import java.util.List;

public record OwnerDashboardOverviewResponse(
    OwnerDashboardStatsResponse stats,
    List<PropertySummaryResponse> recentProperties,
    List<ContractSummaryResponse> urgentContracts,
    List<VisitRequestResponse> pendingVisits
) {}
