package com.openroof.openroof.model.admin;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.AdminReviewStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_reviews", indexes = {
        @Index(name = "idx_admin_reviews_property", columnList = "property_id"),
        @Index(name = "idx_admin_reviews_reviewer", columnList = "reviewer_id"),
        @Index(name = "idx_admin_reviews_status", columnList = "status"),
        @Index(name = "idx_admin_reviews_date", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private AdminReviewStatus status = AdminReviewStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
