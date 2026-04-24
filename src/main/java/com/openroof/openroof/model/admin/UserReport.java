package com.openroof.openroof.model.admin;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.ReportStatus;
import com.openroof.openroof.model.enums.UserReportReason;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "user_reports", indexes = {
        @Index(name = "idx_user_reports_reported_user", columnList = "reported_user_id"),
        @Index(name = "idx_user_reports_reporter_user", columnList = "reporter_user_id"),
        @Index(name = "idx_user_reports_status", columnList = "status")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporterUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserReportReason reason;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDIENTE;
}
