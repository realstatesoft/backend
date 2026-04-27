package com.openroof.openroof.service;

import com.openroof.openroof.dto.lead.CreateLeadFromWizardRequest;
import com.openroof.openroof.dto.lead.LeadResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.lead.Lead;
import com.openroof.openroof.model.lead.LeadStatus;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.LeadRepository;
import com.openroof.openroof.repository.LeadStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadStatusRepository leadStatusRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final com.openroof.openroof.repository.LeadInteractionRepository leadInteractionRepository;

    private static final String DEFAULT_STATUS = "Nuevo";
    private static final String WIZARD_SOURCE = "sell_wizard";

    /**
     * Crea un Lead desde el Sell Wizard.
     */
    @Transactional
    public LeadResponse createFromWizard(CreateLeadFromWizardRequest request) {
        // Buscar agente
        AgentProfile agent = agentProfileRepository.findById(request.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado con ID: " + request.agentId()));

        // Buscar o crear status por defecto (maneja concurrencia y soft-delete)
        LeadStatus status = getOrCreateDefaultStatus();

        // Construir metadata con toda la info del wizard
        Map<String, Object> metadata = buildMetadata(request);

        // Construir notas
        String notes = buildNotes(request);

        // Crear el Lead
        Lead lead = Lead.builder()
                .agent(agent)
                .status(status)
                .name(request.getFullName())
                .email(request.email())
                .phone(request.phone())
                .source(WIZARD_SOURCE)
                .notes(notes)
                .metadata(metadata)
                .build();

        Lead saved = leadRepository.save(lead);
        log.info("Lead creado desde wizard: id={}, agentId={}, name={}", saved.getId(), agent.getId(), saved.getName());

        return toResponse(saved, true);
    }

    /**
     * Obtiene los leads de un agente.
     */
    @Transactional(readOnly = true)
    public Page<LeadResponse> getLeadsByAgent(Long agentId, Pageable pageable) {
        return leadRepository.findByAgentId(agentId, pageable)
                .map(l -> toResponse(l, false));
    }

    /**
     * Obtiene un lead por ID.
     */
    @Transactional(readOnly = true)
    public LeadResponse getById(Long id) {
        Lead lead = leadRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado con ID: " + id));
        return toResponse(lead, true);
    }

    /**
     * Cuenta leads de un agente.
     */
    @Transactional(readOnly = true)
    public long countByAgent(Long agentId) {
        return leadRepository.countByAgentId(agentId);
    }

    // ─── HELPERS ──────────────────────────────────────────────────

    private LeadStatus getOrCreateDefaultStatus() {
        // 1. Try to find an active (non-deleted) status
        return leadStatusRepository.findByName(DEFAULT_STATUS)
                .orElseGet(() -> {
                    // 2. Check if a soft-deleted status exists (unique constraint still applies)
                    return leadStatusRepository.findByNameIncludingDeleted(DEFAULT_STATUS)
                            .map(existing -> {
                                existing.restore();
                                existing.setActive(true);
                                return leadStatusRepository.save(existing);
                            })
                            .orElseGet(this::tryCreateDefaultStatus);
                });
    }

    private LeadStatus tryCreateDefaultStatus() {
        try {
            return leadStatusRepository.saveAndFlush(LeadStatus.builder()
                    .name(DEFAULT_STATUS)
                    .color("#3b82f6")
                    .displayOrder(0)
                    .active(true)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Another thread already created it — re-fetch
            return leadStatusRepository.findByNameIncludingDeleted(DEFAULT_STATUS)
                    .orElseThrow(() -> new IllegalStateException(
                            "No se pudo obtener o crear el estado por defecto: " + DEFAULT_STATUS, e));
        }
    }

    private Map<String, Object> buildMetadata(CreateLeadFromWizardRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Ubicación
        metadata.put("address", request.address());
        metadata.put("latitude", request.latitude());
        metadata.put("longitude", request.longitude());
        
        // Tipo y categoría
        metadata.put("propertyType", request.propertyType() != null ? request.propertyType().name() : null);
        metadata.put("category", request.category() != null ? request.category().name() : null);
        
        // Detalles de la propiedad
        metadata.put("surfaceArea", request.surfaceArea());
        metadata.put("builtArea", request.builtArea());
        metadata.put("yearBuilt", request.yearBuilt());
        metadata.put("bedrooms", request.bedrooms());
        metadata.put("halfBath", request.halfBath());
        metadata.put("threeQuarterBath", request.threeQuarterBath());
        metadata.put("floors", request.floors());
        
        // Features
        metadata.put("hasPool", request.hasPool());
        metadata.put("parkingSpaces", request.parkingSpaces());
        metadata.put("hasSecureEntry", request.hasSecureEntry());
        metadata.put("hasBasement", request.hasBasement());
        metadata.put("basementArea", request.basementArea());
        
        // Condiciones
        metadata.put("exteriorCondition", request.exteriorCondition());
        metadata.put("livingRoomCondition", request.livingRoomCondition());
        metadata.put("bathroomCondition", request.bathroomCondition());
        metadata.put("kitchenCondition", request.kitchenCondition());
        metadata.put("countertopType", request.countertopType());
        
        // HOA & Special
        metadata.put("hasHOA", request.hasHOA());
        metadata.put("specialConditions", request.specialConditions());
        
        // Relación y timeline
        metadata.put("agentRelationship", request.agentRelationship());
        metadata.put("timeline", request.timeline());
        
        // Datos adicionales
        if (request.additionalData() != null) {
            metadata.putAll(request.additionalData());
        }
        
        return metadata;
    }

    private String buildNotes(CreateLeadFromWizardRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Solicitud desde Sell Wizard\n");
        sb.append("───────────────────────────\n");
        sb.append("Dirección: ").append(request.address()).append("\n");
        sb.append("Tipo: ").append(request.propertyType()).append("\n");
        sb.append("Operación: ").append(PropertyCategory.SALE.equals(request.category()) ? "Venta" : "Alquiler").append("\n");
        
        if (request.surfaceArea() != null && !request.surfaceArea().isEmpty()) {
            sb.append("Superficie: ").append(request.surfaceArea()).append(" m²\n");
        }
        if (request.bedrooms() != null && request.bedrooms() > 0) {
            sb.append("Habitaciones: ").append(request.bedrooms()).append("\n");
        }
        if (request.timeline() != null) {
            sb.append("Timeline: ").append(formatTimeline(request.timeline())).append("\n");
        }
        
        return sb.toString();
    }

    private String formatTimeline(String timeline) {
        return switch (timeline) {
            case "asap" -> "Lo antes posible";
            case "1_month" -> "En 1 mes";
            case "2_3_months" -> "En 2-3 meses";
            case "4_plus" -> "En 4+ meses";
            case "browsing" -> "Solo explorando";
            default -> timeline;
        };
    }

    private LeadResponse toResponse(Lead lead, boolean includeInteractions) {
        java.util.List<com.openroof.openroof.dto.lead.LeadInteractionResponse> interactionDtos = java.util.List.of();
        
        if (includeInteractions) {
            interactionDtos = leadInteractionRepository.findByLeadIdOrderByCreatedAtDesc(lead.getId()).stream()
                    .map(i -> new com.openroof.openroof.dto.lead.LeadInteractionResponse(
                            i.getId(),
                            i.getType().name(),
                            i.getSubject(),
                            i.getNote(),
                            i.getPerformedBy() != null ? i.getPerformedBy().getName() : null,
                            i.getOldStatus() != null ? i.getOldStatus().getName() : null,
                            i.getNewStatus() != null ? i.getNewStatus().getName() : null,
                            i.getCreatedAt()
                    ))
                    .toList();
        }

        return new LeadResponse(
                lead.getId(),
                lead.getAgent() != null ? lead.getAgent().getId() : null,
                lead.getAgent() != null && lead.getAgent().getUser() != null 
                        ? lead.getAgent().getUser().getName() : null,
                lead.getName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getSource(),
                lead.getStatus() != null ? lead.getStatus().getName() : null,
                lead.getStatus() != null ? lead.getStatus().getColor() : null,
                lead.getNotes(),
                lead.getMetadata(),
                interactionDtos,
                lead.getCreatedAt()
        );
    }
}
