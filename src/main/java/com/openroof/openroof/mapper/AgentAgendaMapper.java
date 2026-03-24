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
        
        // visitId == null means "remove association"; only skip update if field was not included.
        // Since UpdateAgentAgendaRequest is a plain record (null = not sent OR explicit null),
        // we use the resolved `visit` parameter from the service which is:
        //   - a Visit object  → caller sent a valid visitId
        //   - null            → caller sent visitId: null  (explicit removal)
        //   The service always calls this method, so we must always apply the value.
        //   To distinguish "not included in request" we rely on the service only passing
        //   a non-null `visit` when visitId != null, and null otherwise — which is correct.
        //   We therefore always update the visit field to support explicit dissociation.
        entity.setVisit(visit);
    }
}
