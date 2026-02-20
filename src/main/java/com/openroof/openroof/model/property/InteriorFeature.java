package com.openroof.openroof.model.property;

import com.openroof.openroof.model.enums.InteriorFeatureCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interior_features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorFeature extends AbstractFeature {

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private InteriorFeatureCategory category;
}
