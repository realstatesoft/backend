package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ClientType;
import com.openroof.openroof.model.enums.Priority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateExternalClientRequest(
        @NotNull Long agentId,
        @NotBlank String name,
        @Email String email,
        String phone,
        ClientStatus status,
        Priority priority,
        ClientType clientType,
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
        Boolean isSearchingProperty,
        List<String> tags
) {
}
