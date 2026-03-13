package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ContactMethod;
import com.openroof.openroof.model.enums.Priority;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateAgentClientRequest(

        @NotNull(message = "El ID del agente es obligatorio") Long agentId,

        @NotNull(message = "El ID del usuario/cliente es obligatorio") Long userId,

        ClientStatus status,

        Priority priority,

        List<String> tags,

        // Budget range
        BigDecimal minBudget,
        BigDecimal maxBudget,

        // Bedroom range
        Integer minBedrooms,
        Integer maxBedrooms,

        // Bathroom range
        Integer minBathrooms,
        Integer maxBathrooms,

        ContactMethod preferredContactMethod,

        String notes) {
}
