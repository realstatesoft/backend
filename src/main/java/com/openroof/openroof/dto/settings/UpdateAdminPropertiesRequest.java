package com.openroof.openroof.dto.settings;

import jakarta.validation.constraints.*;

public record UpdateAdminPropertiesRequest(
        @NotNull @Min(1) @Max(50)
        Integer maxImages
) {}
