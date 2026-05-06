package com.openroof.openroof.dto.settings;

import com.openroof.openroof.model.enums.NotifyChannel;
import jakarta.validation.constraints.*;

public record UpdateUserSettingsRequest(
        boolean notifyPriceDrop,
        boolean notifyNewMatch,
        boolean notifyMessages,
        @NotNull NotifyChannel notifyChannel,
        boolean profileVisibleToAgents,
        boolean allowDirectContact
) {}
