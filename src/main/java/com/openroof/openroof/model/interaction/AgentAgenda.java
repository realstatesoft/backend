package com.openroof.openroof.model.interaction;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agent_agenda", indexes = {
        @Index(name = "idx_agent_agenda_agent", columnList = "agent_id"),
        @Index(name = "idx_agent_agenda_visit", columnList = "visit_id"),
        @Index(name = "idx_agent_agenda_timerange", columnList = "starts_at, ends_at")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentAgenda extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentProfile agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(length = 500)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
