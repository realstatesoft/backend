package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.AgentClientMapper;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentClientRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.specification.AgentClientSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AgentClientService {

    private final AgentClientRepository agentClientRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;
    private final AgentClientMapper agentClientMapper;

    // ─── CREATE ───────────────────────────────────────────────────

    public AgentClientResponse create(CreateAgentClientRequest request) {
        // Resolver agente
        AgentProfile agent = agentProfileRepository.findById(request.agentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agente no encontrado con ID: " + request.agentId()));

        // Resolver usuario/cliente
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con ID: " + request.userId()));

        AgentClient agentClient = agentClientMapper.toEntity(request, agent, user);
        
        try {
            agentClient = agentClientRepository.save(agentClient);
            // Si hay flush inmediato o al terminar la tx se lanzará la excepción si el (agent, user) ya existe
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BadRequestException("Ya existe un registro de cliente para este agente y usuario");
        }

        return agentClientMapper.toResponse(agentClient);
    }

    // ─── READ ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AgentClientResponse getById(Long id) {
        AgentClient agentClient = findOrThrow(id);
        return agentClientMapper.toResponse(agentClient);
    }

    @Transactional(readOnly = true)
    public Page<AgentClientSummaryResponse> getByAgent(Long agentId, Pageable pageable) {
        return agentClientRepository.findByAgent_Id(agentId, pageable)
                .map(agentClientMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<AgentClientSummaryResponse> searchClients(Long agentId, AgentClientSearchRequest criteria, Pageable pageable) {
        Specification<AgentClient> spec = AgentClientSpecification.filterBy(agentId, criteria);
        return agentClientRepository.findAll(spec, pageable)
                .map(agentClientMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public String exportClientsToCsv(Long agentId, AgentClientSearchRequest criteria) {
        Specification<AgentClient> spec = AgentClientSpecification.filterBy(agentId, criteria);
        List<AgentClient> clients = agentClientRepository.findAll(spec);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Email,Status,Priority,ClientType,CreatedAt\n");

        for (AgentClient ac : clients) {
            User u = ac.getUser();
            csv.append(ac.getId()).append(",")
                    .append(u != null ? u.getName() : "").append(",")
                    .append(u != null ? u.getEmail() : "").append(",")
                    .append(ac.getStatus()).append(",")
                    .append(ac.getPriority()).append(",")
                    .append(ac.getClientType() != null ? ac.getClientType() : "").append(",")
                    .append(ac.getCreatedAt()).append("\n");
        }

        return csv.toString();
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    public AgentClientResponse update(Long id, UpdateAgentClientRequest request) {
        AgentClient agentClient = findOrThrow(id);
        agentClientMapper.updateEntity(agentClient, request);
        agentClient = agentClientRepository.save(agentClient);
        return agentClientMapper.toResponse(agentClient);
    }

    // ─── DELETE ───────────────────────────────────────────────────

    public void delete(Long id) {
        AgentClient agentClient = findOrThrow(id);
        agentClientRepository.delete(agentClient);
    }

    // ─── Helpers privados ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public Long getAgentIdByUser(Long userId) {
        return agentProfileRepository.findByUser_Id(userId)
                .map(AgentProfile::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de agente no encontrado para el usuario ID: " + userId));
    }

    private AgentClient findOrThrow(Long id) {
        return agentClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente de agente no encontrado con ID: " + id));
    }
}
