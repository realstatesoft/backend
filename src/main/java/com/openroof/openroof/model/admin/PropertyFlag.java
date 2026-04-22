package com.openroof.openroof.model.admin;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.FlagType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "property_flags", indexes = {
        @Index(name = "idx_property_flags_property", columnList = "property_id"),
        @Index(name = "idx_property_flags_reported_by", columnList = "reported_by_id"),
        @Index(name = "idx_property_flags_type", columnList = "flag_type"),
        @Index(name = "idx_property_flags_resolved", columnList = "resolved_at")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyFlag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private User reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 50)
    private FlagType flagType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    public boolean isResolved() {
        return resolvedAt != null;
    }
}
