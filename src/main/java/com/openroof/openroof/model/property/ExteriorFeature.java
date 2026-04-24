package com.openroof.openroof.model.property;

import com.openroof.openroof.model.enums.ExteriorFeatureCategory;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "exterior_features")
@SQLRestriction("deleted_at IS NULL")
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
