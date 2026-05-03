package com.openroof.openroof.dto.settings;

public record AgentSettingsResponse(
        boolean autoAssignLeads,
        boolean notifyNewLead,
        boolean notifyVisitRequest,
        boolean notifyNewOffer,
        Integer workRadiusKm
) {}
