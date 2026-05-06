package com.openroof.openroof.dto.settings;

import jakarta.validation.constraints.*;

public record UpdateAgentSettingsRequest(
        boolean autoAssignLeads,
        boolean notifyNewLead,
        boolean notifyVisitRequest,
        boolean notifyNewOffer,
        @Min(1) @Max(500) Integer workRadiusKm
) {}
