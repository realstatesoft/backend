package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.contract.ContractResponse;
import com.openroof.openroof.dto.contract.ContractSummaryResponse;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponse toResponse(Contract c) {
        AgentProfile la = c.getListingAgent();
        AgentProfile ba = c.getBuyerAgent();
        Property prop   = c.getProperty();
        User buyer      = c.getBuyer();
        User seller     = c.getSeller();

        return new ContractResponse(
                c.getId(),
                prop != null ? prop.getId() : null,
                prop != null ? prop.getTitle() : null,
                buyer != null ? buyer.getId() : null,
                buyer != null ? buyer.getName() : null,
                buyer != null ? buyer.getEmail() : null,
                seller != null ? seller.getId() : null,
                seller != null ? seller.getName() : null,
                seller != null ? seller.getEmail() : null,
                la != null ? la.getId() : null,
                la != null && la.getUser() != null ? la.getUser().getName() : null,
                ba != null ? ba.getId() : null,
                ba != null && ba.getUser() != null ? ba.getUser().getName() : null,
                c.getTemplate() != null ? c.getTemplate().getId() : null,
                c.getContractType(),
                c.getStatus(),
                c.getAmount(),
                c.getCommissionPct(),
                c.getListingAgentCommissionPct(),
                c.getBuyerAgentCommissionPct(),
                c.getStartDate(),
                c.getEndDate(),
                c.getTerms(),
                c.getDocumentUrl(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    public ContractSummaryResponse toSummaryResponse(Contract c) {
        Property prop = c.getProperty();
        User buyer    = c.getBuyer();
        User seller   = c.getSeller();

        return new ContractSummaryResponse(
                c.getId(),
                prop != null ? prop.getId() : null,
                prop != null ? prop.getTitle() : null,
                buyer != null ? buyer.getName() : null,
                seller != null ? seller.getName() : null,
                c.getContractType(),
                c.getStatus(),
                c.getAmount(),
                c.getStartDate(),
                c.getEndDate(),
                c.getCreatedAt()
        );
    }
}
