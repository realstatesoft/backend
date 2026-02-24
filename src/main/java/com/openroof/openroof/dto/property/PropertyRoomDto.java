package com.openroof.openroof.dto.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PropertyRoomDto(

        @NotBlank(message = "El nombre de la habitación es obligatorio") @Size(max = 100, message = "El nombre no puede exceder 100 caracteres") String name,

        BigDecimal area,

        List<Long> interiorFeatureIds) {
}
