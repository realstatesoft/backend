package com.openroof.openroof.model.interaction;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.VisitRequestStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "visit_requests", indexes = {
        @Index(name = "idx_vr_property", columnList = "property_id"),
        @Index(name = "idx_vr_buyer", columnList = "buyer_id"),
        @Index(name = "idx_vr_agent", columnList = "agent_id"),
        @Index(name = "idx_vr_status", columnList = "status")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentProfile agent;

    @Column(name = "proposed_at", nullable = false)
    private LocalDateTime proposedAt;

    @Column(name = "counter_proposed_at")
    private LocalDateTime counterProposedAt;

    @Column(name = "counter_propose_message", columnDefinition = "TEXT")
    private String counterProposeMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VisitRequestStatus status = VisitRequestStatus.PENDING;

    @Column(name = "buyer_name", length = 100)
    private String buyerName;

    @Column(name = "buyer_email", length = 255)
    private String buyerEmail;

    @Column(name = "buyer_phone", length = 20)
    private String buyerPhone;

    @Column(columnDefinition = "TEXT")
    private String message;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;
}
