package com.openroof.openroof.model.property;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.common.embeddable.RequestMetadata;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "property_views", indexes = {
        @Index(name = "idx_property_views_property", columnList = "property_id"),
        @Index(name = "idx_property_views_user", columnList = "user_id"),
        @Index(name = "idx_property_views_date", columnList = "created_at"),
        @Index(name = "idx_property_views_property_date", columnList = "property_id, created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_property_views_user_property", columnNames = {"user_id", "property_id"})
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyView extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Embedded
    private RequestMetadata requestMetadata;

    @Column(columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "session_id", length = 255)
    private String sessionId;
}
