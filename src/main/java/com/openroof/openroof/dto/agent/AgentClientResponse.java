package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.MaritalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AgentClientResponse(
        Long id,

        // Agent
        Long agentId,
        String agentName,

        // User/Client
        Long userId,
        String userName,
        String userEmail,
        String userPhone,

        // Estado
        String status,
        String priority,

        // Tags
        List<String> tags,

        // Contadores
        Integer visitedPropertiesCount,
        Integer offersCount,

        // Rangos de preferencia
        BigDecimal minBudget,
        BigDecimal maxBudget,
        Integer minBedrooms,
        Integer maxBedrooms,
        Integer minBathrooms,
        Integer maxBathrooms,

        // Contacto
        String preferredContactMethod,
        LocalDateTime lastContactDate,

        // Notas
        String notes,

        // Perfil detallado
        LocalDate birthDate,
        MaritalStatus maritalStatus,
        String occupation,
        BigDecimal annualIncome,
        String address,
        String sourceChannel,
        Integer interactionsCount,
        List<String> preferredPropertyTypes,
        List<String> preferredAreas,
        List<String> desiredFeatures,

        Boolean isSearchingProperty,

        // Audit
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
