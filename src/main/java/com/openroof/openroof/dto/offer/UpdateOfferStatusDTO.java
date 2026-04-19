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

    private BigDecimal counterOfferAmount;
}
