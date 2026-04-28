package com.openroof.openroof.dto.search;

import java.time.LocalDateTime;
import java.util.Map;

public record SearchPreferenceResponse(
    Long id,
    String name,
    Map<String, Object> filters,
    Boolean notificationsEnabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}