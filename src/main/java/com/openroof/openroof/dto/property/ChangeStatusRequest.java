package com.openroof.openroof.dto.property;

import com.openroof.openroof.model.enums.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(

        @NotNull(message = "El nuevo estado es obligatorio") PropertyStatus newStatus) {
}
