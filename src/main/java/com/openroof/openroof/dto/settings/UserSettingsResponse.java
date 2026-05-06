package com.openroof.openroof.dto.settings;

import com.openroof.openroof.model.enums.NotifyChannel;

public record UserSettingsResponse(
        boolean notifyPriceDrop,
        boolean notifyNewMatch,
        boolean notifyMessages,
        NotifyChannel notifyChannel,
        boolean profileVisibleToAgents,
        boolean allowDirectContact
) {}
