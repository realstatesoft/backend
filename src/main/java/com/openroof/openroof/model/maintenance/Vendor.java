package com.openroof.openroof.model.maintenance;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.MaintenanceCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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
import java.util.List;

@Entity
@Table(name = "vendors", indexes = {
        @Index(name = "idx_vendors_is_active", columnList = "is_active")
})
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE vendors SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor extends BaseEntity {

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "contact_name", length = 255)
    private String contactName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<MaintenanceCategory> specialties;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "total_jobs")
    private Integer totalJobs;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "insurance_expires_at")
    private LocalDate insuranceExpiresAt;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "service_areas", columnDefinition = "jsonb")
    private List<String> serviceAreas;
}
