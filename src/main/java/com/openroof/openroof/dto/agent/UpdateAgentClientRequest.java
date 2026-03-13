package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ContactMethod;
import com.openroof.openroof.model.enums.Priority;

import java.math.BigDecimal;
import java.util.List;

public record UpdateAgentClientRequest(

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
