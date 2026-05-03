package com.openroof.openroof.service;

import com.openroof.openroof.dto.lead.CreateLeadFromWizardRequest;
import com.openroof.openroof.dto.lead.CreateLeadInteractionRequest;
import com.openroof.openroof.dto.lead.LeadInteractionResponse;
import com.openroof.openroof.dto.lead.LeadResponse;
import com.openroof.openroof.dto.lead.UpdateLeadStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrato del servicio de leads (contrato estable para MVC tests y mocks).
 */
public interface LeadServiceApi {

    LeadResponse createFromWizard(CreateLeadFromWizardRequest request);

    Page<LeadResponse> getLeadsByAgent(Long agentId, Pageable pageable);

    LeadResponse getById(Long id);

    long countByAgent(Long agentId);

    LeadResponse updateStatus(Long leadId, UpdateLeadStatusRequest request);

    LeadInteractionResponse addInteraction(Long leadId, CreateLeadInteractionRequest request);
}
