package com.openroof.openroof.dto.dashboard;

import com.openroof.openroof.model.enums.MaintenanceCategory;
import com.openroof.openroof.model.enums.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateMaintenanceRequest(
    @NotNull Long leaseId,
    @NotBlank String title,
    @NotBlank String description,
    @NotNull MaintenanceCategory category,
    @NotNull MaintenancePriority priority,
    @Size(max = 10)
    List<@NotBlank @Size(max = 2048) @Pattern(regexp = "^(https?|ftp)://.+$") String> images,
    Boolean permissionToEnter
) {}
