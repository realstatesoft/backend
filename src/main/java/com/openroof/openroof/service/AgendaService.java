package com.openroof.openroof.service;

import com.openroof.openroof.dto.agenda.AgendaEventResponse;
import com.openroof.openroof.dto.agenda.CreateAgendaEventRequest;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.EventType;
import com.openroof.openroof.model.interaction.AgentAgenda;
import com.openroof.openroof.model.interaction.Visit;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentAgendaRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgendaService {

    private final AgentAgendaRepository agendaRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;

    public List<AgendaEventResponse> getAll(String email) {
        AgentProfile agent = findAgentByEmail(email);
        return agendaRepository.findByAgent_Id(agent.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AgendaEventResponse> getByDateRange(String email, LocalDateTime from, LocalDateTime to) {
        AgentProfile agent = findAgentByEmail(email);
        return agendaRepository.findByAgentIdAndDateRange(agent.getId(), from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AgendaEventResponse> getUpcoming(String email, int limit) {
        AgentProfile agent = findAgentByEmail(email);
        return agendaRepository.findUpcomingByAgentId(agent.getId(), LocalDateTime.now(), PageRequest.of(0, limit))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AgendaEventResponse create(String email, CreateAgendaEventRequest request) {
        AgentProfile agent = findAgentByEmail(email);

        AgentAgenda event = AgentAgenda.builder()
                .agent(agent)
                .title(request.title())
                .description(request.description())
                .eventType(EventType.valueOf(request.eventType()))
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .location(request.location())
                .notes(request.notes())
                .build();

        if (request.visitId() != null) {
            Visit visit = visitRepository.findById(request.visitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visita no encontrada con ID: " + request.visitId()));
            event.setVisit(visit);
        }

        return toResponse(agendaRepository.save(event));
    }

    @Transactional
    public void delete(String email, Long eventId) {
        AgentProfile agent = findAgentByEmail(email);
        AgentAgenda event = agendaRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + eventId));
        if (!event.getAgent().getId().equals(agent.getId())) {
            throw new ResourceNotFoundException("Evento no encontrado con ID: " + eventId);
        }
        event.softDelete();
        agendaRepository.save(event);
    }

    private AgendaEventResponse toResponse(AgentAgenda e) {
        String clientName = null;
        if (e.getVisit() != null && e.getVisit().getBuyer() != null) {
            clientName = e.getVisit().getBuyer().getName();
        }
        return new AgendaEventResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getEventType() != null ? e.getEventType().name() : null,
                e.getStartsAt(),
                e.getEndsAt(),
                e.getLocation(),
                e.getNotes(),
                clientName,
                e.getCreatedAt()
        );
    }

    private AgentProfile findAgentByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return agentProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de agente no encontrado"));
    }
}
