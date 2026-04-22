package com.openroof.openroof.model.property;

import com.openroof.openroof.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "property_similar", indexes = {
        @Index(name = "idx_property_similar_property", columnList = "property_id"),
        @Index(name = "idx_property_similar_score", columnList = "property_id, similarity_score")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertySimilar extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "similar_property_id", nullable = false)
    private Property similarProperty;

    @Column(name = "similarity_score", precision = 5, scale = 4)
    private BigDecimal similarityScore;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
}
