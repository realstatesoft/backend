package com.openroof.openroof.model.maintenance;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.WorkOrderStatus;

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
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "work_orders", indexes = {
        @Index(name = "idx_work_orders_maintenance_request", columnList = "maintenance_request_id"),
        @Index(name = "idx_work_orders_vendor", columnList = "vendor_id")
})
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE work_orders SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_request_id", nullable = false)
    private MaintenanceRequest maintenanceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkOrderStatus status;

    @Column(name = "quoted_amount", precision = 12, scale = 2)
    private BigDecimal quotedAmount;

    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "invoice_url", length = 500)
    private String invoiceUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_photos", columnDefinition = "jsonb")
    private List<String> beforePhotos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_photos", columnDefinition = "jsonb")
    private List<String> afterPhotos;

    @Column(name = "warranty_days")
    private Integer warrantyDays;
}
