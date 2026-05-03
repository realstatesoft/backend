package com.openroof.openroof.model.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMetadata {

    // relacionado con Highlight Payment
    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "highlight_days")
    private Integer highlightDays;

    // agregar otros campos para otros tipos de pago si es necesario
}
