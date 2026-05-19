package com.openroof.openroof.dto.dashboard;

import java.time.LocalDateTime;
import java.util.List;

public record TenantMaintenanceTicketItem(
    Long id,
    String title,
    String description,
    String category,
    String priority,
    String status,
    List<String> images,
    VendorContact vendor,
    List<StatusHistoryItem> statusHistory,
    Integer rating,
    LocalDateTime createdAt
) {
    public record VendorContact(
        String name,
        String phone,
        String email
    ) {}

    public record StatusHistoryItem(
        String status,
        LocalDateTime timestamp
    ) {}
}
