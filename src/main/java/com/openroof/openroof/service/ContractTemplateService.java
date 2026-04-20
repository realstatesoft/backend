package com.openroof.openroof.service;

import com.openroof.openroof.dto.contract.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.contract.ContractTemplate;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.repository.ContractTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContractTemplateService {

    private final ContractTemplateRepository contractTemplateRepository;

    @Transactional
    public ContractTemplateResponse createTemplate(ContractTemplateCreateRequest request) {
        String version = (request.templateVersion() == null || request.templateVersion().isBlank())
                ? "1.0"
                : request.templateVersion().trim();
        Boolean active = request.active() != null ? request.active() : Boolean.TRUE;

        ContractTemplate entity = ContractTemplate.builder()
                .name(request.name().trim())
                .contractType(request.contractType())
                .content(request.content())
                .templateVersion(version)
                .active(active)
                .build();

        ContractTemplate saved = contractTemplateRepository.save(entity);
        log.info("Plantilla de contrato creada id={} name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public ContractTemplateResponse updateTemplate(Long id, ContractTemplateUpdateRequest request) {
        ContractTemplate entity = contractTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));

        entity.setName(request.name().trim());
        entity.setContractType(request.contractType());
        entity.setContent(request.content());
        entity.setTemplateVersion(request.templateVersion().trim());
        entity.setActive(request.active());

        log.info("Plantilla de contrato actualizada id={}", id);
        return toResponse(contractTemplateRepository.save(entity));
    }

    @Transactional
    public ContractTemplateResponse activateTemplate(Long id) {
        return setActive(id, true);
    }

    @Transactional
    public ContractTemplateResponse deactivateTemplate(Long id) {
        return setActive(id, false);
    }

    private ContractTemplateResponse setActive(Long id, boolean active) {
        ContractTemplate entity = contractTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
        entity.setActive(active);
        return toResponse(contractTemplateRepository.save(entity));
    }

    public List<ContractTemplateSummaryResponse> getTemplates(ContractType contractType, Boolean active) {
        return contractTemplateRepository.findAdminFiltered(contractType, active).stream()
                .map(this::toSummary)
                .toList();
    }

    public List<ContractTemplateResponse> getActiveTemplates(ContractType contractType) {
        if (contractType == null) {
            throw new BadRequestException("El parámetro contractType es obligatorio");
        }
        return contractTemplateRepository.findByContractTypeAndActiveTrueOrderByNameAsc(contractType).stream()
                .map(this::toResponse)
                .toList();
    }

    public ContractTemplateResponse getById(Long id) {
        ContractTemplate entity = contractTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
        return toResponse(entity);
    }

    @Transactional
    public void softDeleteTemplate(Long id) {
        ContractTemplate entity = contractTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
        entity.softDelete();
        contractTemplateRepository.save(entity);
        log.info("Plantilla de contrato eliminada (soft) id={}", id);
    }

    private ContractTemplateSummaryResponse toSummary(ContractTemplate t) {
        return new ContractTemplateSummaryResponse(
                t.getId(),
                t.getName(),
                t.getContractType(),
                t.getTemplateVersion(),
                t.getActive(),
                t.getUpdatedAt()
        );
    }

    private ContractTemplateResponse toResponse(ContractTemplate t) {
        return new ContractTemplateResponse(
                t.getId(),
                t.getName(),
                t.getContractType(),
                t.getContent(),
                t.getTemplateVersion(),
                t.getActive(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
