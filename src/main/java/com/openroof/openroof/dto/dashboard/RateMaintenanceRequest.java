package com.openroof.openroof.dto.dashboard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RateMaintenanceRequest(
    @NotNull @Min(1) @Max(5) Integer rating
) {}
