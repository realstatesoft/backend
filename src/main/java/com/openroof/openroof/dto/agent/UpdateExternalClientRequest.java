package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ClientType;
import com.openroof.openroof.model.enums.Priority;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateExternalClientRequest(
        String name,
        String email,
        String phone,
        ClientStatus status,
        Priority priority,
        ClientType clientType,
        LocalDateTime lastContactDate,
        String origin,
        List<String> tags,
        String notes,
        java.math.BigDecimal minBudget,
        java.math.BigDecimal maxBudget,
        Integer minBedrooms,
        Integer maxBedrooms,
        Integer minBathrooms,
        Integer maxBathrooms,
        String maritalStatus,
        java.time.LocalDate birthDate,
        String occupation,
        java.math.BigDecimal annualIncome,
        String address,
        String sourceChannel,
        List<String> preferredPropertyTypes,
        List<String> preferredAreas,
        List<String> desiredFeatures,
        Boolean isSearchingProperty
) {
}
