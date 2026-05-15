package com.openroof.openroof.dto.dashboard;

import java.util.List;
import java.util.Map;

public record TenantMaintenanceResponse(
    List<TenantMaintenanceTicketItem> tickets,
    Map<String, Long> countsByStatus
) {}
