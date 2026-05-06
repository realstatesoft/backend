package com.openroof.openroof.model.config;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "agent_settings", indexes = {
        @Index(name = "idx_agent_settings_user", columnList = "user_id", unique = true)
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "auto_assign_leads", nullable = false)
    @Builder.Default
    private boolean autoAssignLeads = true;

    @Column(name = "notify_new_lead", nullable = false)
    @Builder.Default
    private boolean notifyNewLead = true;

    @Column(name = "notify_visit_request", nullable = false)
    @Builder.Default
    private boolean notifyVisitRequest = true;

    @Column(name = "notify_new_offer", nullable = false)
    @Builder.Default
    private boolean notifyNewOffer = true;

    @Column(name = "work_radius_km")
    private Integer workRadiusKm;
}
