package com.openroof.openroof.model.lead;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.InteractionType;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "lead_interactions", indexes = {
        @Index(name = "idx_lead_interactions_lead", columnList = "lead_id"),
        @Index(name = "idx_lead_interactions_performed_by", columnList = "performed_by_id"),
        @Index(name = "idx_lead_interactions_type", columnList = "type"),
        @Index(name = "idx_lead_interactions_date", columnList = "created_at")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadInteraction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InteractionType type;

    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_status_id")
    private LeadStatus oldStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_status_id")
    private LeadStatus newStatus;
}
