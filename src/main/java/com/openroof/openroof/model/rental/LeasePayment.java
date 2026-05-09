package com.openroof.openroof.model.rental;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.PaymentMethod;
import com.openroof.openroof.model.enums.LeasePaymentStatus;
import com.openroof.openroof.model.enums.LeasePaymentType;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "lease_payments", indexes = {
        @Index(name = "idx_lease_payments_lease", columnList = "lease_id"),
        @Index(name = "idx_lease_payments_payer", columnList = "payer_id"),
        @Index(name = "idx_lease_payments_status", columnList = "status")
})
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE lease_payments SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeasePayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_id")
    private RentalInstallment installment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(length = 50)
    private String gateway;

    @Column(name = "gateway_transaction_id", length = 255)
    private String gatewayTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeasePaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeasePaymentType type;

    @Column(name = "processing_fee", precision = 12, scale = 2)
    private BigDecimal processingFee;

    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "receipt_pdf_url", length = 500)
    private String receiptPdfUrl;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    public boolean isCompleted() {
        return LeasePaymentStatus.COMPLETED == status;
    }

    public boolean isRefundable() {
        return LeasePaymentStatus.COMPLETED == status && paidAt != null;
    }

    public void markAsCompleted() {
        this.status = LeasePaymentStatus.COMPLETED;
        if (this.paidAt == null) {
            this.paidAt = LocalDateTime.now();
        }
    }

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    protected void onPreSave() {
        if (LeasePaymentStatus.COMPLETED == status && paidAt == null) {
            this.paidAt = LocalDateTime.now();
        }
    }
}
