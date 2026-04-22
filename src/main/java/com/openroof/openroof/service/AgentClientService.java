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

        if (request.userPhone() != null) {
            user.setPhone(request.userPhone());
            userRepository.save(user);
        }

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

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Email,Status,Priority,ClientType,CreatedAt\n");

        int page = 0;
        int size = 1000;
        Page<AgentClient> clientPage;

        do {
            clientPage = agentClientRepository.findAll(spec,
                    org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").ascending()));
            for (AgentClient ac : clientPage.getContent()) {
                User u = ac.getUser();
                csv.append(escapeCsv(String.valueOf(ac.getId()))).append(",")
                        .append(escapeCsv(u != null ? u.getName() : "")).append(",")
                        .append(escapeCsv(u != null ? u.getEmail() : "")).append(",")
                        .append(escapeCsv(ac.getStatus() != null ? ac.getStatus().name() : "")).append(",")
                        .append(escapeCsv(ac.getPriority() != null ? ac.getPriority().name() : "")).append(",")
                        .append(escapeCsv(ac.getClientType() != null ? ac.getClientType().name() : "")).append(",")
                        .append(escapeCsv(ac.getCreatedAt() != null ? ac.getCreatedAt().toString() : "")).append("\n");
            }
            page++;
        } while (clientPage.hasNext());

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    public AgentClientResponse update(Long id, UpdateAgentClientRequest request) {
        AgentClient agentClient = findOrThrow(id);
        
        // Update associated User profile fields if provided
        User user = agentClient.getUser();
        if (user != null) {
            boolean userModified = false;
            
            // Handle Name — preserve existing parts when only one component is provided
            if (request.firstName() != null || request.lastName() != null) {
                String currentName = user.getName() != null ? user.getName() : "";
                int lastSpace = currentName.lastIndexOf(' ');
                String existingFirst = lastSpace > 0 ? currentName.substring(0, lastSpace) : currentName;
                String existingLast  = lastSpace > 0 ? currentName.substring(lastSpace + 1) : "";

                String fName = request.firstName() != null ? request.firstName() : existingFirst;
                String lName = request.lastName()  != null ? request.lastName()  : existingLast;

                user.setName((fName + " " + lName).trim());
                userModified = true;
            }

            // Email — validate uniqueness before updating
            if (request.userEmail() != null && !request.userEmail().equals(user.getEmail())) {
                userRepository.findByEmail(request.userEmail()).ifPresent(existing -> {
                    if (!existing.getId().equals(user.getId())) {
                        throw new BadRequestException("El email '" + request.userEmail() + "' ya está en uso por otro usuario");
                    }
                });
                user.setEmail(request.userEmail());
                userModified = true;
            }
            
            if (request.userPhone() != null) {
                user.setPhone(request.userPhone());
                userModified = true;
            }
            
            if (userModified) {
                userRepository.save(user);
            }
        }

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
