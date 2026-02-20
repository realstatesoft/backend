package com.openroof.openroof.common.embeddable;

import com.openroof.openroof.model.enums.ConstructionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

/**
 * Value object para detalles de construcción de una propiedad.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstructionDetails {

    @Column(name = "construction_year")
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "construction_status", length = 50)
    private ConstructionStatus status;

    @Column(name = "structure_material", length = 100)
    private String structureMaterial;

    @Column(name = "walls_material", length = 100)
    private String wallsMaterial;

    @Column(name = "floor_material", length = 100)
    private String floorMaterial;

    @Column(name = "roof_material", length = 100)
    private String roofMaterial;
}
