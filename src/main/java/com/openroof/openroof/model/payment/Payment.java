package com.openroof.openroof.model.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.PaymentGatewayProvider;
import com.openroof.openroof.model.enums.PaymentType;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE payments SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "transaction_code", unique = true, nullable = false, length = 255)
    private String transactionCode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String concept;

    @Embedded
    private PaymentMetadata metadata;

    // ─── Idempotencia (clave provista por el cliente vía header Idempotency-Key) ───
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    // ─── Integración con pasarela (null = flujo manual admin) ───
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", length = 20)
    private PaymentGatewayProvider gateway;

    @Column(name = "gateway_process_id", length = 50)
    private String gatewayProcessId;

    @Column(name = "gateway_authorization_number", length = 10)
    private String gatewayAuthorizationNumber;

    @Column(name = "gateway_ticket_number", length = 20)
    private String gatewayTicketNumber;

    @Column(name = "gateway_response_code", length = 5)
    private String gatewayResponseCode;

    @Column(name = "checkout_started_at")
    private LocalDateTime checkoutStartedAt;
}
