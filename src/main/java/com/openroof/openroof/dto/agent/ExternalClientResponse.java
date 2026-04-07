package com.openroof.openroof.dto.agent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public record ExternalClientResponse(
        Long id,
        Long agentId,
        String name,
        String email,
        String phone,
        String status,
        String priority,
        String clientType,
        LocalDateTime lastContactDate,
        String origin,
        List<String> tags,
        String notes,
        BigDecimal minBudget,
        BigDecimal maxBudget,
        Integer minBedrooms,
        Integer maxBedrooms,
        Integer minBathrooms,
        Integer maxBathrooms,
        String maritalStatus,
        LocalDate birthDate,
        String occupation,
        BigDecimal annualIncome,
        String address,
        String sourceChannel,
        List<String> preferredPropertyTypes,
        List<String> preferredAreas,
        List<String> desiredFeatures,
        Boolean isSearchingProperty,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
