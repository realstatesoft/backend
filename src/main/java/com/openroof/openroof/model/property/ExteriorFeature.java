package com.openroof.openroof.model.property;

import com.openroof.openroof.model.enums.ExteriorFeatureCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exterior_features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExteriorFeature extends AbstractFeature {

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ExteriorFeatureCategory category;
}
