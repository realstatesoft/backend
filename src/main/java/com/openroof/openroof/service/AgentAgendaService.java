package com.openroof.openroof.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openroof.openroof.dto.agent.AgentAgendaResponse;
import com.openroof.openroof.dto.agent.CreateAgentAgendaRequest;
import com.openroof.openroof.dto.agent.UpdateAgentAgendaRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.AgentAgendaMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.interaction.AgentAgenda;
import com.openroof.openroof.model.interaction.Visit;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentAgendaRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.VisitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentAgendaService {

    private final AgentAgendaRepository agentAgendaRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;
    private final AgentAgendaMapper agentAgendaMapper;

    // ─── READ ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AgentAgendaResponse> getAgendaForMonth(String username, LocalDateTime startOfMonth, LocalDateTime endOfMonth) {
        User user = findUserByEmail(username);
        return agentAgendaRepository.findByUserAndMonthOverlap(user.getId(), startOfMonth, endOfMonth)
                .stream()
                .map(agentAgendaMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgentAgendaResponse getById(Long id, String username) {
        User user = findUserByEmail(username);
        AgentAgenda event = getEventForUser(id, user);
        return agentAgendaMapper.toResponse(event);
    }

    // ─── CREATE ───────────────────────────────────────────────────

    @Transactional
    public AgentAgendaResponse create(CreateAgentAgendaRequest request, String username) {
        User user = findUserByEmail(username);

        // Populate agent profile only if the user is an AGENT
        AgentProfile agent = null;
        if (UserRole.AGENT.equals(user.getRole())) {
            agent = agentProfileRepository.findByUser_Email(username).orElse(null);
        }

        Visit visit = null;
        if (request.visitId() != null) {
            visit = visitRepository.findByIdAndUserAccess(request.visitId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visit", "id", request.visitId()));
        }

        if (request.endsAt() != null && request.startsAt() != null
                && !request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        AgentAgenda newEvent = agentAgendaMapper.toEntity(request, user, agent, visit);
        newEvent = agentAgendaRepository.save(newEvent);
        return agentAgendaMapper.toResponse(newEvent);
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    @Transactional
    public AgentAgendaResponse update(Long id, UpdateAgentAgendaRequest request, String username) {
        User user = findUserByEmail(username);
        AgentAgenda event = getEventForUser(id, user);

        // Preserve existing visit unless a new visitId is explicitly provided.
        // (null visitId = "don't touch the association", not "remove it")
        Visit visit = event.getVisit();
        if (request.visitId() != null) {
            visit = visitRepository.findByIdAndUserAccess(request.visitId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visit", "id", request.visitId()));
        }

        LocalDateTime effectiveStart = request.startsAt() != null ? request.startsAt() : event.getStartsAt();
        LocalDateTime effectiveEnd   = request.endsAt()   != null ? request.endsAt()   : event.getEndsAt();
        if (effectiveEnd != null && effectiveStart != null && !effectiveEnd.isAfter(effectiveStart)) {
            throw new BadRequestException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        agentAgendaMapper.updateEntity(event, request, visit);
        event = agentAgendaRepository.save(event);
        return agentAgendaMapper.toResponse(event);
    }

    // ─── DELETE ───────────────────────────────────────────────────

    @Transactional
    public void delete(Long id, String username) {
        User user = findUserByEmail(username);
        AgentAgenda event = getEventForUser(id, user);
        agentAgendaRepository.delete(event);
    }

    // ─── HELPERS ──────────────────────────────────────────────────

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Loads the event filtering by both id and owner in a single query.
     * Always throws 404 — whether the event doesn't exist OR belongs to
     * another user — to prevent ID enumeration attacks.
     */
    private AgentAgenda getEventForUser(Long id, User user) {
        return agentAgendaRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AgentAgenda", "id", id));
    }
}
