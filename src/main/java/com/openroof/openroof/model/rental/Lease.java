package com.openroof.openroof.model.rental;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.DepositStatus;
import com.openroof.openroof.model.enums.LateFeeType;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "leases", indexes = {
        @Index(name = "idx_leases_property", columnList = "property_id"),
        @Index(name = "idx_leases_landlord", columnList = "landlord_id"),
        @Index(name = "idx_leases_primary_tenant", columnList = "primary_tenant_id"),
        @Index(name = "idx_leases_status", columnList = "status")
})
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE leases SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lease extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_tenant_id", nullable = false)
    private User primaryTenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaseStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "lease_type", nullable = false, length = 20)
    private LeaseType leaseType;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_frequency", length = 20)
    private BillingFrequency billingFrequency;

    @Column(name = "due_day")
    private Integer dueDay;

    @Column(name = "grace_period_days")
    private Integer gracePeriodDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "late_fee_type", length = 20)
    private LateFeeType lateFeeType;

    @Column(name = "late_fee_value", precision = 12, scale = 2)
    private BigDecimal lateFeeValue;

    @Column(name = "max_late_fee_cap", precision = 12, scale = 2)
    private BigDecimal maxLateFeeCap;

    @Column(name = "security_deposit", precision = 12, scale = 2)
    private BigDecimal securityDeposit;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", length = 20)
    private DepositStatus depositStatus;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(name = "auto_renew")
    private Boolean autoRenew;

    @Column(name = "renewal_notice_days")
    private Integer renewalNoticeDays;

    @Column(name = "parent_lease_id")
    private Long parentLeaseId;

    // e-signature fields (added via 045-add-esignature-to-leases.yaml)
    @Column(name = "signature_token_landlord")
    private UUID signatureTokenLandlord;

    @Column(name = "signature_token_tenant")
    private UUID signatureTokenTenant;

    @Column(name = "signature_token_expires_at")
    private LocalDateTime signatureTokenExpiresAt;

    @Column(name = "signed_by_landlord_at")
    private LocalDateTime signedByLandlordAt;

    @Column(name = "signed_by_tenant_at")
    private LocalDateTime signedByTenantAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "signature_audit_trail", columnDefinition = "jsonb")
    private Map<String, Object> signatureAuditTrail;

    public boolean isSigned() {
        return signedByLandlordAt != null && signedByTenantAt != null;
    }

    public boolean isActive() {
        return LeaseStatus.ACTIVE == status;
    }
}
