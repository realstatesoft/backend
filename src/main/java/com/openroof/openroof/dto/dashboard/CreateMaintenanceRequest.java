package com.openroof.openroof.dto.dashboard;

import com.openroof.openroof.model.enums.MaintenanceCategory;
import com.openroof.openroof.model.enums.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateMaintenanceRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull MaintenanceCategory category,
    @NotNull MaintenancePriority priority,
    List<String> images,
    Boolean permissionToEnter
) {}
