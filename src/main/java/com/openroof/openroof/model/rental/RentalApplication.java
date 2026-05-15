package com.openroof.openroof.model.rental;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.EmploymentStatus;
import com.openroof.openroof.model.enums.RentalApplicationStatus;
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
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "rental_applications", indexes = {
        @Index(name = "idx_rental_applications_property", columnList = "property_id"),
        @Index(name = "idx_rental_applications_applicant", columnList = "applicant_id"),
        @Index(name = "idx_rental_applications_status", columnList = "status")
})
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE rental_applications SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RentalApplicationStatus status;

    @Column(name = "desired_move_in_date")
    private LocalDate desiredMoveInDate;

    @Column(name = "monthly_income", precision = 12, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "income_to_rent_ratio", precision = 5, scale = 2)
    private BigDecimal incomeToRentRatio;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", length = 30)
    private EmploymentStatus employmentStatus;

    @Column(name = "employer_name", length = 255)
    private String employerName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_addresses", columnDefinition = "jsonb")
    private List<Map<String, Object>> previousAddresses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tenant_references", columnDefinition = "jsonb")
    private List<Map<String, Object>> tenantReferences;

    @Column(name = "has_pets")
    private Boolean hasPets;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "number_of_occupants")
    private Integer numberOfOccupants;

    @Column(name = "screening_consent")
    private Boolean screeningConsent;

    @Column(name = "screening_consent_at")
    private LocalDateTime screeningConsentAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
