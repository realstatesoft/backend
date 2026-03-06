package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.AgentProfileMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentSpecialty;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.AgentSpecialtyRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AgentProfileService {

    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;
    private final AgentSpecialtyRepository agentSpecialtyRepository;
    private final AgentProfileMapper agentProfileMapper;

    // ─── CREATE ───────────────────────────────────────────────────

    public AgentProfileResponse create(CreateAgentProfileRequest request) {
        // Validar que el usuario existe
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con ID: " + request.userId()));

        // Validar que el usuario no tenga ya un perfil de agente
        if (agentProfileRepository.existsByUser_Id(request.userId())) {
            throw new BadRequestException(
                    "El usuario con ID " + request.userId() + " ya tiene un perfil de agente");
        }

        // Resolver especialidades (opcional)
        List<AgentSpecialty> specialties = resolveSpecialties(request.specialtyIds());

        // Crear entidad
        AgentProfile agent = agentProfileMapper.toEntity(request, user, specialties);

        // Asegurar que el rol del usuario sea AGENT
        if (user.getRole() != UserRole.AGENT) {
            user.setRole(UserRole.AGENT);
            userRepository.save(user);
        }

        agent = agentProfileRepository.save(agent);
        return agentProfileMapper.toResponse(agent);
    }

    // ─── READ ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AgentProfileResponse getById(Long id) {
        AgentProfile agent = findAgentOrThrow(id);
        return agentProfileMapper.toResponse(agent);
    }

    @Transactional(readOnly = true)
    public Page<AgentProfileSummaryResponse> getAll(Pageable pageable) {
        return agentProfileRepository.findAllWithUser(pageable)
                .map(agentProfileMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<AgentProfileSummaryResponse> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return agentProfileRepository.findAllWithUser(pageable)
                    .map(agentProfileMapper::toSummaryResponse);
        }
        return agentProfileRepository.searchByKeyword(keyword.trim(), pageable)
                .map(agentProfileMapper::toSummaryResponse);
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    public AgentProfileResponse update(Long id, UpdateAgentProfileRequest request) {
        AgentProfile agent = findAgentOrThrow(id);

        // Actualizar campos básicos
        agentProfileMapper.updateEntity(agent, request);

        // Reemplazar especialidades (si se envían)
        if (request.specialtyIds() != null) {
            List<AgentSpecialty> specialties = resolveSpecialties(request.specialtyIds());
            agent.setSpecialties(specialties);
        }

        // Reemplazar redes sociales (si se envían)
        if (request.socialMedia() != null) {
            agentProfileMapper.replaceSocialMedia(agent, request.socialMedia());
        }

        agent = agentProfileRepository.save(agent);
        return agentProfileMapper.toResponse(agent);
    }

    // ─── DELETE (Soft) ────────────────────────────────────────────

    public void delete(Long id) {
        AgentProfile agent = findAgentOrThrow(id);
        agent.softDelete();
        agentProfileRepository.save(agent);
    }

    // ─── Helpers privados ─────────────────────────────────────────

    private AgentProfile findAgentOrThrow(Long id) {
        return agentProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agente no encontrado con ID: " + id));
    }

    private List<AgentSpecialty> resolveSpecialties(List<Long> specialtyIds) {
        if (specialtyIds == null || specialtyIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(specialtyIds);
        List<AgentSpecialty> specialties = agentSpecialtyRepository.findAllById(uniqueIds);
        if (specialties.size() != uniqueIds.size()) {
            throw new BadRequestException("Algunas especialidades no fueron encontradas");
        }
        return specialties;
    }
}
