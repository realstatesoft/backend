package com.openroof.openroof.model.property;

import com.openroof.openroof.common.AbstractReview;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "property_reviews", indexes = {
        @Index(name = "idx_property_reviews_property", columnList = "property_id"),
        @Index(name = "idx_property_reviews_rating", columnList = "rating")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyReview extends AbstractReview {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
}
