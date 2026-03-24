package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.agent.AgentAgendaResponse;
import com.openroof.openroof.dto.agent.CreateAgentAgendaRequest;
import com.openroof.openroof.dto.agent.UpdateAgentAgendaRequest;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.interaction.AgentAgenda;
import com.openroof.openroof.model.interaction.Visit;
import org.springframework.stereotype.Component;

@Component
public class AgentAgendaMapper {

    public AgentAgendaResponse toResponse(AgentAgenda entity) {
        if (entity == null) {
            return null;
        }

        return new AgentAgendaResponse(
                entity.getId(),
                entity.getAgent() != null ? entity.getAgent().getId() : null,
                entity.getVisit() != null ? entity.getVisit().getId() : null,
                entity.getEventType(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getLocation(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AgentAgenda toEntity(CreateAgentAgendaRequest req, AgentProfile agent, Visit visit) {
        if (req == null) {
            return null;
        }

        return AgentAgenda.builder()
                .agent(agent)
                .visit(visit)
                .eventType(req.eventType())
                .title(req.title())
                .description(req.description())
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .location(req.location())
                .notes(req.notes())
                .build();
    }

    public void updateEntity(AgentAgenda entity, UpdateAgentAgendaRequest req, Visit visit) {
        if (req == null || entity == null) {
            return;
        }

        if (req.eventType() != null) entity.setEventType(req.eventType());
        if (req.title() != null) entity.setTitle(req.title());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.startsAt() != null) entity.setStartsAt(req.startsAt());
        if (req.endsAt() != null) entity.setEndsAt(req.endsAt());
        if (req.location() != null) entity.setLocation(req.location());
        if (req.notes() != null) entity.setNotes(req.notes());
        
        // If the request explicitly comes with a visitId, update it (null is also valid to remove it)
        if (req.visitId() != null || visit != null) {
            entity.setVisit(visit);
        }
    }
}
