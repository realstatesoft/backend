package com.openroof.openroof.model.search;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.AlertType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_user", columnList = "user_id"),
        @Index(name = "idx_alerts_search", columnList = "search_preference_id"),
        @Index(name = "idx_alerts_property", columnList = "property_id"),
        @Index(name = "idx_alerts_type", columnList = "alert_type")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_preference_id")
    private SearchPreference searchPreference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
