package com.openroof.openroof.model.property;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.AssignmentStatus;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "property_assignments", indexes = {
        @Index(name = "idx_pa_property", columnList = "property_id"),
        @Index(name = "idx_pa_agent", columnList = "agent_id"),
        @Index(name = "idx_pa_status", columnList = "status")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentProfile agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.PENDING;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;
}
