package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.contract.ContractResponse;
import com.openroof.openroof.dto.contract.ContractSummaryResponse;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.user.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ContractMapper {

    // ─── Entity → ContractResponse ────────────────────────────────────────────

    public ContractResponse toResponse(Contract c) {
        BigDecimal amount = c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO;

        BigDecimal totalPct    = pctOrZero(c.getCommissionPct());
        BigDecimal listingPct  = pctOrZero(c.getListingAgentCommissionPct());
        BigDecimal buyerPct    = pctOrZero(c.getBuyerAgentCommissionPct());

        BigDecimal totalAmt   = calcAmount(amount, totalPct);
        BigDecimal listingAmt = calcAmount(amount, listingPct);
        BigDecimal buyerAmt   = calcAmount(amount, buyerPct);

        return new ContractResponse(
                c.getId(),

                c.getProperty() != null ? c.getProperty().getId() : null,
                c.getProperty() != null ? c.getProperty().getTitle() : null,

                userId(c.getBuyer()),
                userName(c.getBuyer()),
                userEmail(c.getBuyer()),

                userId(c.getSeller()),
                userName(c.getSeller()),
                userEmail(c.getSeller()),

                agentId(c.getListingAgent()),
                agentName(c.getListingAgent()),

                agentId(c.getBuyerAgent()),
                agentName(c.getBuyerAgent()),

                c.getContractType(),
                c.getStatus(),
                amount,

                c.getStartDate(),
                c.getEndDate(),
                c.getTerms(),
                c.getDocumentUrl(),

                c.getTemplate() != null ? c.getTemplate().getId() : null,
                c.getTemplate() != null ? c.getTemplate().getName() : null,

                totalPct,
                listingPct,
                buyerPct,
                totalAmt,
                listingAmt,
                buyerAmt,

                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    // ─── Entity → ContractSummaryResponse ────────────────────────────────────

    public ContractSummaryResponse toSummaryResponse(Contract c, boolean hasSigned) {
        BigDecimal amount  = c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO;
        BigDecimal totalPct = pctOrZero(c.getCommissionPct());
        BigDecimal totalAmt = calcAmount(amount, totalPct);

        return new ContractSummaryResponse(
                c.getId(),

                c.getProperty() != null ? c.getProperty().getId() : null,
                c.getProperty() != null ? c.getProperty().getTitle() : null,

                userName(c.getBuyer()),
                userName(c.getSeller()),

                agentName(c.getListingAgent()),
                agentName(c.getBuyerAgent()),

                c.getContractType(),
                c.getStatus(),
                amount,
                totalAmt,
                c.getStartDate(),
                c.getCreatedAt(),
                hasSigned
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private BigDecimal pctOrZero(BigDecimal pct) {
        return pct != null ? pct : BigDecimal.ZERO;
    }

    private BigDecimal calcAmount(BigDecimal amount, BigDecimal pct) {
        return amount.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private Long userId(User u) {
        return u != null ? u.getId() : null;
    }

    private String userName(User u) {
        return u != null ? u.getName() : null;
    }

    private String userEmail(User u) {
        return u != null ? u.getEmail() : null;
    }

    private Long agentId(AgentProfile a) {
        return a != null ? a.getId() : null;
    }

    private String agentName(AgentProfile a) {
        return (a != null && a.getUser() != null) ? a.getUser().getName() : null;
    }
}
