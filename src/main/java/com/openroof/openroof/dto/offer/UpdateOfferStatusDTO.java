package com.openroof.openroof.dto.offer;

import com.openroof.openroof.model.enums.OfferStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOfferStatusDTO {
    @NotNull(message = "El estado es obligatorio")
    private OfferStatus status;

    private String rejectionReason;

    private String agentMessage;

    @jakarta.validation.constraints.Positive(message = "El monto de la contraoferta debe ser positivo")
    private BigDecimal counterOfferAmount;
}
