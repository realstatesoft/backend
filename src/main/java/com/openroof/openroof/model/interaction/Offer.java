package com.openroof.openroof.model.interaction;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.OfferStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "offers", indexes = {
        @Index(name = "idx_offers_property", columnList = "property_id"),
        @Index(name = "idx_offers_buyer", columnList = "buyer_id"),
        @Index(name = "idx_offers_status", columnList = "status"),
        @Index(name = "idx_offers_created", columnList = "created_at")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private OfferStatus status = OfferStatus.SENT;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "counter_offer_amount", precision = 12, scale = 2)
    private BigDecimal counterOfferAmount;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
