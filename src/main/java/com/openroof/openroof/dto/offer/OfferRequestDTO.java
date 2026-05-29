package com.openroof.openroof.dto.offer;

import com.openroof.openroof.validation.MaxDigits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferRequestDTO {
    @NotNull(message = "El ID de la propiedad es obligatorio")
    private Long propertyId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
    @MaxDigits(integer = 20)
    private BigDecimal amount;

    private String message;
}
