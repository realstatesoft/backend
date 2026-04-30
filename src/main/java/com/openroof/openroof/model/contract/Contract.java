package com.openroof.openroof.model.contract;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "contracts", indexes = {
        @Index(name = "idx_contracts_property", columnList = "property_id"),
        @Index(name = "idx_contracts_buyer", columnList = "buyer_id"),
        @Index(name = "idx_contracts_seller", columnList = "seller_id"),
        @Index(name = "idx_contracts_status", columnList = "status"),
        @Index(name = "idx_contracts_type", columnList = "contract_type"),
        @Index(name = "idx_contracts_listing_agent", columnList = "listing_agent_id"),
        @Index(name = "idx_contracts_buyer_agent", columnList = "buyer_agent_id")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ContractTemplate template;

    /**
     * Agente del vendedor/propietario (listing agent).
     * Puede ser null si el propietario gestiona la venta sin agente intermediario.
     * Corresponde a property.agent al momento de cerrar el contrato.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_agent_id")
    private AgentProfile listingAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_agent_id")
    private AgentProfile buyerAgent;

    /**
     * Comision total de la operacion en porcentaje.
     * Ej: 6.00 = 6% del monto del contrato.
     * Para ventas, tipicamente 3%-6%. Para alquileres, ~8%-10% del contrato anual.
     */
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Column(name = "commission_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionPct = new BigDecimal("3.00");

    /**
     * Porcentaje de la comision que corresponde al agente listador
     * (el que representa al vendedor o propietario).
     */
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Column(name = "listing_agent_commission_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal listingAgentCommissionPct = new BigDecimal("3.00");

    /**
     * Porcentaje de la comision que corresponde al agente del comprador
     * o inquilino (si aplica). Puede ser 0 si no hay agente del otro lado.
     */
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Column(name = "buyer_agent_commission_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal buyerAgentCommissionPct = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 50)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "PYG";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContractSignature> signatures = new ArrayList<>();
}