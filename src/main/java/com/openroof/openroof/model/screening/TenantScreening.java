package com.openroof.openroof.model.screening;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.model.rental.RentalApplication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "tenant_screenings")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE tenant_screenings SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantScreening extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private RentalApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScreeningProvider provider;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "credit_report_url", length = 500)
    private String creditReportUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "background_check_status", length = 20)
    private BackgroundCheckStatus backgroundCheckStatus;

    @Column(name = "background_report_url", length = 500)
    private String backgroundReportUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eviction_history", columnDefinition = "jsonb")
    private List<Map<String, Object>> evictionHistory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criminal_records", columnDefinition = "jsonb")
    private List<Map<String, Object>> criminalRecords;

    @Column(name = "income_verified")
    private Boolean incomeVerified;

    @Column(name = "identity_verified")
    private Boolean identityVerified;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ScreeningRecommendation recommendation;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "run_at")
    private LocalDateTime runAt;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public ScreeningRecommendation autoRecommend() {
        if (backgroundCheckStatus == BackgroundCheckStatus.FAILED) {
            return ScreeningRecommendation.REJECT;
        }
        if (backgroundCheckStatus == BackgroundCheckStatus.FLAGGED
                || (creditScore != null && creditScore < 580)) {
            return ScreeningRecommendation.REVIEW;
        }
        return ScreeningRecommendation.APPROVE;
    }
}
