package com.openroof.openroof.dto.admin;

public record AdminAttentionItemDto(
        Long propertyId,
        String statusLabel,
        String title,
        String description,
        String priorityLabel,
        String priorityKey) {
}
