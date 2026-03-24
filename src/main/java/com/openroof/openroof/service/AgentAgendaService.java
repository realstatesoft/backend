package com.openroof.openroof.service;

import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.dto.agent.AgentAgendaResponse;
import com.openroof.openroof.dto.agent.CreateAgentAgendaRequest;
import com.openroof.openroof.dto.agent.UpdateAgentAgendaRequest;
import com.openroof.openroof.mapper.AgentAgendaMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.interaction.AgentAgenda;
import com.openroof.openroof.model.interaction.Visit;
import com.openroof.openroof.repository.AgentAgendaRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentAgendaService {

    private final AgentAgendaRepository agentAgendaRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final VisitRepository visitRepository;
    private final AgentAgendaMapper agentAgendaMapper;

    @Transactional(readOnly = true)
    public List<AgentAgendaResponse> getAgendaForMonth(String username, LocalDateTime startOfMonth, LocalDateTime endOfMonth) {
        AgentProfile agent = agentProfileRepository.findByUser_Email(username)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "email", username));

        return agentAgendaRepository.findByAgentIdAndStartsAtBetweenOrderByStartsAtAsc(agent.getId(), startOfMonth, endOfMonth)
                .stream()
                .map(agentAgendaMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AgentAgendaResponse getById(Long id, String username) {
        AgentAgenda event = getEventValidator(id, username);
        return agentAgendaMapper.toResponse(event);
    }

    @Transactional
    public AgentAgendaResponse create(CreateAgentAgendaRequest request, String username) {
        AgentProfile agent = agentProfileRepository.findByUser_Email(username)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "email", username));

        Visit visit = null;
        if (request.visitId() != null) {
            visit = visitRepository.findById(request.visitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visit", "id", request.visitId()));
        }

        AgentAgenda newEvent = agentAgendaMapper.toEntity(request, agent, visit);
        newEvent = agentAgendaRepository.save(newEvent);

        return agentAgendaMapper.toResponse(newEvent);
    }

    @Transactional
    public AgentAgendaResponse update(Long id, UpdateAgentAgendaRequest request, String username) {
        AgentAgenda event = getEventValidator(id, username);

        Visit visit = null;
        if (request.visitId() != null) {
            visit = visitRepository.findById(request.visitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visit", "id", request.visitId()));
        }

        agentAgendaMapper.updateEntity(event, request, visit);
        event = agentAgendaRepository.save(event);

        return agentAgendaMapper.toResponse(event);
    }

    @Transactional
    public void delete(Long id, String username) {
        AgentAgenda event = getEventValidator(id, username);
        agentAgendaRepository.delete(event); // Or implement soft delete if used in other places. Using JPA delete.
    }
    
    private AgentAgenda getEventValidator(Long id, String username) {
        AgentProfile agent = agentProfileRepository.findByUser_Email(username)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "email", username));

        AgentAgenda event = agentAgendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AgentAgenda", "id", id));

        if (!event.getAgent().getId().equals(agent.getId())) {
            throw new IllegalArgumentException("No tienes permiso para modificar este evento.");
        }
        
        return event;
    }
}
