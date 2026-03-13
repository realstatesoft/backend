package com.openroof.openroof.mapper;

import com.openroof.openroof.common.embeddable.IntegerRange;
import com.openroof.openroof.common.embeddable.MoneyRange;
import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.user.User;
import org.springframework.stereotype.Component;

/**
 * Mapper manual para conversiones entre AgentClient (entidad) y DTOs.
 */
@Component
public class AgentClientMapper {

    // ─── Entity → Full Response ──────────────────────────────────

    public AgentClientResponse toResponse(AgentClient ac) {
        MoneyRange budget = ac.getBudgetRange();
        IntegerRange bedrooms = ac.getBedroomRange();
        IntegerRange bathrooms = ac.getBathroomRange();
        User user = ac.getUser();
        AgentProfile agent = ac.getAgent();

        return new AgentClientResponse(
                ac.getId(),
                // Agent
                agent != null ? agent.getId() : null,
                agent != null && agent.getUser() != null ? agent.getUser().getName() : null,
                // User
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                // Estado
                enumName(ac.getStatus()),
                enumName(ac.getPriority()),
                // Tags
                ac.getTags(),
                // Contadores
                ac.getVisitedPropertiesCount(),
                ac.getOffersCount(),
                // Rangos
                budget != null ? budget.getMin() : null,
                budget != null ? budget.getMax() : null,
                bedrooms != null ? bedrooms.getMin() : null,
                bedrooms != null ? bedrooms.getMax() : null,
                bathrooms != null ? bathrooms.getMin() : null,
                bathrooms != null ? bathrooms.getMax() : null,
                // Contacto
                enumName(ac.getPreferredContactMethod()),
                ac.getLastContactDate(),
                // Notas
                ac.getNotes(),
                // Audit
                ac.getCreatedAt(),
                ac.getUpdatedAt());
    }

    // ─── Entity → Summary Response ───────────────────────────────

    public AgentClientSummaryResponse toSummaryResponse(AgentClient ac) {
        User user = ac.getUser();

        return new AgentClientSummaryResponse(
                ac.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                enumName(ac.getStatus()),
                enumName(ac.getPriority()),
                ac.getLastContactDate(),
                ac.getCreatedAt());
    }

    // ─── Request → Entity ────────────────────────────────────────

    public AgentClient toEntity(CreateAgentClientRequest req, AgentProfile agent, User user) {
        AgentClient.AgentClientBuilder builder = AgentClient.builder()
                .agent(agent)
                .user(user);

        if (req.status() != null)
            builder.status(req.status());
        if (req.priority() != null)
            builder.priority(req.priority());
        if (req.tags() != null)
            builder.tags(req.tags());
        if (req.preferredContactMethod() != null)
            builder.preferredContactMethod(req.preferredContactMethod());
        if (req.notes() != null)
            builder.notes(req.notes());

        // Budget range
        if (req.minBudget() != null || req.maxBudget() != null) {
            builder.budgetRange(new MoneyRange(req.minBudget(), req.maxBudget()));
        }

        // Bedroom range
        if (req.minBedrooms() != null || req.maxBedrooms() != null) {
            builder.bedroomRange(new IntegerRange(req.minBedrooms(), req.maxBedrooms()));
        }

        // Bathroom range
        if (req.minBathrooms() != null || req.maxBathrooms() != null) {
            builder.bathroomRange(new IntegerRange(req.minBathrooms(), req.maxBathrooms()));
        }

        return builder.build();
    }

    // ─── Actualización parcial ────────────────────────────────────

    public void updateEntity(AgentClient ac, UpdateAgentClientRequest req) {
        if (req.status() != null)
            ac.setStatus(req.status());
        if (req.priority() != null)
            ac.setPriority(req.priority());
        if (req.tags() != null)
            ac.setTags(req.tags());
        if (req.preferredContactMethod() != null)
            ac.setPreferredContactMethod(req.preferredContactMethod());
        if (req.notes() != null)
            ac.setNotes(req.notes());

        // Budget range
        if (req.minBudget() != null || req.maxBudget() != null) {
            MoneyRange existing = ac.getBudgetRange();
            if (existing == null) {
                existing = new MoneyRange();
            }
            if (req.minBudget() != null)
                existing.setMin(req.minBudget());
            if (req.maxBudget() != null)
                existing.setMax(req.maxBudget());
            ac.setBudgetRange(existing);
        }

        // Bedroom range
        if (req.minBedrooms() != null || req.maxBedrooms() != null) {
            IntegerRange existing = ac.getBedroomRange();
            if (existing == null) {
                existing = new IntegerRange();
            }
            if (req.minBedrooms() != null)
                existing.setMin(req.minBedrooms());
            if (req.maxBedrooms() != null)
                existing.setMax(req.maxBedrooms());
            ac.setBedroomRange(existing);
        }

        // Bathroom range
        if (req.minBathrooms() != null || req.maxBathrooms() != null) {
            IntegerRange existing = ac.getBathroomRange();
            if (existing == null) {
                existing = new IntegerRange();
            }
            if (req.minBathrooms() != null)
                existing.setMin(req.minBathrooms());
            if (req.maxBathrooms() != null)
                existing.setMax(req.maxBathrooms());
            ac.setBathroomRange(existing);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }
}
