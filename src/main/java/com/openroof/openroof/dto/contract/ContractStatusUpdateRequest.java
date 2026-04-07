package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;

public record ContractStatusUpdateRequest(
        @NotNull ContractStatus status
) {}
