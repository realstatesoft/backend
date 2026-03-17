package com.openroof.openroof.mapper;

import com.openroof.openroof.common.embeddable.IntegerRange;
import com.openroof.openroof.common.embeddable.MoneyRange;
import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.MaritalStatus;
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
                user != null ? user.getPhone() : null,
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
                // Perfil detallado
                ac.getBirthDate(),
                ac.getMaritalStatus(),
                ac.getOccupation(),
                ac.getAnnualIncome(),
                ac.getAddress(),
                ac.getSourceChannel(),
                ac.getInteractionsCount(),
                ac.getPreferredPropertyTypes(),
                ac.getPreferredAreas(),
                ac.getDesiredFeatures(),
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

        // New fields
        if (req.birthDate() != null) builder.birthDate(req.birthDate());
        if (req.maritalStatus() != null) builder.maritalStatus(req.maritalStatus());
        if (req.occupation() != null) builder.occupation(req.occupation());
        if (req.annualIncome() != null) builder.annualIncome(req.annualIncome());
        if (req.address() != null) builder.address(req.address());
        if (req.sourceChannel() != null) builder.sourceChannel(req.sourceChannel());
        if (req.preferredPropertyTypes() != null) builder.preferredPropertyTypes(req.preferredPropertyTypes());
        if (req.preferredAreas() != null) builder.preferredAreas(req.preferredAreas());
        if (req.desiredFeatures() != null) builder.desiredFeatures(req.desiredFeatures());

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

        if (req.birthDate() != null) ac.setBirthDate(req.birthDate());
        if (req.maritalStatus() != null) ac.setMaritalStatus(req.maritalStatus());
        if (req.occupation() != null) ac.setOccupation(req.occupation());
        if (req.annualIncome() != null) ac.setAnnualIncome(req.annualIncome());
        if (req.address() != null) ac.setAddress(req.address());
        if (req.sourceChannel() != null) ac.setSourceChannel(req.sourceChannel());
        if (req.preferredPropertyTypes() != null) ac.setPreferredPropertyTypes(req.preferredPropertyTypes());
        if (req.preferredAreas() != null) ac.setPreferredAreas(req.preferredAreas());
        if (req.desiredFeatures() != null) ac.setDesiredFeatures(req.desiredFeatures());

        // Budget range
        ac.setBudgetRange(mergeMoneyRange(ac.getBudgetRange(), req.minBudget(), req.maxBudget()));

        // Bedroom range
        ac.setBedroomRange(mergeIntegerRange(ac.getBedroomRange(), req.minBedrooms(), req.maxBedrooms()));

        // Bathroom range
        ac.setBathroomRange(mergeIntegerRange(ac.getBathroomRange(), req.minBathrooms(), req.maxBathrooms()));
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }

    private MoneyRange mergeMoneyRange(MoneyRange current, java.math.BigDecimal min, java.math.BigDecimal max) {
        if (min == null && max == null) {
            return current;
        }
        MoneyRange target = (current != null) ? current : new MoneyRange();
        if (min != null) target.setMin(min);
        if (max != null) target.setMax(max);
        return target;
    }

    private IntegerRange mergeIntegerRange(IntegerRange current, Integer min, Integer max) {
        if (min == null && max == null) {
            return current;
        }
        IntegerRange target = (current != null) ? current : new IntegerRange();
        if (min != null) target.setMin(min);
        if (max != null) target.setMax(max);
        return target;
    }
}
