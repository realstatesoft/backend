package com.openroof.openroof.dto.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record SearchPreferenceRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,

    @NotNull(message = "Filters are required")
    Map<String, Object> filters) {}