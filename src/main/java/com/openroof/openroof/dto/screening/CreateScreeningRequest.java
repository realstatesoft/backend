package com.openroof.openroof.dto.screening;

import jakarta.validation.constraints.NotNull;

public record CreateScreeningRequest(
        @NotNull Long applicationId
) {}
