package com.openroof.openroof.model.reservation;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.ReservationStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservations_property", columnList = "property_id"),
        @Index(name = "idx_reservations_buyer", columnList = "buyer_id"),
        @Index(name = "idx_reservations_status", columnList = "status"),
        @Index(name = "idx_reservations_expires", columnList = "expires_at")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(name = "reservation_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal reservationAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;
}
