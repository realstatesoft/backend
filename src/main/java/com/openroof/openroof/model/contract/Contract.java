package com.openroof.openroof.model.contract;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
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
        @Index(name = "idx_contracts_type", columnList = "contract_type")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_agent_id")
    private AgentProfile listingAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_agent_id")
    private AgentProfile buyerAgent;

    @Column(name = "commission_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionPct = BigDecimal.ZERO;

    @Column(name = "listing_agent_commission_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal listingAgentCommissionPct = BigDecimal.ZERO;

    @Column(name = "buyer_agent_commission_pct", precision = 5, scale = 2)
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
