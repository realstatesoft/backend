package com.openroof.openroof.model.agent;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.InteractionSource;
import com.openroof.openroof.model.enums.InteractionType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_interactions", indexes = {
        @Index(name = "idx_interactions_agent", columnList = "agent_id"),
        @Index(name = "idx_interactions_client", columnList = "agent_client_id"),
        @Index(name = "idx_interactions_external", columnList = "external_client_id"),
        @Index(name = "idx_interactions_type", columnList = "type"),
        @Index(name = "idx_interactions_date", columnList = "created_at"),
        @Index(name = "idx_interactions_occurred_at", columnList = "occurred_at")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientInteraction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentProfile agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_client_id")
    private AgentClient agentClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_client_id")
    private ExternalClient externalClient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InteractionType type;

    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(length = 100)
    private String outcome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InteractionSource source = InteractionSource.MANUAL;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
