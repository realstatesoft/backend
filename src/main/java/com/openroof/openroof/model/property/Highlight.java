package com.openroof.openroof.model.property;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.payment.Payment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "highlights", indexes = {
        @Index(name = "idx_highlights_property", columnList = "property_id"),
        @Index(name = "idx_highlights_until", columnList = "highlighted_until")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Highlight extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "highlighted_from", nullable = false)
    private LocalDateTime highlightedFrom;

    @Column(name = "highlighted_until", nullable = false)
    private LocalDateTime highlightedUntil;
}
