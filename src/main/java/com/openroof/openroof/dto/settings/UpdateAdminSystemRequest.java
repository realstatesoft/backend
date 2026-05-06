package com.openroof.openroof.dto.settings;

import jakarta.validation.constraints.*;

public record UpdateAdminSystemRequest(
        @NotBlank @Size(min = 3, max = 3)
        String defaultCurrency
) {}
