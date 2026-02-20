package com.openroof.openroof.common.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Value object para información de servicios/utilities de una propiedad.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityInfo {

    @Column(name = "water_connection", length = 50)
    private String waterConnection;

    @Column(name = "sanitary_installation", length = 50)
    private String sanitaryInstallation;

    @Column(name = "electricity_installation", length = 50)
    private String electricityInstallation;
}
