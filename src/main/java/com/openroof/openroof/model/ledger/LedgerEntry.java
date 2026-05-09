package com.openroof.openroof.model.ledger;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.LedgerEntryCategory;
import com.openroof.openroof.model.enums.LedgerEntryType;
import com.openroof.openroof.model.maintenance.WorkOrder;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.LeasePayment;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

// Accounting records — immutable by convention. No soft delete.
// @SQLRestriction is intentionally omitted; deletedAt will never be set.
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_entries_property_date", columnList = "property_id, date"),
        @Index(name = "idx_ledger_entries_lease_type_date", columnList = "lease_id, type, date")
})
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE ledger_entries SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerEntryType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LedgerEntryCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_payment_id")
    private LeasePayment relatedPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_work_order_id")
    private WorkOrder relatedWorkOrder;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "is_tax_deductible")
    private Boolean isTaxDeductible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public static LedgerEntry fromPayment(LeasePayment payment) {
        return LedgerEntry.builder()
                .lease(payment.getLease())
                .property(payment.getLease().getProperty())
                .date(payment.getPaidAt() != null ? payment.getPaidAt().toLocalDate() : LocalDate.now())
                .type(LedgerEntryType.CREDIT)
                .category(LedgerEntryCategory.RENT)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .description("Payment: " + payment.getReceiptNumber())
                .relatedPayment(payment)
                .isTaxDeductible(false)
                .build();
    }

    public static LedgerEntry fromWorkOrder(WorkOrder workOrder) {
        Lease lease = workOrder.getMaintenanceRequest().getLease();
        if (lease == null) {
            throw new IllegalArgumentException("lease is required for LedgerEntry from WorkOrder id=" + workOrder.getId());
        }

        BigDecimal amount = workOrder.getFinalAmount() != null
                ? workOrder.getFinalAmount()
                : workOrder.getQuotedAmount();
        if (amount == null) {
            throw new IllegalArgumentException("amount is required for LedgerEntry from WorkOrder id=" + workOrder.getId());
        }

        return LedgerEntry.builder()
                .lease(lease)
                .property(workOrder.getMaintenanceRequest().getProperty())
                .date(workOrder.getCompletedDate() != null
                        ? workOrder.getCompletedDate().toLocalDate()
                        : LocalDate.now())
                .type(LedgerEntryType.DEBIT)
                .category(LedgerEntryCategory.MAINTENANCE)
                .amount(amount)
                .currency("USD")
                .description("Work Order #" + workOrder.getId())
                .relatedWorkOrder(workOrder)
                .isTaxDeductible(true)
                .build();
    }
}
