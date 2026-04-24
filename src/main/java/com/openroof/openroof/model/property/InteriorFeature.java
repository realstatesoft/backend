package com.openroof.openroof.model.property;

import com.openroof.openroof.model.enums.InteriorFeatureCategory;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "interior_features")
@SQLRestriction("deleted_at IS NULL")
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
